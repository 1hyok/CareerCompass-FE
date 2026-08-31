#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import {
    validateCiTestPlanImpact,
    validateCiTestPlanSources,
} from "./ci-test-plan.mjs";
import { resolveAndroidTestPlan } from "./resolve-android-test-plan.mjs";
import { changedPathsFromGithubFiles } from "./resolve-pr-impact.mjs";

function pullRequestFrom(payload) {
    return payload?.pull_request ?? payload;
}

export async function validatePullRequestCiTestPlan(
    payload,
    { root = process.cwd(), changedFiles = null, repository = process.env.GITHUB_REPOSITORY } = {},
) {
    const pullRequest = pullRequestFrom(payload);
    const { plan } = resolveAndroidTestPlan(pullRequest, { repository });
    await validateCiTestPlanSources(plan, { root });
    if (changedFiles !== null) {
        await validateCiTestPlanImpact(plan, changedPathsFromGithubFiles(changedFiles), {
            root,
        });
    }
    return { plan };
}

async function main() {
    const [payloadPath, rootArgument, changedFilesPath] = process.argv.slice(2);
    if (!payloadPath) {
        throw new Error("pull request JSON 경로가 필요합니다.");
    }
    const payload = JSON.parse(await fs.readFile(path.resolve(payloadPath), "utf8"));
    const changedFiles = changedFilesPath
        ? JSON.parse(await fs.readFile(path.resolve(changedFilesPath), "utf8"))
        : null;
    const result = await validatePullRequestCiTestPlan(payload, {
        root: path.resolve(rootArgument ?? process.cwd()),
        changedFiles,
    });
    console.log(`CI Test Plan: androidTest mode=${result.plan.androidTest.mode}`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    });
}
