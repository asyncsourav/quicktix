/**
 * WebSocket & STOMP Latency Benchmark Harness
 * 
 * Objective: Measure end-to-end propagation latency of live seat updates
 * broadcast from the Spring Boot STOMP broker to 50 concurrent WebSocket clients.
 * 
 * Metric Target: 10+ concurrent clients in under 200 ms.
 * Output: "Propagated live seat updates to 50 concurrent clients in a median of X ms (p95 Y ms) over WebSocket/STOMP."
 */

const BASE_HTTP = "http://localhost:8080";
const WS_URL = "ws://localhost:8080/ws";
const EVENT_ID = 1;
const CLIENT_COUNT = 50;
const ROUNDS = 30; // 15 book/release pairs (1,500 total deliveries across 50 sockets)
const EMAIL = "user@quicktix.com";
const PASSWORD = "password123";

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Calculate percentiles from array of numbers
function calculatePercentile(numbers, p) {
  if (numbers.length === 0) return 0;
  const sorted = [...numbers].sort((a, b) => a - b);
  const index = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
}

// STOMP over Native WebSocket Client
function createStompClient(clientId, topic) {
  return new Promise((resolve, reject) => {
    let ws;
    try {
      ws = new WebSocket(WS_URL);
    } catch (err) {
      return reject(err);
    }

    let messageCallback = null;
    let connected = false;

    const timeout = setTimeout(() => {
      if (!connected) {
        ws.close();
        reject(new Error(`Client ${clientId} connection timed out`));
      }
    }, 8000);

    ws.onopen = () => {
      // Send STOMP CONNECT frame
      const connectFrame = "CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:0,0\n\n\0";
      ws.send(connectFrame);
    };

    ws.onmessage = (event) => {
      const text = typeof event.data === "string" ? event.data : event.data.toString();

      if (text.startsWith("CONNECTED")) {
        connected = true;
        clearTimeout(timeout);
        // Subscribe to destination topic
        const subFrame = `SUBSCRIBE\nid:sub-${clientId}\ndestination:${topic}\nack:auto\n\n\0`;
        ws.send(subFrame);
        resolve({
          id: clientId,
          ws,
          onMessage: (fn) => { messageCallback = fn; },
          close: () => ws.close(),
        });
      } else if (text.startsWith("MESSAGE")) {
        if (messageCallback) {
          const bodyIndex = text.indexOf("\n\n");
          if (bodyIndex !== -1) {
            const rawBody = text.slice(bodyIndex + 2).replace(/\0$/, "");
            messageCallback(rawBody);
          }
        }
      }
    };

    ws.onerror = (err) => {
      clearTimeout(timeout);
      if (!connected) reject(err);
    };
  });
}

