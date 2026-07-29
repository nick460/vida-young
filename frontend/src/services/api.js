const API_URL = import.meta.env.VITE_API_URL || "";

export async function apiRequest(path, options = {}) {
  const { auth, ...fetchOptions } = options;
  const token = localStorage.getItem("vy_token");
  const isFormData = fetchOptions.body instanceof FormData;
  const includeAuth = auth !== false;
  const headers = {
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(fetchOptions.headers || {})
  };

  if (includeAuth && token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...fetchOptions,
    headers
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Error HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
