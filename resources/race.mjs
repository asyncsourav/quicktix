

/* 
  ================== concurrency test file ==================

  This file will fire exactly 10 requests to book a single ticket at the exact very time all at once

  we will get only one ticket as confirmed and booked
  the other nine will get rejeceted 
  
*/


const BASE = "http://localhost:8080";
const TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGV4QGV4YW1wbGUuY29tIiwidXNlcklkIjo0LCJyb2xlIjoiVVNFUiIsImlhdCI6MTc5MTgxMzMzNiwiZXhwIjoxNzkxODk5NzM2fQ.mJ52c1Dra1cb_fWklPF_ZYSVrtA1YYGSkcO24O7jFGw";
const EVENT_ID = 1;
const SEAT_ID = 2; // Seat A2
const N = 10; // 10 simultaneous requests


const fire = async (i) => {
  const res = await fetch(`${BASE}/api/bookings`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TOKEN}`,
    },
    body: JSON.stringify({ eventId: EVENT_ID, seatIds: [SEAT_ID] }),
  });

  const data = await res.json();
  return {
    request: i + 1,
    status: res.status,
    success: data.success ?? false,
    message: (data.message || data.error || "").slice(0, 300),
  };
};

const results = await Promise.all(Array.from({ length: N }, (_, i) => fire(i)));
console.table(results);