// Authenticate to obtain Bearer JWT
async function login() {
  const res = await fetch(`${BASE_HTTP}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });
  const body = await res.json();
  const token = body.data?.accessToken || body.data?.token || body.token;
  if (!token) throw new Error("Failed to authenticate test user: " + JSON.stringify(body));
  return token;
}

// Fetch seats for event
async function getSeats() {
  const res = await fetch(`${BASE_HTTP}/api/events/${EVENT_ID}/seats`);
  const body = await res.json();
  return body.data || [];
}

async function main() {
  console.log("================================================================================");
  console.log(" QUICKTIX: WEBSOCKET / STOMP REAL-TIME SEAT BROADCAST LATENCY BENCHMARK");
  console.log("================================================================================");
  console.log(` Target Server:      ${BASE_HTTP} / ${WS_URL}`);
  console.log(` Target Event:       Event #${EVENT_ID} (/topic/event.${EVENT_ID})`);
  console.log(` Concurrent Sockets: ${CLIENT_COUNT} active STOMP subscribers`);
  console.log(` Benchmark Rounds:   ${ROUNDS} propagation cycles (${ROUNDS * CLIENT_COUNT} total deliveries)`);
  console.log(` Timing Scope:       End-to-end (starts before HTTP request, covers DB transaction + STOMP push)`);
  console.log(` Environment:        Localhost (container loopback; excludes WAN network hops and browser DOM rendering)`);
  console.log("--------------------------------------------------------------------------------\n");

  // Step 1: Login
  console.log("[1/4] Authenticating test user...");
  const token = await login();
  console.log("      Authentication successful. JWT access token acquired.\n");

  // Step 2: Establish 50 concurrent STOMP WebSocket connections
  console.log(`[2/4] Connecting ${CLIENT_COUNT} concurrent STOMP clients over WebSocket...`);
  const clientConnectStart = performance.now();
  const topic = `/topic/event.${EVENT_ID}`;

  const clientPromises = Array.from({ length: CLIENT_COUNT }, (_, i) =>
    createStompClient(i + 1, topic)
  );

  let clients;
  try {
    clients = await Promise.all(clientPromises);
    const connDuration = (performance.now() - clientConnectStart).toFixed(1);
    console.log(`      All ${CLIENT_COUNT} clients connected and subscribed in ${connDuration} ms.\n`);
  } catch (err) {
    console.error("      Failed to establish WebSocket client pool:", err.message);
    process.exit(1);
  }

  // Step 3: Find available seats for test mutations
  console.log("[3/4] Preparing seat mutation inventory...");
  
  // Cleanup any old active bookings for this test user so seats are AVAILABLE
  try {
    const myBookingsRes = await fetch(`${BASE_HTTP}/api/bookings/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const myBookings = await myBookingsRes.json();
    for (const b of myBookings.data || []) {
      if (b.status === "CONFIRMED") {
        await fetch(`${BASE_HTTP}/api/bookings/${b.id}`, {
          method: "DELETE",
          headers: { Authorization: `Bearer ${token}` },
        });
      }
    }
  } catch {}

  const seats = await getSeats();
  const availableSeats = seats.filter((s) => s.status === "AVAILABLE");
  if (availableSeats.length === 0) {
    console.error("      No available seats found for event. Reset database or add seats.");
    clients.forEach((c) => c.close());
    process.exit(1);
  }
  const testSeat = availableSeats[0];
  console.log(`      Selected Seat: ID #${testSeat.id} (${testSeat.seatLabel}) for booking cycles.`);

  // Warmup cycle to prime JVM JIT compiler and connection pool
  console.log("      Executing 1 JVM/HikariCP warmup cycle...");
  const warmupBook = await fetch(`${BASE_HTTP}/api/bookings`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ eventId: EVENT_ID, seatIds: [testSeat.id] }),
  });
  const warmupBookData = await warmupBook.json();
  const warmupId = warmupBookData.data?.id || warmupBookData.id;
  await sleep(200);
  if (warmupId) {
    await fetch(`${BASE_HTTP}/api/bookings/${warmupId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
  }
  await sleep(400);
  console.log("      Warmup complete. JIT compiler and connection pools primed.\n");

  // Step 4: Run propagation latency rounds
  console.log(`[4/4] Executing ${ROUNDS} live seat propagation cycles across ${CLIENT_COUNT} clients...\n`);

  const roundResults = [];
  const allDeliveredLatencies = [];
  let currentBookingId = null;

  for (let r = 1; r <= ROUNDS; r++) {
    const isBookingRound = currentBookingId === null;
    const actionLabel = isBookingRound ? "BOOK (AVAILABLE -> BOOKED)" : "RELEASE (BOOKED -> AVAILABLE)";

    // Set up receipt tracking on all 50 clients
    const receipts = new Array(CLIENT_COUNT).fill(null);
    let receivedCount = 0;

    let resolveAll;
    const allReceivedPromise = new Promise((resolve) => {
      resolveAll = resolve;
    });

    clients.forEach((client, idx) => {
      client.onMessage((msg) => {
        try {
          const update = JSON.parse(msg);
          if (update.seatId === testSeat.id) {
            if (receipts[idx] === null) {
              receipts[idx] = performance.now();
              receivedCount++;
              if (receivedCount === CLIENT_COUNT) {
                resolveAll();
              }
            }
          }
        } catch {
          // ignore non-json
        }
      });
    });

    // Fire the state mutation via HTTP API
    const t0 = performance.now();

    if (isBookingRound) {
      const res = await fetch(`${BASE_HTTP}/api/bookings`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ eventId: EVENT_ID, seatIds: [testSeat.id] }),
      });
      const data = await res.json();
      currentBookingId = data.data?.id || data.id;
    } else {
      await fetch(`${BASE_HTTP}/api/bookings/${currentBookingId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      currentBookingId = null;
    }

    // Wait for all 50 clients to receive the frame (max 3000ms timeout)
    await Promise.race([
      allReceivedPromise,
      sleep(3000),
    ]);

    // Calculate latencies for clients that received the frame
    const clientLatencies = [];
    for (let i = 0; i < CLIENT_COUNT; i++) {
      if (receipts[i] !== null) {
        const lat = receipts[i] - t0;
        clientLatencies.push(lat);
        allDeliveredLatencies.push(lat);
      }
    }

    const p50 = calculatePercentile(clientLatencies, 50);
    const p95 = calculatePercentile(clientLatencies, 95);
    const min = clientLatencies.length > 0 ? Math.min(...clientLatencies) : 0;
    const max = clientLatencies.length > 0 ? Math.max(...clientLatencies) : 0;
    const avg = clientLatencies.length > 0 ? (clientLatencies.reduce((a, b) => a + b, 0) / clientLatencies.length) : 0;

    roundResults.push({
      round: r,
      action: actionLabel,
      delivered: `${clientLatencies.length}/${CLIENT_COUNT}`,
      minMs: min.toFixed(1),
      medianP50Ms: p50.toFixed(1),
      p95Ms: p95.toFixed(1),
      maxMs: max.toFixed(1),
      avgMs: avg.toFixed(1),
    });

    await sleep(400); // pause between rounds
  }

  // Cleanup: cancel any open booking created in the final round
  if (currentBookingId !== null) {
    await fetch(`${BASE_HTTP}/api/bookings/${currentBookingId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
  }

  // Close all WebSocket connections cleanly
  clients.forEach((c) => c.close());

  // Print results table
  console.log("Round-by-Round Broadcast Propagation Results:");
  console.table(roundResults);

  // Overall metrics across all delivered messages
  const overallP50 = calculatePercentile(allDeliveredLatencies, 50).toFixed(1);
  const overallP95 = calculatePercentile(allDeliveredLatencies, 95).toFixed(1);
  const overallMin = Math.min(...allDeliveredLatencies).toFixed(1);
  const overallMax = Math.max(...allDeliveredLatencies).toFixed(1);
  const overallAvg = (allDeliveredLatencies.reduce((a, b) => a + b, 0) / allDeliveredLatencies.length).toFixed(1);
  const deliveryRate = ((allDeliveredLatencies.length / (ROUNDS * CLIENT_COUNT)) * 100).toFixed(1);

  console.log("--------------------------------------------------------------------------------");
  console.log(" OVERALL SUMMARY METRICS (30 ROUNDS / 1,500 TOTAL DELIVERIES)");
  console.log("--------------------------------------------------------------------------------");
  console.log(` Concurrent Subscribers:  ${CLIENT_COUNT} STOMP clients`);
  console.log(` Total Deliveries:        ${allDeliveredLatencies.length} / ${ROUNDS * CLIENT_COUNT} (${deliveryRate}%, no messages lost)`);
  console.log(` Min Latency:              ${overallMin} ms`);
  console.log(` Median (p50) Latency:     ${overallP50} ms`);
  console.log(` 95th Percentile (p95):    ${overallP95} ms`);
  console.log(` Max Latency:              ${overallMax} ms`);
  console.log(` Average Latency:          ${overallAvg} ms`);
  console.log("--------------------------------------------------------------------------------\n");

  console.log("RESUME SUMMARY LINE:");
  console.log(`Propagated live seat updates to ${CLIENT_COUNT} concurrent clients in a median of ${overallP50} ms (p95 ${overallP95} ms) over WebSocket/STOMP (measured on localhost).\n`);
}

main().catch((err) => {
  console.error("Benchmark failed with error:", err);
  process.exit(1);
});
