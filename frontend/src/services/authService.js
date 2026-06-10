import { apiRequest } from "./api.js";

export function login(username, password) {
  return apiRequest("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
}
