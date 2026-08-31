import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    ANDROID_TEST_DEVICES,
    DEPENDABOT_BOT_ID,
    ciTestPlanDigest,
    inspectAndroidTestImpact,
    inspectCiTestPlan,
    inspectPullRequestCiTestPlan,
    isTrustedDependabotPullRequest,
    trustedPullRequestActorFromEnvironment,
    validateCiTestPlanImpact,
    validateCiTestPlanSources,
} from "./ci-test-plan.mjs";
import { resolveAndroidTestPlan } from "./resolve-android-test-plan.mjs";
import { validatePullRequestCiTestPlan } from "./validate-pr-ci-test-plan.mjs";

const selectedBody = `
## CI Test Plan

\`\`\`json
{
  "androidTest": {
    "mode": "selected",
    "reason": "변경된 receiver 완료 경계를 실제 Android 런타임에서 확인",
    "tests": [{
      "path": "app/src/androidTest/kotlin/com/example/ReceiverTest.kt",
      "selector": "com.example.ReceiverRuntimeTest#completion",
      "device": "api30"
    }]
  }
}
\`\`\`
`;

const repository = "Team-CamBridge/CareerCompass-FE";
const dependabotActor = { login: "dependabot[bot]", type: "Bot", id: DEPENDABOT_BOT_ID };

function dependabotPullRequest(overrides = {}) {
    return {
        number: 2,
        title: "chore(deps): bump actions/checkout",
        body: "",
        user: { ...dependabotActor },
        head: {
            ref: "dependabot/github_actions/develop/actions-checkout-7",
            sha: "a".repeat(40),
            repo: { full_name: repository },
        },
        base: { ref: "develop", repo: { full_name: repository } },
        ...overrides,
    };
}

test("none, selected, full 세 모드만 받는다", () => {
    const selected = inspectCiTestPlan(selectedBody, { pullRequestNumber: 7 });
    assert.equal(selected.valid, true);
    assert.equal(selected.plan.androidTest.mode, "selected");

    const invalid = inspectCiTestPlan(selectedBody.replace('"selected"', '"maybe"'));
    assert.equal(invalid.valid, false);
});

test("selected는 명시적 파일, FQCN#method, lane을 요구한다", () => {
    const invalid = inspectCiTestPlan(
        selectedBody.replace("com.example.ReceiverRuntimeTest#completion", "ReceiverTest#completion"),
    );
    assert.equal(invalid.valid, false);
    assert.match(invalid.errors.join("\n"), /fully-qualified/);
});

test("selected는 모든 managed API lane을 허용하고 미지원 device를 거부한다", () => {
    assert.deepEqual(ANDROID_TEST_DEVICES, ["api26", "api30", "api34", "api36"]);
    for (const device of ANDROID_TEST_DEVICES) {
        assert.equal(inspectCiTestPlan(selectedBody.replace('"api30"', `"${device}"`)).valid, true);
    }

    const invalid = inspectCiTestPlan(selectedBody.replace('"api30"', '"api35"'));
    assert.equal(invalid.valid, false);
    assert.match(invalid.errors.join("\n"), /api26, api30, api34, api36/);
});

test("PR template은 parser가 허용하는 managed API lane을 모두 안내한다", async () => {
    const template = await fs.readFile(".github/PULL_REQUEST_TEMPLATE.md", "utf8");
    assert.ok(template.includes(`실행 lane(${ANDROID_TEST_DEVICES.join("/")})`));
});

test("선택 selector의 package, class, @Test method를 현재 revision에서 확인한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-plan-"));
    const file = path.join(root, "app/src/androidTest/kotlin/com/example/ReceiverTest.kt");
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(
        file,
        "package com.example\n\nclass ReceiverRuntimeTest {\n @Test fun completion() = Unit\n}\n",
    );
    const inspection = inspectCiTestPlan(selectedBody);
    await assert.doesNotReject(validateCiTestPlanSources(inspection.plan, { root }));
    await assert.rejects(
        validateCiTestPlanSources(
            {
                androidTest: {
                    ...inspection.plan.androidTest,
                    tests: [
                        {
                            ...inspection.plan.androidTest.tests[0],
                            selector: "com.example.ReceiverRuntimeTest#missing",
                        },
                    ],
                },
            },
            { root },
        ),
        /@Test 메서드/,
    );
});

test("기존 PR도 계획이 없으면 실패한다", () => {
    const pullRequest = { number: 3, created_at: "2026-08-01T00:00:00Z", body: "old" };
    assert.equal(inspectPullRequestCiTestPlan(pullRequest).valid, false);
    assert.throws(() => resolveAndroidTestPlan(pullRequest), /CI Test Plan/);
});

test("trusted same-repository Dependabot gets a full plan without the human template", async () => {
    const pullRequest = dependabotPullRequest();
    const actorFromEnvironment = trustedPullRequestActorFromEnvironment({
        TRUSTED_PR_ACTOR_LOGIN: dependabotActor.login,
        TRUSTED_PR_ACTOR_TYPE: dependabotActor.type,
        TRUSTED_PR_ACTOR_ID: String(dependabotActor.id),
    });
    assert.deepEqual(actorFromEnvironment, dependabotActor);
    assert.equal(isTrustedDependabotPullRequest(pullRequest, {
        repository,
        actor: actorFromEnvironment,
    }), true);

    const resolved = resolveAndroidTestPlan(pullRequest, { repository, actor: dependabotActor });
    assert.deepEqual(resolved.plan, {
        androidTest: {
            mode: "full",
            reason: "Trusted same-repository Dependabot dependency update",
        },
    });
    assert.equal(resolved.digest, ciTestPlanDigest(resolved.plan));

    const validated = await validatePullRequestCiTestPlan(pullRequest, {
        repository,
        actor: dependabotActor,
        changedFiles: [{ filename: ".github/workflows/codeql.yml", status: "modified" }],
    });
    assert.deepEqual(validated.plan, resolved.plan);
});

