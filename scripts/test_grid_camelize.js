#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");

const appJsPath = path.resolve(__dirname, "../src/main/resources/static/js/app.js");
const src = fs.readFileSync(appJsPath, "utf8");
const start = src.indexOf("function camelizeTabulatorRows");
const end = src.indexOf("\nfunction toAsciiTurkishFilename");
if (start < 0 || end < 0) {
    throw new Error("Could not extract grid helpers from app.js");
}
const helpers = new Function(`${src.slice(start, end)}
return { camelizeTabulatorRows, gridText, gridNumber, gridArea };
`)();
const { camelizeTabulatorRows, gridText, gridNumber, gridArea } = helpers;

function assertEqual(actual, expected, message) {
    if (actual !== expected) {
        throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
    }
}

const rows = camelizeTabulatorRows({
    data: [{
        cut_order_no: "CUT-1",
        project_name: "Villa",
        item_count: 4,
        total_area_m2: 3.84,
        status: "COMPLETED"
    }]
});

assertEqual(rows.data[0].cutOrderNo, "CUT-1", "cut_order_no");
assertEqual(rows.data[0].projectName, "Villa", "project_name");
assertEqual(rows.data[0].itemCount, 4, "item_count");
assertEqual(rows.data[0].totalAreaM2, 3.84, "total_area_m2");
assertEqual(gridText(undefined), "—", "gridText(undefined)");
assertEqual(gridText(null), "—", "gridText(null)");
assertEqual(gridText(""), "—", "gridText('')");
assertEqual(gridText("CUT-1"), "CUT-1", "gridText(value)");
assertEqual(gridNumber(undefined, (n) => n.toFixed(2)), "—", "gridNumber(undefined)");
assertEqual(gridNumber(3.84, (n) => n.toFixed(2)), "3.84", "gridNumber(format)");
assertEqual(gridArea(undefined), "—", "gridArea(undefined)");
assertEqual(gridArea(3.8), "3.80 m²", "gridArea(value)");

const slabRows = camelizeTabulatorRows({
    data: [{ slabCode: "SLB-1", surfaceAreaM2: 2.5 }]
});
assertEqual(slabRows.data[0].slabCode, "SLB-1", "already-camelCase slabCode");

console.log("grid helper checks passed");
