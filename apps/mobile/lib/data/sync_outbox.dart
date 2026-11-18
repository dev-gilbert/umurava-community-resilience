import 'dart:convert';

import 'package:uuid/uuid.dart';

import 'local_database.dart';

class SyncOutbox {
  SyncOutbox({LocalDatabase? database, Uuid? uuid})
      : _database = database ?? LocalDatabase.instance,
        _uuid = uuid ?? const Uuid();

  final LocalDatabase _database;
  final Uuid _uuid;

  Future<String> enqueue({
    required String communityId,
    required String deviceId,
    required String entityType,
    required String entityId,
    required String operation,
    required Map<String, Object?> payload,
    DateTime? createdAt,
  }) async {
    final id = _uuid.v4();
    final timestamp = (createdAt ?? DateTime.now()).toUtc();

    await _database.enqueueEvent(
      eventId: id,
      communityId: communityId,
      deviceId: deviceId,
      entityType: entityType,
      entityId: entityId,
      operation: operation,
      payloadJson: jsonEncode(payload),
      createdAt: timestamp,
    );
    return id;
  }


  Future<String> saveResourceChange({
    required Map<String, Object?> resource,
    required String communityId,
    required String deviceId,
    required String entityId,
    required String operation,
    required Map<String, Object?> payload,
    DateTime? createdAt,
  }) async {
    final id = _uuid.v4();
    final timestamp = (createdAt ?? DateTime.now()).toUtc();
    await _database.saveResourceAndEnqueue(
      resource: resource,
      eventId: id,
      communityId: communityId,
      deviceId: deviceId,
      entityType: 'RESOURCE',
      entityId: entityId,
      operation: operation,
      payloadJson: jsonEncode(payload),
      createdAt: timestamp,
    );
    return id;
  }

  Future<List<Map<String, Object?>>> pendingBatch({
    int limit = 100,
    DateTime? now,
  }) {
    return _database.pendingEvents(limit: limit, now: now);
  }

  Future<Map<String, Object?>> buildPushRequest({
    required String communityId,
    required String deviceId,
    int limit = 100,
    DateTime? now,
  }) async {
    final rows = await pendingBatch(limit: limit, now: now);
    final events = rows.map((row) {
      return <String, Object?>{
        'id': row['event_id'],
        'communityId': row['community_id'],
        'deviceId': row['device_id'],
        'entityType': row['entity_type'],
        'entityId': row['entity_id'],
        'operation': row['operation'],
        'payload': jsonDecode(row['payload'] as String),
        'eventVersion': 1,
        'eventCreatedAt': row['created_at'],
      };
    }).toList();

    return {
      'communityId': communityId,
      'deviceId': deviceId,
      'events': events,
    };
  }

  Future<void> markAccepted(String eventId, {int? serverSequence}) {
    return _database.markEventSynced(eventId, serverSequence: serverSequence);
  }

  Future<void> markFailed(
    Map<String, Object?> event, {
    required String error,
    DateTime? now,
  }) {
    return _database.scheduleRetry(
      event['event_id'] as String,
      error: error,
      currentAttempts: event['attempts'] as int? ?? 0,
      now: now,
    );
  }

  Future<int> cursorFor(String communityId) {
    return _database.readCursor(communityId);
  }

  Future<void> saveCursor(String communityId, int cursor) {
    return _database.saveCursor(communityId, cursor);
  }
}
