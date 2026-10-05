import { mountApp } from "shared";

// Compose Multiplatform renders into a canvas, so the React root is replaced
// by a plain container element that Compose takes over.
const container = document.getElementById("root");

if (!container) {
  throw new Error("Missing #root container in index.html");
}

mountApp(container.id);