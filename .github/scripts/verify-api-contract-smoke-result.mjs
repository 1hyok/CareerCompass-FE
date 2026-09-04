#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import {
    collectXmlFiles,
    parseAndroidTestXml,
} from "./render-android-test-results.mjs";

const TARGET_CLASS = "com.careercompass.core.network.service.ApiWireContractSmokeTest";

export function verifyApiContractSmokeDocuments(documents) {
    const testcases = documents.flatMap(({ file, xml }) =>
        parseAndroidTestXml(xml, { file, validateSuiteCounters: true }).testcases,
    );
    const smokeResults = testcases.filter((testcase) => testcase.className === TARGET_CLASS);
    const executed = smokeResults.length;
    const skipped = smokeResults.filter((testcase) => testcase.status === "skipped").length;
    const failed = smokeResults.filter(
        (testcase) => testcase.status === "failure" || testcase.status === "error",
    ).length;

    if (executed < 1) {
        throw new Error(`${TARGET_CLASS} XML 실행 결과가 최소 1건 필요합니다.`);
    }
    if (skipped > 0) {
        throw new Error(`${TARGET_CLASS} XML에 skipped 결과가 ${skipped}건 있습니다.`);
    }
    if (failed > 0) {
        throw new Error(`${TARGET_CLASS} XML에 failure/error 결과가 ${failed}건 있습니다.`);
    }

    return { executed, skipped };
}

export async function verifyApiContractSmokeResult(reportRoot) {
    const files = await collectXmlFiles(reportRoot);
    if (files.length === 0) {
        throw new Error(`API contract smoke XML 결과가 없습니다: ${reportRoot}`);
    }

    const documents = await Promise.all(
        files.map(async (file) => ({ file, xml: await fs.readFile(file, "utf8") })),
    );
    return verifyApiContractSmokeDocuments(documents);
}

async function main() {
    const [reportRoot] = process.argv.slice(2);
    if (!reportRoot) {
        throw new Error("API contract smoke XML 결과 경로가 필요합니다.");
    }

    const result = await verifyApiContractSmokeResult(reportRoot);
    console.log(
        `API contract smoke results verified: ${result.executed} executed, ${result.skipped} skipped`,
    );
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    });
}
