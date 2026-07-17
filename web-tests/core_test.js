import "../docs/core.js";
const core = globalThis.JerusalemCore;

function equal(actual, expected, label) {
  if (actual !== expected) throw new Error(`${label}: expected ${expected}, received ${actual}`);
}

Deno.test("fader values map to the shared UI scale", () => {
  equal(core.levelRatio(-32768), 0, "minus infinity");
  equal(core.levelRatio(-6000), 0, "minus 60 dB");
  equal(core.levelRatio(1000), 1, "plus 10 dB");
});

Deno.test("mixer values use console-friendly labels", () => {
  equal(core.formatDb(-32768), "−∞", "minus infinity label");
  equal(core.formatDb(-600), "-6.0", "negative level label");
  equal(core.formatDb(0), "+0.0", "zero level label");
  equal(core.panText(-12), "L12", "left pan");
  equal(core.panText(0), "C", "center pan");
  equal(core.panText(9), "R9", "right pan");
});
