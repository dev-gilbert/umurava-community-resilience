import json, logging, os, sqlite3, time
from pathlib import Path
from urllib.parse import urlencode
import requests

logging.basicConfig(level=os.getenv("LOG_LEVEL","INFO"), format="%(asctime)s %(levelname)s %(message)s")
log=logging.getLogger("umurava-edge-sync")
COMMUNITY=os.environ["SYNC_COMMUNITY_ID"]
DEVICE=os.environ["SYNC_DEVICE_ID"]
CLOUD=os.environ["CLOUD_API_URL"].rstrip("/")
EDGE=os.getenv("EDGE_API_URL","http://edge-api:8080").rstrip("/")
STATE=Path(os.getenv("SYNC_STATE_PATH","/state/edge-sync.sqlite3"))
POLL=max(2,int(os.getenv("SYNC_POLL_SECONDS","10")))
STATE.parent.mkdir(parents=True,exist_ok=True)
STATE.touch(mode=0o600,exist_ok=True)
os.chmod(STATE,0o600)

db=sqlite3.connect(STATE,check_same_thread=False)
db.execute("CREATE TABLE IF NOT EXISTS state (key TEXT PRIMARY KEY,value TEXT NOT NULL)")
db.commit()

def get_state(k, default=None):
    row=db.execute("SELECT value FROM state WHERE key=?",(k,)).fetchone()
    return row[0] if row else default

def put_state(k,v):
    db.execute("INSERT INTO state(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",(k,str(v)))
    db.commit()

class Api:
    def __init__(self,name,base):
        self.name,self.base=name,base
        self.access=get_state(name+"_access",os.getenv(name.upper()+"_ACCESS_TOKEN",""))
        self.refresh=get_state(name+"_refresh",os.getenv(name.upper()+"_REFRESH_TOKEN",""))
        if not self.access and not self.refresh:
            raise RuntimeError(name+" access or refresh token is required")
        self.http=requests.Session()

    def refresh_token(self):
        if not self.refresh: raise RuntimeError(self.name+" refresh token unavailable; provision a new session")
        r=self.http.post(self.base+"/api/v1/auth/refresh",json={"refreshToken":self.refresh},timeout=20)
        r.raise_for_status(); data=r.json()
        self.access=data["accessToken"]; self.refresh=data["refreshToken"]
        put_state(self.name+"_access",self.access); put_state(self.name+"_refresh",self.refresh)

    def request(self,method,path,**kwargs):
        for attempt in range(2):
            r=self.http.request(method,self.base+path,headers={"Authorization":"Bearer "+self.access},timeout=30,**kwargs)
            if r.status_code==401 and attempt==0:
                self.refresh_token(); continue
            r.raise_for_status()
            return r.json() if r.content else None
        raise RuntimeError("Authentication failed for "+self.name)

cloud=Api("cloud",CLOUD)
edge=Api("edge",EDGE)

def relay(source,target,direction):
    cursor=int(get_state(direction+"_cursor","0"))
    total=0
    while True:
        query=urlencode({"communityId":COMMUNITY,"deviceId":DEVICE,"cursor":cursor,"limit":100})
        page=source.request("GET","/api/v1/sync/pull?"+query)
        events=page.get("events",[])
        if events:
            payload={"communityId":COMMUNITY,"deviceId":DEVICE,"events":events}
            results=target.request("POST","/api/v1/sync/push",json=payload)
            if len(results)!=len(events) or any(x.get("result") not in ("ACCEPTED","DUPLICATE") for x in results):
                raise RuntimeError(direction+" destination did not accept every event")
            source.request("POST","/api/v1/sync/ack",json={"communityId":COMMUNITY,"deviceId":DEVICE,"eventIds":[e["id"] for e in events]})
            cursor=int(page.get("nextCursor",cursor)); put_state(direction+"_cursor",cursor)
            total+=len(events)
        if not page.get("hasMore",False): break
    if total: log.info("%s relayed %d events; cursor=%d",direction,total,cursor)

log.info("Starting bidirectional UMURAVA edge relay for community %s",COMMUNITY)
while True:
    try:
        relay(cloud,edge,"cloud_to_edge")
        relay(edge,cloud,"edge_to_cloud")
        time.sleep(POLL)
    except KeyboardInterrupt:
        break
    except Exception as exc:
        log.error("Relay cycle failed (%s); retaining cursors and retrying",type(exc).__name__)
        time.sleep(min(60,POLL*3))
