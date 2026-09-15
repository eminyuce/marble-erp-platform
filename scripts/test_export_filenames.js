#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");

const appJsPath = path.resolve(__dirname, "../src/main/resources/static/js/app.js");
const src = fs.readFileSync(appJsPath, "utf8");
const start = src.indexOf("function toAsciiTurkishFilename");
const end = src.indexOf("\nfunction exportDropdownHtml");
if (start < 0 || end < 0) {
    throw new Error("Could not extract export filename helpers from app.js");
}
const helpers = new Function("window", "document", "Tabulator", `${src.slice(start, end)}
return { toAsciiTurkishFilename, buildExportFilename, formatExportTimestamp, downloadTable };
`)({}, { querySelector() { return null; } }, undefined);
const { toAsciiTurkishFilename, buildExportFilename, downloadTable } = helpers;

function assertEqual(actual, expected, message) {
    if (actual !== expected) {
        throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
    }
}

function assertMatch(actual, regex, message) {
    if (!regex.test(actual)) {
        throw new Error(`${message}: ${JSON.stringify(actual)} did not match ${regex}`);
    }
}

function assertNotContains(actual, needle, message) {
    if (actual.includes(needle)) {
        throw new Error(`${message}: ${JSON.stringify(actual)} should not contain ${JSON.stringify(needle)}`);
    }
}

const sample = new Date(2026, 8, 15, 12, 21, 45);
const filenamePattern = /^[a-z0-9_-]+_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.[a-z0-9]+$/;

assertEqual(toAsciiTurkishFilename("kullanıcılar"), "kullanicilar", "turkish users stem");
assertEqual(
    buildExportFilename("bloklar", "xlsx", sample),
    "bloklar_2026-09-15_12-21-45.xlsx",
    "blocks xlsx"
);
assertEqual(
    buildExportFilename("siparişler", "csv", sample),
    "siparisler_2026-09-15_12-21-45.csv",
    "orders csv"
);
assertEqual(
    buildExportFilename("faturalar", "pdf", sample),
    "faturalar_2026-09-15_12-21-45.pdf",
    "invoices pdf"
);
assertEqual(
    buildExportFilename("ürünler.csv", ".xlsx", sample),
    "urunler_2026-09-15_12-21-45.xlsx",
    "products strips prior extension"
);
assertEqual(
    buildExportFilename("Şantiye Projeleri!", "XLSX", sample),
    "santiye_projeleri_2026-09-15_12-21-45.xlsx",
    "transliterates and strips special characters"
);
assertEqual(
    buildExportFilename("siparisler", "csv", new Date(2026, 0, 2, 0, 5, 9)),
    "siparisler_2026-01-02_00-05-09.csv",
    "midnight 24-hour"
);
assertEqual(
    buildExportFilename("siparisler", "csv", new Date(2026, 0, 2, 23, 59, 59)),
    "siparisler_2026-01-02_23-59-59.csv",
    "late evening 24-hour"
);

const live = buildExportFilename("kullanicilar", "csv");
assertMatch(live, filenamePattern, "live filename format");
assertNotContains(live, ":", "no colon in live filename");
assertMatch(live, /^kullanicilar_/, "live entity prefix");
assertMatch(live, /\.csv$/, "live csv extension");

[
    ["bloklar", "csv"],
    ["uretim_emirleri", "xlsx"],
    ["plakalar", "csv"],
    ["kesim_emirleri", "xlsx"],
    ["projeler", "csv"],
    ["satinalma", "xlsx"],
    ["satislar", "csv"],
    ["kullanicilar", "xlsx"]
].forEach(([entity, ext]) => {
    const name = buildExportFilename(entity, ext, sample);
    assertMatch(name, filenamePattern, `${entity} ${ext} format`);
    assertEqual(name, `${entity}_2026-09-15_12-21-45.${ext}`, `${entity} ${ext} exact`);
    assertNotContains(name, ":", `${entity} has no colon`);
});

const csvCalls = [];
downloadTable({
    download(format, filename) {
        csvCalls.push({ format, filename });
    }
}, "bloklar", "csv");
assertEqual(csvCalls.length, 1, "csv download invoked");
assertEqual(csvCalls[0].format, "csv", "csv format");
assertMatch(csvCalls[0].filename, /^bloklar_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.csv$/, "csv download filename");

const xlsxCalls = [];
downloadTable({
    download(format, filename, opts) {
        xlsxCalls.push({ format, filename, opts });
    }
}, "kullanicilar", "xlsx");
assertEqual(xlsxCalls[0].format, "xlsx", "xlsx format");
assertMatch(xlsxCalls[0].filename, /^kullanicilar_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.xlsx$/, "xlsx download filename");
assertEqual(xlsxCalls[0].opts.sheetName, "Veri", "xlsx sheet name unchanged");

console.log("export filename checks passed");
