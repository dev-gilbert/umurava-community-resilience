#!/usr/bin/env bash
set -euo pipefail
: "${UMURAVA_API_URL:?Set UMURAVA_API_URL}"
: "${UMURAVA_E2E_EMAIL:?Set UMURAVA_E2E_EMAIL}"
: "${UMURAVA_E2E_PASSWORD:?Set UMURAVA_E2E_PASSWORD}"
: "${UMURAVA_E2E_COMMUNITY_ID:?Set UMURAVA_E2E_COMMUNITY_ID}"
BASE="${UMURAVA_API_URL%/}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
request() {
  local method="$1" path="$2" body="${3:-}" expected="${4:-200}"
  local args=(-sS -o "$TMP/body" -w '%{http_code}' -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')
  [[ -n "$body" ]] && args+=(-d "$body")
  local status
  status="$(curl "${args[@]}")"
  if [[ "$status" != "$expected" ]]; then
    echo "FAIL $method $path expected HTTP $expected got $status" >&2
    cat "$TMP/body" >&2
    exit 1
  fi
  cat "$TMP/body"
}
AUTH="$(curl -fsS "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' -d "$(jq -n --arg e "$UMURAVA_E2E_EMAIL" --arg p "$UMURAVA_E2E_PASSWORD" '{email:$e,password:$p}')")"
TOKEN="$(jq -r '.accessToken' <<<"$AUTH")"
[[ -n "$TOKEN" && "$TOKEN" != null ]] || { echo "Login did not return accessToken" >&2; exit 1; }
C="$UMURAVA_E2E_COMMUNITY_ID"
STAMP="$(date -u +%Y%m%d%H%M%S)"

# 1. Community and membership
request GET "/api/v1/me/communities" >/dev/null
request GET "/api/v1/communities/$C/memberships" >/dev/null

# 2. Shared resource registry
RESOURCE="$(request POST "/api/v1/communities/$C/resources" "$(jq -n --arg n "E2E resource $STAMP" '{name:$n,category:"GENERAL",description:"Automated acceptance test",quantity:10,minimumQuantity:1,unit:"units}')" 201)"
RID="$(jq -r '.id' <<<"$RESOURCE")"
[[ "$RID" != null && -n "$RID" ]]

# 3. Food
request POST "/api/v1/communities/$C/food-batches" "$(jq -n --arg n "E2E food $STAMP" '{name:$n,category:"GRAIN",quantity:20,unit:"kg",storageLocation:"E2E test"}')" 201 >/dev/null
request GET "/api/v1/communities/$C/food-batches" >/dev/null

# 4. Water source and quality test
WATER="$(request POST "/api/v1/communities/$C/water-sources" "$(jq -n --arg n "E2E water $STAMP" '{name:$n,sourceType:"BOREHOLE",estimatedLiters:500}')" 201)"
WID="$(jq -r '.id' <<<"$WATER")"
request POST "/api/v1/communities/$C/water-sources/$WID/quality-tests" '{"phValue":7.1,"turbidityNtu":1.2,"ecoliDetected":false,"notes":"Automated test only"}' 201 >/dev/null
request GET "/api/v1/communities/$C/water-sources" >/dev/null

# 5. Energy
request POST "/api/v1/communities/$C/energy-assets" "$(jq -n --arg n "E2E solar $STAMP" '{name:$n,assetType:"SOLAR",capacityKwh:12,currentOutputKw:3,operationalStatus:"OPERATIONAL",locationDescription:"E2E test"}')" 201 >/dev/null
request GET "/api/v1/communities/$C/energy-assets" >/dev/null

# 6. Health readiness (no patient information)
request POST "/api/v1/communities/$C/health-facilities" "$(jq -n --arg n "E2E clinic $STAMP" '{name:$n,facilityType:"CLINIC"}')" 201 >/dev/null
request GET "/api/v1/communities/$C/health-facilities" >/dev/null

# 7. Transport
request POST "/api/v1/communities/$C/transport-vehicles" "$(jq -n --arg n "E2E-$STAMP" '{vehicleCode:$n,vehicleType:"AMBULANCE",capacity:2,latitude:-1.95,longitude:30.06}')" 201 >/dev/null
request POST "/api/v1/communities/$C/transport-requests" '{"requestType":"SUPPLY","priority":"NORMAL","pickupLatitude":-1.95,"pickupLongitude":30.06,"destinationLatitude":-1.94,"destinationLongitude":30.07,"notes":"Automated test only; not a real dispatch"}' 201 >/dev/null
request GET "/api/v1/communities/$C/transport-requests" >/dev/null

# 8. Emergency and communications
request POST "/api/v1/communities/$C/incidents" "$(jq -n --arg n "E2E test incident $STAMP" '{incidentType:"TEST",description:$n,severity:"LOW",latitude:-1.95,longitude:30.06}')" 201 >/dev/null
request GET "/api/v1/communities/$C/incidents" >/dev/null
request POST "/api/v1/communities/$C/announcements" "$(jq -n --arg n "E2E test announcement $STAMP" '{title:$n,message:"Automated acceptance test; please ignore",priority:"LOW"}')" 201 >/dev/null
request GET "/api/v1/communities/$C/announcements" >/dev/null

echo "PASS: community, resource, food, water, energy, health, transport, incident and communications API workflows."
