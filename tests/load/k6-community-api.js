import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://127.0.0.1:8081').replace(/\/$/, '');
const TOKEN = __ENV.ACCESS_TOKEN || '';
const COMMUNITY_ID = __ENV.COMMUNITY_ID || '';

export const options = {
  scenarios: { community_read: { executor: 'constant-vus', vus: Number(__ENV.VUS || 10), duration: __ENV.DURATION || '30s' } },
  thresholds: { http_req_duration: ['p(95)<750'], http_req_failed: ['rate<0.01'] },
};

export default function () {
  const headers = TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {};
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, { 'health endpoint returns 200': (r) => r.status === 200 });
  if (TOKEN && COMMUNITY_ID) {
    const resources = http.get(`${BASE_URL}/api/v1/communities/${COMMUNITY_ID}/resources`, { headers });
    check(resources, { 'authorized community resource read returns 200': (r) => r.status === 200 });
  }
  sleep(1);
}
