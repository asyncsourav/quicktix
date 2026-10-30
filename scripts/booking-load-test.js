



// Install command -> "winget install GrafanaLabs.k6"

// Run command -> "& "C:\Program Files\k6\k6.exe" run scripts/booking-load-test.js"

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 50 },
    { duration: '10s', target: 100 },
    { duration: '30s', target: 100 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['p(95)<100'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'user@quicktix.com';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password123';

export function setup() {
  const loginUrl = `${BASE_URL}/api/auth/login`;
  const payload = JSON.stringify({
    email: USER_EMAIL,
    password: USER_PASSWORD,
  });

  const res = http.post(loginUrl, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const body = res.json();
  const token = body?.data?.accessToken || body?.accessToken;
  if (!token) {
    throw new Error(`Failed to obtain JWT token from ${loginUrl}`);
  }

  return { token };
}

export default function (data) {
  const targetUrl = `${BASE_URL}/api/bookings/me`;

  const res = http.get(targetUrl, {
    headers: {
      Authorization: `Bearer ${data.token}`,
      Accept: 'application/json',
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.05);
}
