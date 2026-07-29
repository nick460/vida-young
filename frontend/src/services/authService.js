import { apiRequest } from "./api.js";

export function login(username, password) {
  return apiRequest("/api/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify({ username, password })
  });
}
