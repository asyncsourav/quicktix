const BASE = "http://localhost:8080";
const EMAIL = "ratelimit@quicktix.com";   // dedicated test user, NOT the race-test user
const PASSWORD = "password123";
const EVENT_ID = 1;
const SEAT_ID = 1;        // Seat 1 is ALREADY booked, so the data never changes
const TOTAL = 1000;       // phase 1: total rapid-fire requests
const CONCURRENCY = 100;  // how many are in flight at the same time

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const JSON_HEADERS = { "Content-Type": "application/json" };

async function getToken() {
  // Create the test user (ignored if it already exists), then log in
  await fetch(`${BASE}/api/auth/register`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({ name: "Rate Limit Tester", email: EMAIL, password: PASSWORD, role: "USER" }),
  }).catch(() => {});
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(`Login failed: ${res.status} ${JSON.stringify(body)}`);
  return body.data.accessToken;
}

async function book(token) {
  const res = await fetch(`${BASE}/api/bookings`, {
    method: "POST",
    headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` },
    body: JSON.stringify({ eventId: EVENT_ID, seatIds: [SEAT_ID] }),
  });
  const text = await res.text();
  return { status: res.status, retryAfter: res.headers.get("retry-after"), body: text.slice(0, 160) };
}

const token = await getToken();

// ---------- Phase 1: rapid-fire burst ----------
console.log(`Phase 1: ${TOTAL} requests, ${CONCURRENCY} in flight at a time...`);
const counts = {};
let sample429 = null;
let next = 0;

async function worker() {
  while (next < TOTAL) {
    next++;
    try {
      const r = await book(token);
      counts[r.status] = (counts[r.status] || 0) + 1;
      if (r.status === 429 && !sample429) sample429 = r;
    } catch {
      counts.network_error = (counts.network_error || 0) + 1;
    }
  }
}

const t0 = performance.now();
await Promise.all(Array.from({ length: CONCURRENCY }, worker));
const seconds = (performance.now() - t0) / 1000;

const blocked = counts[429] || 0;
const errors = counts.network_error || 0;
const allowed = TOTAL - blocked - errors;

console.table(counts);
console.log(`Finished in ${seconds.toFixed(2)} s`);
console.log(`Allowed through the limiter: ${allowed}`);
console.log(`Blocked with 429:            ${blocked} of ${TOTAL} (${((blocked / TOTAL) * 100).toFixed(1)}%)`);
if (errors) console.log(`Network errors (not counted): ${errors}. Lower CONCURRENCY if this is high.`);
if (sample429) {
  console.log(`Sample 429 -> Retry-After: ${sample429.retryAfter} | body: ${sample429.body}`);
}

// ---------- Phase 2: sustained rate ----------
const SECONDS = 10;
const PER_SECOND = 4;
console.log(`\nPhase 2: ${PER_SECOND} requests/second for ${SECONDS} s (limit should hold at the refill rate)...`);
let ok = 0, limited = 0;
for (let i = 0; i < SECONDS * PER_SECOND; i++) {
  const r = await book(token);
  if (r.status === 429) limited++; else ok++;
  await sleep(1000 / PER_SECOND);
}
console.log(`Allowed: ${ok} | Blocked: ${limited} | about ${(ok / SECONDS).toFixed(1)} allowed per second`);
