async function testAuth() {
  console.log("--- 1. Testing Login & CORS ---");
  const loginRes = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Origin': 'http://localhost:5173'
    },
    body: JSON.stringify({ email: 'organizer@quicktix.com', password: 'password123' })
  });

  console.log('Login Status:', loginRes.status);
  console.log('CORS Allow-Origin:', loginRes.headers.get('access-control-allow-origin'));
  console.log('CORS Allow-Credentials:', loginRes.headers.get('access-control-allow-credentials'));
  
  const setCookie = loginRes.headers.get('set-cookie');
  console.log('Set-Cookie Header:', setCookie);

  const loginJson = await loginRes.json();
  console.log('User Profile in JSON:', loginJson.data?.user?.email, '| Role:', loginJson.data?.user?.role);
  console.log('Access Token present in JSON:', !!loginJson.data?.accessToken);

  // Extract refreshToken cookie value
  const cookieMatch = setCookie ? setCookie.match(/refreshToken=([^;]+)/) : null;
  const cookieVal = cookieMatch ? cookieMatch[1] : null;

  if (cookieVal) {
    console.log('\n--- 2. Testing /api/auth/refresh with HttpOnly Cookie ---');
    const refreshRes = await fetch('http://localhost:8080/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Cookie': 'refreshToken=' + cookieVal,
        'Origin': 'http://localhost:5173'
      }
    });

    console.log('Refresh Status:', refreshRes.status);
    const newSetCookie = refreshRes.headers.get('set-cookie');
    console.log('Rotated Set-Cookie Header:', newSetCookie);
    const refreshJson = await refreshRes.json();
    console.log('New Access Token present:', !!refreshJson.data?.accessToken);

    // Test logout with the rotated cookie
    const newCookieMatch = newSetCookie ? newSetCookie.match(/refreshToken=([^;]+)/) : null;
    const newCookieVal = newCookieMatch ? newCookieMatch[1] : cookieVal;

    console.log('\n--- 3. Testing /api/auth/logout with Cookie ---');
    const logoutRes = await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      headers: {
        'Cookie': 'refreshToken=' + newCookieVal,
        'Origin': 'http://localhost:5173'
      }
    });

    console.log('Logout Status:', logoutRes.status);
    console.log('Logout Set-Cookie (cleared with Max-Age=0):', logoutRes.headers.get('set-cookie'));
    const logoutJson = await logoutRes.json();
    console.log('Logout Message:', logoutJson.message);

    // Verify token reuse fails after logout
    console.log('\n--- 4. Testing reuse of logged-out token (Revoked) ---');
    const reuseRes = await fetch('http://localhost:8080/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Cookie': 'refreshToken=' + newCookieVal,
        'Origin': 'http://localhost:5173'
      }
    });
    console.log('Reuse Status (should be 401):', reuseRes.status);
    const reuseJson = await reuseRes.json();
    console.log('Reuse Error Message:', reuseJson.message);
  }
}

testAuth().catch(console.error);
