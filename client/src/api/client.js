import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach JWT access token if available
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('quicktix_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Concurrency queue for silent token refresh
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

const clearSession = () => {
  localStorage.removeItem('quicktix_token');
  localStorage.removeItem('quicktix_refresh_token');
  localStorage.removeItem('quicktix_user');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
};

// Response interceptor: extract response body and silently refresh token on 401
api.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config;

    // Check if error is 401 Unauthorized
    if (error.response?.status === 401 && originalRequest) {
      const requestUrl = originalRequest.url || '';

      // Do not attempt refresh on auth endpoints (login, register, refresh itself)
      if (
        requestUrl.includes('/auth/login') ||
        requestUrl.includes('/auth/register') ||
        requestUrl.includes('/auth/refresh')
      ) {
        if (requestUrl.includes('/auth/refresh')) {
          clearSession();
        }
        const message =
          error.response?.data?.message ||
          error.response?.data?.error ||
          'Authentication failed';
        return Promise.reject(new Error(message));
      }

      // If already retried once and failed, abort to prevent infinite loop
      if (originalRequest._retry) {
        clearSession();
        return Promise.reject(error);
      }

      const refreshToken = localStorage.getItem('quicktix_refresh_token');
      if (!refreshToken) {
        clearSession();
        return Promise.reject(error);
      }

      // If a refresh is already in progress, enqueue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Raw axios call to avoid interceptor recursion
        const res = await axios.post(`${baseURL}/auth/refresh`, {
          refreshToken,
        });

        // Backend response format: { success: true, message: "...", data: { token, accessToken, refreshToken, user } }
        const authPayload = res.data?.data || res.data;
        const newAccessToken = authPayload.token || authPayload.accessToken;
        const newRefreshToken = authPayload.refreshToken;

        if (!newAccessToken) {
          throw new Error('No access token returned from refresh');
        }

        localStorage.setItem('quicktix_token', newAccessToken);
        if (newRefreshToken) {
          localStorage.setItem('quicktix_refresh_token', newRefreshToken);
        }

        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        processQueue(null, newAccessToken);
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearSession();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      'An unexpected error occurred';
    return Promise.reject(new Error(message));
  }
);

export default api;
