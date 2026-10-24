const BASE = "http://localhost:8080";

// Fetch fresh JWT dynamically or use environment override
let TOKEN = process.env.JWT_TOKEN || "";
if (!TOKEN) {
  try {
    const loginRes = await fetch(`${BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "user@quicktix.com", password: "password123" }),
    });
    const loginData = await loginRes.json();
    TOKEN = loginData?.data?.accessToken || "";
  } catch (err) {
    console.warn("Could not auto-login, proceeding without Bearer header:", err.message);
  }
}

const URLS = [`${BASE}/api/events`, `${BASE}/api/events/1`];
const WARMUP = 20;
const RUNS = 200;

const pct = (arr, p) => arr[Math.min(arr.length - 1, Math.floor(arr.length * p))];

for (const url of URLS) {
  const times = [];
  for (let i = 0; i < WARMUP + RUNS; i++) {
    const t0 = performance.now();
    const headers = TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {};
    const res = await fetch(url, { headers });
    await res.arrayBuffer(); // wait for the full body
    const ms = performance.now() - t0;
    if (res.status !== 200) throw new Error(`${url} -> ${res.status}`);
    if (i >= WARMUP) times.push(ms);
  }
  times.sort((a, b) => a - b);
  const avg = times.reduce((a, b) => a + b, 0) / times.length;
  console.log(
    `${url}\n  avg ${avg.toFixed(1)} ms | p50 ${pct(times, 0.5).toFixed(1)} ms | p95 ${pct(times, 0.95).toFixed(1)} ms`
  );
}
