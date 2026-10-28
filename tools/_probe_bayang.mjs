import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync("web/index.html", "utf8");
const dom = new JSDOM(html, {runScripts: "dangerously", pretendToBeVisual: true,
  beforeParse(w) {
    const F = class extends Date {
      constructor(...a) { a.length === 0 ? super(Date.UTC(2026, 9, 28, 18, 0)) : super(...a); }
      static now() { return Date.UTC(2026, 9, 28, 18, 0); }
    };
    w.Date = F;
  }});
const w = dom.window;
await new Promise(r => setTimeout(r, 600));
console.log("getHours():", new w.Date().getHours());
console.log("display:", JSON.stringify(w.document.getElementById("bayangan").style.display));
console.log("children:", w.document.getElementById("bayangan").children.length);