test("Dependabot identity, trusted event sender, same-repository branch, and develop base are required", () => {
    const trusted = dependabotPullRequest();
    const untrusted = [
        { pullRequest: { ...trusted, user: { ...dependabotActor, type: "User" } }, actor: dependabotActor },
        { pullRequest: { ...trusted, user: { ...dependabotActor, id: 1 } }, actor: dependabotActor },
        { pullRequest: { ...trusted, user: { login: "another-bot[bot]", type: "Bot", id: 1 } }, actor: dependabotActor },
        { pullRequest: { ...trusted, head: { ...trusted.head, ref: "feature/not-dependabot" } }, actor: dependabotActor },
        { pullRequest: { ...trusted, head: { ...trusted.head, repo: { full_name: "outside/fork" } } }, actor: dependabotActor },
        { pullRequest: { ...trusted, base: { ...trusted.base, repo: { full_name: "outside/fork" } } }, actor: dependabotActor },
        { pullRequest: { ...trusted, base: { ...trusted.base, ref: "main" } }, actor: dependabotActor },
        { pullRequest: { ...trusted, head: { ...trusted.head, sha: "f".repeat(40) } }, actor: { login: "maintainer", type: "User", id: 7 } },
        { pullRequest: trusted, actor: { ...dependabotActor, id: 1 } },
        { pullRequest: trusted, actor: { ...dependabotActor, type: "User" } },
    ];

    for (const { pullRequest, actor } of untrusted) {
        assert.equal(isTrustedDependabotPullRequest(pullRequest, { repository, actor }), false);
        assert.throws(
            () => resolveAndroidTestPlan(pullRequest, { repository, actor }),
            /CI Test Plan/,
        );
    }
});

test("trusted Dependabot exception receives immutable event-sender fields in every gate", async () => {
    const [repositoryQuality, androidManagedDevice] = await Promise.all([
        fs.readFile(".github/workflows/repository-quality.yml", "utf8"),
        fs.readFile(".github/workflows/android-managed-device.yml", "utf8"),
    ]);
    for (const workflow of [repositoryQuality, androidManagedDevice]) {
        assert.match(workflow, /TRUSTED_PR_ACTOR_ID: \$\{\{ github\.event\.sender\.id \}\}/);
        assert.match(workflow, /TRUSTED_PR_ACTOR_LOGIN: \$\{\{ github\.event\.sender\.login \}\}/);
        assert.match(workflow, /TRUSTED_PR_ACTOR_TYPE: \$\{\{ github\.event\.sender\.type \}\}/);
    }
});

test("도입 이후 PR은 계획이 필수이고 digest는 결정적이다", () => {
    const pullRequest = { number: 4, created_at: "2026-09-01T00:00:00Z", body: "missing" };
    assert.equal(inspectPullRequestCiTestPlan(pullRequest).valid, false);
    const resolved = resolveAndroidTestPlan({ ...pullRequest, body: selectedBody });
    assert.equal(resolved.digest, ciTestPlanDigest(resolved.plan));
});

test("pull_request event wrapper와 direct REST payload를 같은 방식으로 검증한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-plan-wrapper-"));
    const noneBody = selectedBody
        .replace('"selected"', '"none"')
        .replace(/,\s*"tests": \[[\s\S]*?\]\s*\n  /, "\n  ");
    const pullRequest = { number: 1280, created_at: "2026-09-01T00:00:00Z", body: noneBody };

    await assert.doesNotReject(validatePullRequestCiTestPlan(pullRequest, { root }));
    await assert.doesNotReject(validatePullRequestCiTestPlan({ pull_request: pullRequest }, { root }));
});

test("하네스 변경은 full, Android 런타임 경계는 selected 이상을 강제한다", async () => {
    assert.deepEqual(
        inspectAndroidTestImpact([".github/workflows/android-managed-device.yml"]).full,
        [".github/workflows/android-managed-device.yml"],
    );
    assert.deepEqual(
        inspectAndroidTestImpact([
            ".github/scripts/classify-android-managed-device-failure.mjs",
            ".github/workflows/android-managed-device-retry.yml",
        ]).full,
        [
            ".github/scripts/classify-android-managed-device-failure.mjs",
            ".github/workflows/android-managed-device-retry.yml",
        ],
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            { androidTest: { mode: "none", reason: "CI only" } },
            [".github/workflows/android-managed-device.yml"],
        ),
        /mode=full/,
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            { androidTest: { mode: "none", reason: "runtime" } },
            ["app/src/main/AndroidManifest.xml"],
        ),
        /selected 또는 full/,
    );
});

test("변경한 @Test 파일의 selector를 selected 계획에서 빠뜨릴 수 없다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-impact-"));
    const testPath = "app/src/androidTest/java/com/example/RuntimeTest.kt";
    await fs.mkdir(path.dirname(path.join(root, testPath)), { recursive: true });
    await fs.writeFile(
        path.join(root, testPath),
        "package com.example\nclass RuntimeTest { @Test fun works() = Unit }\n",
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            {
                androidTest: {
                    mode: "selected",
                    reason: "other test",
                    tests: [
                        {
                            path: "app/src/androidTest/java/com/example/OtherTest.kt",
                            selector: "com.example.OtherTest#works",
                            device: "api30",
                        },
                    ],
                },
            },
            [testPath],
            { root },
        ),
        /변경한 @Test 메서드/,
    );
});
