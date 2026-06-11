import React from "react";
import { createRoot } from "react-dom/client";

function App() {
  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: 32, lineHeight: 1.5 }}>
      <h1>KmpPrinter Web Sample</h1>
      <p>This minimal web sample keeps the repository buildable while the main sample UI lives in Compose Multiplatform.</p>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
