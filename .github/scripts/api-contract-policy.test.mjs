import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

import { verifyApiContractSmokeDocuments } from './verify-api-contract-smoke-result.mjs';

const workflow = await readFile(
  new URL('../workflows/api-contract-smoke.yml', import.meta.url),
  'utf8',
);
const build = await readFile(
  new URL('../../core/network/build.gradle.kts', import.meta.url),
  'utf8',
);
const smoke = await readFile(
  new URL(
    '../../core/network/src/test/kotlin/com/careercompass/core/network/service/ApiWireContractSmokeTest.kt',
    import.meta.url,
  ),
  'utf8',
);
const verifier = await readFile(
  new URL('./verify-api-contract-smoke-result.mjs', import.meta.url),
  'utf8',
);

const EXPECTED_TRIGGER_BLOCK = `on:
  pull_request:
    paths:
      - 'core/network/**'
      - 'core/common/**'
      - 'core/domain/**'
      - 'core/model/**'
      - 'build-logic/**'
      - 'build.gradle.kts'
      - 'settings.gradle.kts'
      - 'gradle.properties'
      - 'gradle/**'
      - 'gradle/libs.versions.toml'
      - '.github/scripts/render-android-test-results.mjs'
      - '.github/scripts/verify-api-contract-smoke-result.mjs'
      - '.github/workflows/api-contract-smoke.yml'
  push:
    branches: [develop]
    paths:
      - 'core/network/**'
      - 'core/common/**'
      - 'core/domain/**'
      - 'core/model/**'
      - 'build-logic/**'
      - 'build.gradle.kts'
      - 'settings.gradle.kts'
      - 'gradle.properties'
      - 'gradle/**'
      - 'gradle/libs.versions.toml'
      - '.github/scripts/render-android-test-results.mjs'
      - '.github/scripts/verify-api-contract-smoke-result.mjs'
      - '.github/workflows/api-contract-smoke.yml'
  schedule:
    - cron: '41 18 * * 1,4'
  workflow_dispatch:`;
const EXPECTED_PERMISSIONS_BLOCK = `permissions:
  contents: read`;
const EXPECTED_JOB_STEP_NAMES = [
  'Clone repo',
  'Set up JDK 21',
  'Set up Gradle and validate wrapper',
  'Require Docker runtime',
  'Set up CI-only app configuration',
  'Run Retrofit wire contract smoke',
  'Verify Retrofit wire contract result',
  'Upload failure reports',
];

function namedJobBlock(source, name) {
  const lines = source.split('\n');
  const header = `  ${name}:`;
  const starts = lines.flatMap((line, index) => line === header ? [index] : []);
  if (starts.length !== 1) throw new Error(`${name} job이 정확히 하나여야 합니다.`);
  const [start] = starts;
  const end = lines.findIndex(
    (line, index) => index > start && /^  [A-Za-z0-9_-]+:\s*$/.test(line),
  );
  return lines.slice(start, end === -1 ? undefined : end).join('\n');
}

