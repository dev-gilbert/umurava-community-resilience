import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

class LocalDatabase {
  LocalDatabase._();

  static final instance = LocalDatabase._();
  static const _databaseName = 'umurava_local.db';
  static const _databaseVersion = 2;

  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;

    final path = p.join(await getDatabasesPath(), _databaseName);
    _db = await openDatabase(
      path,
      version: _databaseVersion,
      onCreate: (db, version) async {
        await _createLocalResources(db);
        await _createOutbox(db);
        await _createSyncState(db);
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        if (oldVersion < 2) {
          await db.execute("ALTER TABLE outbox ADD COLUMN community_id TEXT");
          await db.execute("ALTER TABLE outbox ADD COLUMN device_id TEXT");
          await db.execute("ALTER TABLE outbox ADD COLUMN next_retry_at TEXT");
          await db.execute("ALTER TABLE outbox ADD COLUMN last_error TEXT");
          await db.execute(
            "ALTER TABLE outbox ADD COLUMN server_sequence INTEGER",
          );
          await _createSyncState(db);
        }
      },
    );
    return _db!;
  }

  Future<void> _createLocalResources(DatabaseExecutor db) async {
    await db.execute('''
      CREATE TABLE local_resources(
        id TEXT PRIMARY KEY,
        community_id TEXT NOT NULL,
        name TEXT NOT NULL,
        category TEXT NOT NULL,
        quantity REAL NOT NULL DEFAULT 0,
        unit TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
      )
    ''');
  }

  Future<void> _createOutbox(DatabaseExecutor db) async {
    await db.execute('''
      CREATE TABLE outbox(
        event_id TEXT PRIMARY KEY,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        operation TEXT NOT NULL,
        payload TEXT NOT NULL,
        created_at TEXT NOT NULL,
        attempts INTEGER NOT NULL DEFAULT 0,
        status TEXT NOT NULL DEFAULT 'PENDING',
        community_id TEXT,
        device_id TEXT,
        next_retry_at TEXT,
        last_error TEXT,
        server_sequence INTEGER
      )
    ''');
    await db.execute(
      'CREATE INDEX idx_outbox_pending ON outbox(status, next_retry_at, created_at)',
    );
  }

  Future<void> _createSyncState(DatabaseExecutor db) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS sync_state(
        state_key TEXT PRIMARY KEY,
        state_value TEXT NOT NULL,
        updated_at TEXT NOT NULL
      )
    ''');
  }

  Future<List<Map<String, Object?>>> resources() async {
    return (await database).query(
      'local_resources',
      orderBy: 'name COLLATE NOCASE',
    );
  }

  Future<void> enqueueEvent({
    required String eventId,
    required String communityId,
    required String deviceId,
    required String entityType,
    required String entityId,
    required String operation,
    required String payloadJson,
    required DateTime createdAt,
  }) async {
    final db = await database;
    await db.insert(
      'outbox',
      {
        'event_id': eventId,
        'community_id': communityId,
        'device_id': deviceId,
        'entity_type': entityType,
        'entity_id': entityId,
        'operation': operation,
        'payload': payloadJson,
        'created_at': createdAt.toUtc().toIso8601String(),
        'attempts': 0,
        'status': 'PENDING',
      },
      conflictAlgorithm: ConflictAlgorithm.abort,
    );
  }


  Future<void> saveResourceAndEnqueue({
    required Map<String, Object?> resource,
    required String eventId,
    required String communityId,
    required String deviceId,
    required String entityType,
    required String entityId,
    required String operation,
    required String payloadJson,
    required DateTime createdAt,
  }) async {
    final db = await database;
    await db.transaction((transaction) async {
      await transaction.insert(
        'local_resources',
        resource,
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
      await transaction.insert(
        'outbox',
        {
          'event_id': eventId,
          'community_id': communityId,
          'device_id': deviceId,
          'entity_type': entityType,
          'entity_id': entityId,
          'operation': operation,
          'payload': payloadJson,
          'created_at': createdAt.toUtc().toIso8601String(),
          'attempts': 0,
          'status': 'PENDING',
        },
        conflictAlgorithm: ConflictAlgorithm.abort,
      );
    });
  }

  Future<List<Map<String, Object?>>> pendingEvents({
    int limit = 100,
    DateTime? now,
  }) async {
    final currentTime = (now ?? DateTime.now()).toUtc().toIso8601String();
    return (await database).query(
      'outbox',
      where: "status IN ('PENDING','RETRY') AND (next_retry_at IS NULL OR next_retry_at <= ?)",
      whereArgs: [currentTime],
      orderBy: 'created_at ASC',
      limit: limit.clamp(1, 100).toInt(),
    );
  }

  Future<void> markEventSynced(String eventId, {int? serverSequence}) async {
    await (await database).update(
      'outbox',
      {
        'status': 'SYNCED',
        'server_sequence': serverSequence,
        'last_error': null,
        'next_retry_at': null,
      },
      where: 'event_id = ?',
      whereArgs: [eventId],
    );
  }

  Future<void> scheduleRetry(
    String eventId, {
    required String error,
    required int currentAttempts,
    DateTime? now,
  }) async {
    final nextAttempt = currentAttempts + 1;
    final exponent = (nextAttempt - 1).clamp(0, 10).toInt();
    final delaySeconds = (5 * (1 << exponent)).clamp(5, 3600).toInt();
    final retryAt = (now ?? DateTime.now())
        .toUtc()
        .add(Duration(seconds: delaySeconds))
        .toIso8601String();

    await (await database).update(
      'outbox',
      {
        'attempts': nextAttempt,
        'status': 'RETRY',
        'next_retry_at': retryAt,
        'last_error': error.length > 500 ? error.substring(0, 500) : error,
      },
      where: 'event_id = ?',
      whereArgs: [eventId],
    );
  }

  Future<void> saveCursor(String communityId, int cursor) async {
    await (await database).insert(
      'sync_state',
      {
        'state_key': 'cursor:$communityId',
        'state_value': cursor.toString(),
        'updated_at': DateTime.now().toUtc().toIso8601String(),
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<int> readCursor(String communityId) async {
    final rows = await (await database).query(
      'sync_state',
      columns: ['state_value'],
      where: 'state_key = ?',
      whereArgs: ['cursor:$communityId'],
      limit: 1,
    );
    if (rows.isEmpty) return 0;
    return int.tryParse(rows.first['state_value'] as String) ?? 0;
  }
}
