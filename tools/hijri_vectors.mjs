// Reference Hijri dates from ICU (Node's Intl), to test the Kotlin engine against an independent implementation.
const parts = (cal, date) => {
  const f = new Intl.DateTimeFormat(`en-u-ca-${cal}-nu-latn`, { timeZone: "UTC", year: "numeric", month: "numeric", day: "numeric" });
  const p = Object.fromEntries(f.formatToParts(date).map((x) => [x.type, x.value]));
  return [parseInt(p.year, 10), parseInt(p.month, 10), parseInt(p.day, 10)];
};
const out = { umalqura: [], civil: [] };
let seed = 20261106;
const rand = () => ((seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648);
const days = new Set();
const start = Date.UTC(1990, 0, 1), end = Date.UTC(2076, 11, 31);
while (days.size < 400) days.add(Math.floor(start / 864e5 + rand() * ((end - start) / 864e5)));
for (const iso of ["2024-03-11", "2024-04-10", "2025-03-01", "2025-03-30", "2026-02-18", "2026-11-06", "2027-02-08"]) days.add(Date.parse(iso) / 864e5);
for (const d of [...days].sort((a, b) => a - b)) {
  const date = new Date(d * 864e5);
  const iso = date.toISOString().slice(0, 10);
  out.umalqura.push([iso, ...parts("islamic-umalqura", date)]);
  out.civil.push([iso, ...parts("islamic-civil", date)]);
}
console.log(JSON.stringify(out));