function namedStepBlock(job, name) {
  const lines = job.split('\n');
  const starts = lines.flatMap((line, index) => {
    const match = /^(\s*)- name:\s*(.+?)\s*$/.exec(line);
    if (match === null) return [];
    const candidate = match[2].replace(/^(['"])(.*)\1$/, '$2');
    return candidate === name ? [{ index, indentation: match[1].length }] : [];
  });
  if (starts.length !== 1) throw new Error(`${name} 단계가 정확히 하나여야 합니다.`);
  const [{ index: start, indentation }] = starts;
  const next = lines.findIndex(
    (line, index) => index > start && new RegExp(`^ {${indentation}}- `).test(line),
  );
  return {
    indentation,
    source: lines.slice(start, next === -1 ? undefined : next).join('\n'),
  };
}

function directJobStepNames(job) {
  return job.split('\n').flatMap((line) => {
    if (!/^ {6}- /.test(line)) return [];
    const match = /^ {6}- name:\s*(.+?)\s*$/.exec(line);
    if (match === null) {
      throw new Error('contract-smoke job의 모든 단계는 canonical name을 가져야 합니다.');
    }
    return [match[1].replace(/^(['"])(.*)\1$/, '$2')];
  });
}

function directPropertyEntries(source, indentation, name) {
  const lines = source.split('\n');
  const propertyPattern = new RegExp(`^ {${indentation}}${name}\\s*:\\s*(.*?)\\s*$`);
  return lines.flatMap((line, index) => {
    const match = propertyPattern.exec(line);
    if (match === null) return [];
    const next = lines.findIndex((candidate, candidateIndex) => {
      if (candidateIndex <= index || candidate.trim() === '' || candidate.trimStart().startsWith('#')) {
        return false;
      }
      return /^\s*/.exec(candidate)[0].length <= indentation;
    });
    return [{
      value: match[1],
      source: lines.slice(index + 1, next === -1 ? undefined : next).join('\n'),
    }];
  });
}

function exactTopLevelBlock(source, name) {
  const entries = directPropertyEntries(source, 0, name);
  if (entries.length !== 1 || entries[0].value !== '') {
    throw new Error(`workflow ${name} 블록이 정확히 하나여야 합니다.`);
  }
  const body = entries[0].source.trimEnd();
  return body === '' ? `${name}:` : `${name}:\n${body}`;
}

function hasDefaultsRunShell(source, defaultsIndentation) {
  return directPropertyEntries(source, defaultsIndentation, 'defaults').some((defaults) => {
    if (/\bshell\s*:/.test(defaults.value)) return true;
    return directPropertyEntries(defaults.source, defaultsIndentation + 2, 'run').some((run) =>
      /\bshell\s*:/.test(run.value) ||
      directPropertyEntries(run.source, defaultsIndentation + 4, 'shell').length > 0,
    );
  });
}

function requireOnlyPlainDirectProperties(source, indentation, name, allowedProperties) {
  const allowed = new Set(allowedProperties);
  for (const line of source.split('\n')) {
    if (line.trim() === '' || line.trimStart().startsWith('#')) continue;
    const actualIndentation = /^\s*/.exec(line)[0].length;
    if (actualIndentation !== indentation) continue;
    const directProperty = /^([A-Za-z_][A-Za-z0-9_-]*)\s*:/.exec(line.slice(indentation));
    if (directProperty === null) {
      throw new Error(`${name}의 직접 속성은 plain YAML key만 허용합니다.`);
    }
    if (!allowed.has(directProperty[1])) {
      throw new Error(`${name}에 ${directProperty[1]} 속성을 둘 수 없습니다.`);
    }
  }
}

function requireNoDirectProperties(
  source,
  directPropertyIndentation,
  name,
  properties = ['if', 'continue-on-error'],
) {
  const forbidden = new RegExp(
    `^ {${directPropertyIndentation}}(${properties.join('|')})\\s*:`,
    'm',
  ).exec(source);
  if (forbidden !== null) {
    throw new Error(`${name}에 ${forbidden[1]} 조건을 둘 수 없습니다.`);
  }
}

function foldedStepRun(step, name) {
  const lines = step.source.split('\n');
  const directPropertyIndentation = step.indentation + 2;
  const runPattern = new RegExp(`^ {${directPropertyIndentation}}run\\s*:\\s*(.*?)\\s*$`);
  const runLines = lines.flatMap((line, index) => {
    const match = runPattern.exec(line);
    return match === null ? [] : [{ index, value: match[1] }];
  });
  if (runLines.length !== 1 || runLines[0].value !== '>-') {
    throw new Error(`${name} 단계는 단일 folded run 명령이어야 합니다.`);
  }
  const commandIndentation = directPropertyIndentation + 2;
  const commandLines = lines
    .slice(runLines[0].index + 1)
    .filter((line) => line.trim() !== '')
    .map((line) => {
      if (!new RegExp(`^ {${commandIndentation}}\\S`).test(line)) {
        throw new Error(`${name} 단계의 run 명령 들여쓰기가 올바르지 않습니다.`);
      }
      return line.slice(commandIndentation).trim();
    });
  return commandLines.join(' ');
}

function singleLineStepRun(step, name) {
  const lines = step.source.split('\n');
  const directPropertyIndentation = step.indentation + 2;
  const runPattern = new RegExp(`^ {${directPropertyIndentation}}run\\s*:\\s*(.*?)\\s*$`);
  const runLines = lines.flatMap((line, index) => {
    const match = runPattern.exec(line);
    return match === null ? [] : [{ index, value: match[1] }];
  });
  if (runLines.length !== 1 || runLines[0].value === '') {
    throw new Error(`${name} 단계는 단일 inline run 명령이어야 합니다.`);
  }
  const continuation = lines
    .slice(runLines[0].index + 1)
    .some((line) => line.trim() !== '' && !line.trimStart().startsWith('#'));
  if (continuation) {
    throw new Error(`${name} 단계의 run 명령 뒤에 추가 명령을 둘 수 없습니다.`);
  }
  return runLines[0].value;
}

function verifyFailClosedPolicy(workflowSource, smokeSource) {
  const job = namedJobBlock(workflowSource, 'contract-smoke');
  requireNoDirectProperties(job, 4, 'contract-smoke job');
  if (hasDefaultsRunShell(workflowSource, 0)) {
    throw new Error('workflow defaults.run.shell은 사용할 수 없습니다.');
  }
  if (hasDefaultsRunShell(job, 4)) {
    throw new Error('contract-smoke job defaults.run.shell은 사용할 수 없습니다.');
  }
  requireOnlyPlainDirectProperties(
    workflowSource,
    0,
    'workflow',
    ['name', 'on', 'permissions', 'concurrency', 'jobs'],
  );
  requireOnlyPlainDirectProperties(
    job,
    4,
    'contract-smoke job',
    ['name', 'runs-on', 'timeout-minutes', 'env', 'steps'],
  );
  if (exactTopLevelBlock(workflowSource, 'on') !== EXPECTED_TRIGGER_BLOCK) {
    throw new Error('workflow on 블록은 승인된 PR/push/schedule/manual trigger로 고정해야 합니다.');
  }
  if (exactTopLevelBlock(workflowSource, 'permissions') !== EXPECTED_PERMISSIONS_BLOCK) {
    throw new Error('workflow permissions는 contents: read로 고정해야 합니다.');
  }
  if (
    JSON.stringify(directJobStepNames(job)) !== JSON.stringify(EXPECTED_JOB_STEP_NAMES)
  ) {
    throw new Error('contract-smoke job 단계의 이름, 순서, 개수는 canonical 목록으로 고정해야 합니다.');
  }
  const envBlocks = [...job.matchAll(/^    env:\s*$/gm)];
  if (envBlocks.length !== 1) {
    throw new Error('contract-smoke job env가 정확히 하나여야 합니다.');
  }
  const envTail = job.slice(envBlocks[0].index + envBlocks[0][0].length);
  const envEnd = /^    \S/m.exec(envTail)?.index ?? envTail.length;
  const jobEnv = envTail.slice(0, envEnd);
  const jobEnvLines = jobEnv
    .split('\n')
    .filter((line) => line.trim() !== '' && !line.trimStart().startsWith('#'));
  if (
    jobEnvLines.length !== 1 ||
    jobEnvLines[0] !== "      RUN_API_CONTRACT_SMOKE: 'true'"
  ) {
    throw new Error("contract-smoke job env는 RUN_API_CONTRACT_SMOKE='true' 하나로 고정해야 합니다.");
  }

  const dockerStep = namedStepBlock(job, 'Require Docker runtime');
  const smokeStep = namedStepBlock(job, 'Run Retrofit wire contract smoke');
  const verifierStep = namedStepBlock(job, 'Verify Retrofit wire contract result');
  requireNoDirectProperties(
    dockerStep.source,
    dockerStep.indentation + 2,
    'Require Docker runtime 단계',
    ['if', 'continue-on-error', 'shell'],
  );
  requireNoDirectProperties(
    smokeStep.source,
    smokeStep.indentation + 2,
    'Run Retrofit wire contract smoke 단계',
    ['if', 'continue-on-error', 'shell'],
  );
  requireNoDirectProperties(
    verifierStep.source,
    verifierStep.indentation + 2,
    'Verify Retrofit wire contract result 단계',
    ['if', 'continue-on-error', 'shell'],
  );
  requireOnlyPlainDirectProperties(
    dockerStep.source,
    dockerStep.indentation + 2,
    'Require Docker runtime 단계',
    ['run'],
  );
  requireOnlyPlainDirectProperties(
    smokeStep.source,
    smokeStep.indentation + 2,
    'Run Retrofit wire contract smoke 단계',
    ['run'],
  );
  requireOnlyPlainDirectProperties(
    verifierStep.source,
    verifierStep.indentation + 2,
    'Verify Retrofit wire contract result 단계',
    ['run'],
  );

  const dockerCommand = singleLineStepRun(dockerStep, 'Require Docker runtime');
  if (dockerCommand !== 'docker info') {
    throw new Error('Docker runtime 단계는 docker info를 실행해야 합니다.');
  }
  const smokeCommand = foldedStepRun(smokeStep, 'Run Retrofit wire contract smoke');
  if (
    smokeCommand !==
    "./gradlew :core:network:testDebugUnitTest --tests '*ApiWireContractSmokeTest' --stacktrace --init-script .github/ci-expected-failures.init.gradle"
  ) {
    throw new Error('API wire contract smoke 명령은 정확한 Gradle 테스트 선택자로 고정해야 합니다.');
  }
  const verifierCommand = foldedStepRun(verifierStep, 'Verify Retrofit wire contract result');
  if (
    verifierCommand !==
    'node .github/scripts/verify-api-contract-smoke-result.mjs core/network/build/test-results/testDebugUnitTest'
  ) {
    throw new Error('API wire contract XML 검증 명령은 성공 우회 없이 정확히 고정해야 합니다.');
  }
  if (job.indexOf(smokeStep.source) >= job.indexOf(verifierStep.source)) {
    throw new Error('XML 결과 검증은 Gradle smoke 테스트 뒤에 실행해야 합니다.');
  }

  if (!/check\(DockerClientFactory\.instance\(\)\.isDockerAvailable\)/.test(smokeSource)) {
    throw new Error('전용 workflow의 Docker 부재는 check로 실패해야 합니다.');
  }
  if ((smokeSource.match(/\bassumeTrue\s*\(/g) ?? []).length !== 1) {
    throw new Error('assumeTrue는 로컬 opt-in 환경 플래그에만 한 번 사용해야 합니다.');
  }
  const exactOptInAssumption =
    /assumeTrue\(\s*"(?:[^"\\]|\\.)*",\s*System\.getenv\(ENABLE_ENV\) == "true",\s*\)/g;
  if ((smokeSource.match(exactOptInAssumption) ?? []).length !== 1) {
    throw new Error('assumeTrue 조건은 RUN_API_CONTRACT_SMOKE opt-in 환경 플래그만 허용합니다.');
  }
}

test('contract smoke is explicit, Docker-backed, secretless, and bounded', () => {
  verifyFailClosedPolicy(workflow, smoke);
  assert.match(workflow, /^name: API Wire Contract Smoke$/m);
  assert.match(workflow, /^  pull_request:\n    paths:/m);
  assert.match(workflow, /^  schedule:/m);
  assert.match(workflow, /^  workflow_dispatch:/m);
  assert.match(workflow, /^permissions:\n  contents: read$/m);
  assert.match(workflow, /timeout-minutes: 15/);
  assert.match(workflow, /RUN_API_CONTRACT_SMOKE: 'true'/);
  assert.match(workflow, /run: docker info/);
  assert.match(workflow, /--tests '\*ApiWireContractSmokeTest'/);
  assert.match(workflow, /node \.github\/scripts\/verify-api-contract-smoke-result\.mjs/);
  assert.match(workflow, /core\/network\/build\/test-results\/testDebugUnitTest/);
  assert.ok(
    workflow.indexOf('Run Retrofit wire contract smoke') <
      workflow.indexOf('Verify Retrofit wire contract result'),
    'XML result verification must run after the Gradle smoke test',
  );
  assert.doesNotMatch(workflow, /secrets\./);
  for (const affectedPath of [
    'core/network/**',
    'core/common/**',
    'core/domain/**',
    'core/model/**',
    'build-logic/**',
    'settings.gradle.kts',
    'gradle/**',
    '.github/scripts/render-android-test-results.mjs',
    '.github/scripts/verify-api-contract-smoke-result.mjs',
  ]) {
    assert.ok(workflow.includes(`'${affectedPath}'`), `missing API reverse-dependency path ${affectedPath}`);
  }
});

test('workflow policy rejects conditional verifier and skip-producing mutations', () => {
  const conditionalJob = workflow.replace(
    '  contract-smoke:\n',
    '  contract-smoke:\n    if: false\n',
  );
  const toleratedJob = workflow.replace(
    '  contract-smoke:\n',
    '  contract-smoke:\n    continue-on-error: true\n',
  );
  const conditionalVerifier = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    '      - name: Verify Retrofit wire contract result\n        if: false\n',
  );
  const toleratedVerifier = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    '      - name: Verify Retrofit wire contract result\n        continue-on-error: true\n',
  );
  const customDockerShell = workflow.replace(
    '      - name: Require Docker runtime\n',
    '      - name: Require Docker runtime\n        shell: bash -c \'true\' {0}\n',
  );
  const customSmokeShell = workflow.replace(
    '      - name: Run Retrofit wire contract smoke\n',
    '      - name: Run Retrofit wire contract smoke\n        shell: bash -c \'true\' {0}\n',
  );
  const customVerifierShell = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    '      - name: Verify Retrofit wire contract result\n        shell: bash -c \'true\' {0}\n',
  );
  const customJobShell = workflow.replace(
    '  contract-smoke:\n',
    '  contract-smoke:\n    defaults:\n      run:\n        shell: bash {0} || true\n',
  );
  const customWorkflowShell = workflow.replace(
    'jobs:\n',
    'defaults:\n  run:\n    shell: bash {0} || true\n\njobs:\n',
  );
  const quotedVerifierIf = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    "      - name: Verify Retrofit wire contract result\n        'if': false\n",
  );
  const escapedVerifierTolerance = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    '      - name: Verify Retrofit wire contract result\n        "\\u0063ontinue-on-error": true\n',
  );
  const explicitVerifierTolerance = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    '      - name: Verify Retrofit wire contract result\n        ? continue-on-error\n        : true\n',
  );
  const bypassedVerifier = workflow.replace(
    '          core/network/build/test-results/testDebugUnitTest\n',
    '          core/network/build/test-results/testDebugUnitTest || true\n',
  );
  const forgedSmokeResult = workflow.replace(
    '          --stacktrace\n',
    "          --stacktrace; mkdir -p core/network/build/test-results/testDebugUnitTest; printf '<testsuite tests=\\\"1\\\" failures=\\\"0\\\" errors=\\\"0\\\" skipped=\\\"0\\\"><testcase classname=\\\"com.careercompass.core.network.service.ApiWireContractSmokeTest\\\" name=\\\"fake\\\"/></testsuite>' > core/network/build/test-results/testDebugUnitTest/TEST-fake.xml\n",
  );
  const extendedJobEnvironment = workflow.replace(
    "      RUN_API_CONTRACT_SMOKE: 'true'\n",
    "      RUN_API_CONTRACT_SMOKE: 'true'\n      JAVA_TOOL_OPTIONS: '-Dtest.fake=true'\n",
  );
  const disabledPullRequestTrigger = workflow.replace(
    '  pull_request:\n    paths:\n',
    "  pull_request:\n    branches-ignore: ['**']\n    paths:\n",
  );
  const widenedPermissions = workflow.replace(
    'permissions:\n  contents: read\n',
    'permissions:\n  contents: read\n  actions: write\n',
  );
  const forgedResultStep = workflow.replace(
    '      - name: Verify Retrofit wire contract result\n',
    "      - name: Normalize smoke report\n        run: printf 'fake pass' > core/network/build/test-results/testDebugUnitTest/TEST-fake.xml\n\n      - name: Verify Retrofit wire contract result\n",
  );
  const disabledEnvironment = workflow.replace(
    "      RUN_API_CONTRACT_SMOKE: 'true'",
    "      RUN_API_CONTRACT_SMOKE: 'false'",
  );
  const dockerAssumption = smoke.replace(
    'check(DockerClientFactory.instance().isDockerAvailable)',
    'assumeTrue(DockerClientFactory.instance().isDockerAvailable)',
  );
  const dockerInOptInAssumption = smoke.replace(
    'System.getenv(ENABLE_ENV) == "true",',
    'System.getenv(ENABLE_ENV) == "true" &&\n                DockerClientFactory.instance().isDockerAvailable,',
  );

  assert.throws(
    () => verifyFailClosedPolicy(conditionalJob, smoke),
    /contract-smoke job에 if 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(toleratedJob, smoke),
    /contract-smoke job에 continue-on-error 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(conditionalVerifier, smoke),
    /Verify Retrofit wire contract result 단계에 if 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(toleratedVerifier, smoke),
    /Verify Retrofit wire contract result 단계에 continue-on-error 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(customDockerShell, smoke),
    /Require Docker runtime 단계에 shell 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(customSmokeShell, smoke),
    /Run Retrofit wire contract smoke 단계에 shell 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(customVerifierShell, smoke),
    /Verify Retrofit wire contract result 단계에 shell 조건/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(customJobShell, smoke),
    /contract-smoke job defaults\.run\.shell/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(customWorkflowShell, smoke),
    /workflow defaults\.run\.shell/,
  );
  for (const nonCanonicalVerifier of [
    quotedVerifierIf,
    escapedVerifierTolerance,
    explicitVerifierTolerance,
  ]) {
    assert.throws(
      () => verifyFailClosedPolicy(nonCanonicalVerifier, smoke),
      /직접 속성은 plain YAML key만 허용/,
    );
  }
  assert.throws(
    () => verifyFailClosedPolicy(bypassedVerifier, smoke),
    /XML 검증 명령은 성공 우회 없이 정확히 고정/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(forgedSmokeResult, smoke),
    /smoke 명령은 정확한 Gradle 테스트 선택자로 고정/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(extendedJobEnvironment, smoke),
    /job env는 RUN_API_CONTRACT_SMOKE='true' 하나로 고정/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(disabledPullRequestTrigger, smoke),
    /workflow on 블록은 승인된/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(widenedPermissions, smoke),
    /workflow permissions는 contents: read/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(forgedResultStep, smoke),
    /job 단계의 이름, 순서, 개수는 canonical 목록/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(disabledEnvironment, smoke),
    /job env는 RUN_API_CONTRACT_SMOKE='true' 하나로 고정/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(workflow, dockerAssumption),
    /Docker 부재는 check로 실패/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(disabledEnvironment, dockerAssumption),
    /job env는 RUN_API_CONTRACT_SMOKE='true' 하나로 고정/,
  );
  assert.throws(
    () => verifyFailClosedPolicy(workflow, dockerInOptInAssumption),
    /assumeTrue 조건은 RUN_API_CONTRACT_SMOKE opt-in 환경 플래그만 허용/,
  );
});

test('wire smoke uses Testcontainers with a pinned MockServer REST API image', () => {
  assert.match(build, /testImplementation\(libs\.testcontainers\.mockserver\)/);
  assert.doesNotMatch(build, /mockserver\.client/);
  assert.match(smoke, /MOCKSERVER_VERSION = "7\.6\.0"/);
  assert.match(smoke, /DockerClientFactory\.instance\(\)\.isDockerAvailable/);
  assert.match(
    smoke,
    /check\(DockerClientFactory\.instance\(\)\.isDockerAvailable\)/,
  );
  assert.equal(smoke.match(/assumeTrue\(/g)?.length, 1);
  assert.doesNotMatch(smoke, /assumeTrue\("Docker runtime is required"/);
  assert.match(smoke, /put\("matchType", "STRICT"\)/);
  assert.match(smoke, /\/api\/v1\/auth\/social\/kakao/);
  assert.match(smoke, /Authorization", "Bearer contract-token"/);
});

test('result verifier requires at least one executed smoke test and rejects skips', () => {
  assert.match(verifier, /TARGET_CLASS = "com\.careercompass\.core\.network\.service\.ApiWireContractSmokeTest"/);
  assert.match(verifier, /parseAndroidTestXml\(xml, \{ file, validateSuiteCounters: true \}\)/);
  assert.doesNotMatch(verifier, /matchAll\(\/<testcase/);

  const result = ({ body = '', failures = 0, skipped = 0 } = {}) => ({
    file: 'TEST-ApiWireContractSmokeTest.xml',
    xml: `<testsuite tests="1" failures="${failures}" errors="0" skipped="${skipped}"><testcase classname="com.careercompass.core.network.service.ApiWireContractSmokeTest" name="contract">${body}</testcase></testsuite>`,
  });

  assert.deepEqual(verifyApiContractSmokeDocuments([result()]), {
    executed: 1,
    skipped: 0,
  });
  assert.throws(
    () => verifyApiContractSmokeDocuments([]),
    /최소 1건/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([result({ body: '<skipped/>', skipped: 1 })]),
    /skipped 결과/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([result({ body: '<failure/>', failures: 1 })]),
    /failure\/error 결과/,
  );
});

test('result verifier rejects XML structure tricks and inconsistent suite counters', () => {
  const targetClass = 'com.careercompass.core.network.service.ApiWireContractSmokeTest';
  const document = (xml) => ({ file: 'TEST-ApiWireContractSmokeTest.xml', xml });

  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="0" failures="0" errors="0" skipped="0"><system-out><![CDATA[<testcase classname="${targetClass}" name="fake"/>]]></system-out></testsuite>`),
    ]),
    /최소 1건/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="0" errors="0" skipped="0"><system-out><testcase classname="${targetClass}" name="fake"/></system-out></testsuite>`),
    ]),
    /<testsuite>의 직접 자식/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="0" errors="0" skipped="0"><system-out><testsuite tests="1" failures="0" errors="0" skipped="0"><testcase classname="${targetClass}" name="fake"/></testsuite></system-out></testsuite>`),
    ]),
    /<testsuite>는 XML 루트 또는 <testsuites>의 직접 자식/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="0" errors="0" skipped="0"><testcase classname="${targetClass}" name="broken"></testsuite>`),
    ]),
    /XML 닫는 태그/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="1" errors="0" skipped="0"><testcase classname="${targetClass}" name="passed"/></testsuite>`),
    ]),
    /failures 카운터 불일치/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="0" errors="0" skipped="1"><testcase classname="${targetClass}" name="passed"/></testsuite>`),
    ]),
    /skipped 카운터 불일치/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<report><testsuite tests="1" failures="0" errors="0" skipped="0"><testcase classname="${targetClass}" name="fake"/></testsuite></report>`),
    ]),
    /XML 루트는 <testsuite> 또는 <testsuites>/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="0" errors="0" skipped="0"><testcase classname="${targetClass}" name="fake"><wrapper><failure/></wrapper></testcase></testsuite>`),
    ]),
    /결과는 <testcase>의 직접 자식/,
  );
  assert.throws(
    () => verifyApiContractSmokeDocuments([
      document(`<testsuite tests="1" failures="1" errors="0" skipped="0"><testcase classname="${targetClass}" name="fake"/><failure/></testsuite>`),
    ]),
    /결과는 <testcase>의 직접 자식/,
  );
  const emptySuite = '<testsuite tests="0" failures="0" errors="0" skipped="0"/>';
  for (const xml of [
    `junk${emptySuite}`,
    `${emptySuite}junk`,
    `<![CDATA[junk]]>${emptySuite}`,
  ]) {
    assert.throws(
      () => verifyApiContractSmokeDocuments([document(xml)]),
      /XML 루트 요소 앞뒤에는 공백 외 텍스트나 CDATA/,
    );
  }
  assert.deepEqual(
    verifyApiContractSmokeDocuments([
      document(`<?xml version="1.0" encoding="UTF-8"?><testsuite name="${targetClass}" tests="1" failures="0" errors="0" skipped="0" time="0.1"><properties><property name="java.runtime.version" value="21"/></properties><testcase classname="${targetClass}" name="contract" time="0.1"/><system-out><![CDATA[normal Gradle output]]></system-out><system-err><![CDATA[]]></system-err></testsuite>`),
    ]),
    { executed: 1, skipped: 0 },
  );
});

test('REST control plane resets state and verifies exactly one recorded request', () => {
  assert.match(smoke, /controlPut\("\/mockserver\/reset"\)/);
  assert.match(smoke, /controlPut\("\/mockserver\/expectation"/);
  assert.match(smoke, /\/mockserver\/retrieve\?type=REQUESTS/);
  assert.match(smoke, /assertEquals\("\$method \$path must cross the socket exactly once", 1, recorded\.size\)/);
});
