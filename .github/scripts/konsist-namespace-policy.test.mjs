import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import test from "node:test";

// Konsist 는 코드 구조를 «문자열» 로 서술한다. 패키지 접두사가 실제 namespace 와 어긋나도
// 컴파일이 통과하고 테스트도 초록으로 남는다 — 스캔이 0건을 훑을 뿐이기 때문이다.
// 실제로 이식 직후 두 가드가 `com\.careercompass\.` 를 찾고 있어 아무것도 잡지 못했다.
// 그 상태는 어느 검사에도 걸리지 않았으므로, 접두사 자체를 여기서 대조한다.

const konsistDirectory = new URL(
    "../../konsist/src/test/kotlin/com/careercompass/konsist/",
    import.meta.url,
);

/** 모듈 namespace 의 정본은 build 파일이다. 여기서 뽑아 konsist 리터럴과 맞춘다. */
async function namespaceRoot() {
    const build = await readFile(new URL("../../app/build.gradle.kts", import.meta.url), "utf8");
    const match = build.match(/namespace\s*=\s*"([a-z0-9_]+\.[a-z0-9_]+)\./);
    assert.ok(match, "app/build.gradle.kts 에서 namespace 를 읽지 못했다");
    return match[1];
}

async function konsistSources() {
    const names = (await readdir(konsistDirectory)).filter((name) => name.endsWith(".kt"));
    assert.ok(names.length > 0, "konsist 테스트를 하나도 찾지 못했다 — 판정이 망가졌다");
    return Promise.all(
        names.map(async (name) => [name, await readFile(new URL(name, konsistDirectory), "utf8")]),
    );
}

test("konsist 의 패키지 접두사가 실제 namespace 와 일치한다", async () => {
    const root = await namespaceRoot();
    // 정규식 안에서는 점이 이스케이프된다. 두 형태를 모두 본다.
    const foreign = new RegExp(String.raw`\bcom\\?\.(?!${root.split(".")[1]}\b)[a-z0-9_]+`, "g");

    for (const [name, source] of await konsistSources()) {
        // import 줄은 konsist 라이브러리(com.lemonappdev) 등 남의 패키지를 정당하게 쓴다.
        // 대조 대상은 이 저장소의 구조를 서술하는 본문뿐이다.
        const body = source
            .split("\n")
            .filter((line) => !/^\s*(import|package)\s/.test(line))
            .join("\n");
        const hits = [...body.matchAll(foreign)].map((m) => m[0]);
        assert.deepEqual(
            hits,
            [],
            `${name} 이 ${root} 가 아닌 패키지 접두사를 쓴다: ${hits.join(", ")}`,
        );
    }
});

test("konsist 가 참조하는 모듈 경로가 실제로 존재한다", async () => {
    const settings = await readFile(new URL("../../settings.gradle.kts", import.meta.url), "utf8");
    const modules = new Set(
        [...settings.matchAll(/include\("(:[a-z0-9:_-]+)"\)/g)]
            .map((m) => m[1].slice(1).replaceAll(":", "/")),
    );
    assert.ok(modules.size > 0, "settings.gradle.kts 에서 모듈을 읽지 못했다");

    for (const [name, source] of await konsistSources()) {
        // 문자열 리터럴로 적힌 모듈 경로만 본다 — 주석의 산문은 대상이 아니다.
        const referenced = [...source.matchAll(/"((?:core|feature|app)\/[a-z0-9/_-]+)\/"/g)]
            .map((m) => m[1]);
        for (const path of referenced) {
            assert.ok(
                modules.has(path),
                `${name} 이 존재하지 않는 모듈을 가리킨다: ${path}`,
            );
        }
    }
});
