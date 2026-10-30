import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";

createRoot(document.getElementById("akar")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
