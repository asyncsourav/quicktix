const BASE = "http://localhost:8080";

async function main() {
  // 1. Fetch current cached event 1
  const beforeRes = await fetch(`${BASE}/api/events/1`);
  const beforeData = await beforeRes.json();
  console.log("Before update:", beforeData.data.title);

  // 2. Login as organizer
  const loginRes = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "organizer@quicktix.com", password: "password123" }),
  });
  const loginData = await loginRes.json();
  const token = loginData.data.accessToken;

  // 3. Update event 1
  const updatedTitle = `Coldplay Tour (Updated ${Date.now().toString().slice(-4)})`;
  const updateRes = await fetch(`${BASE}/api/events/1`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      title: updatedTitle,
      description: "Updated description to verify Redis @CacheEvict.",
      venueId: beforeData.data.venueId || 1,
      startTime: "2026-11-20T19:30:00",
      category: "Concert",
      basePrice: 95.0,
    }),
  });
  const updateData = await updateRes.json();
  console.log("PUT status:", updateRes.status, "Title sent:", updatedTitle);

  // 4. Fetch event 1 again to verify eviction served fresh data
  const afterRes = await fetch(`${BASE}/api/events/1`);
  const afterData = await afterRes.json();
  console.log("After update:", afterData.data.title);

  if (afterData.data.title === updatedTitle) {
    console.log("Eviction check passed: Fresh data returned immediately.");
  } else {
    console.error("Eviction check failed: Stale data returned.");
  }
}

main().catch(console.error);
