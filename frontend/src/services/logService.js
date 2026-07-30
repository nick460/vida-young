import { apiRequest } from "./api.js";

export function listLogFiles() {
  return apiRequest("/api/logs");
}

export function getLogFile(fileName, lines = 600) {
  return apiRequest(`/api/logs/${encodeURIComponent(fileName)}?lines=${lines}`);
}
