(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  root.JerusalemCore = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  "use strict";
  function clamp(value, min, max) { return Math.max(min, Math.min(max, value)); }
  function levelRatio(value) { return value <= -32000 ? 0 : clamp((value / 100 + 60) / 70, 0, 1); }
  function formatDb(value) { return value <= -32000 ? "−∞" : `${value >= 0 ? "+" : ""}${(value / 100).toFixed(1)}`; }
  function panText(value) { return value === 0 ? "C" : `${value < 0 ? "L" : "R"}${Math.abs(value)}`; }
  return Object.freeze({ clamp, levelRatio, formatDb, panText });
});
