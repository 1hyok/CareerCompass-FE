# CareerCompass-FE

건국대학교 졸업 프로젝트 **CareerCompass** (팀 CamBridge) 의 Android 클라이언트.

개인 맞춤 장학금·공모전·채용공고를 모아 적합도를 분석하고, 경험 카드를 바탕으로 자기소개서 초안을 만들어 주는 앱이다.

기획·디자인 산출물(Figma 13페이지, 기능 스펙, 유스케이스, 발표자료)은 코드 저장소에 두지 않는다. 별도 문서 폴더에서 관리한다.

## 현재 상태

저장소 운영 하네스, GitHub Actions 파이프라인, **Android 멀티모듈 골격**까지 올라가 있다. 화면 구현은 아직 없다 — 각 feature 모듈은 패키지 구조만 잡혀 있는 빈 껍데기다.

```bash
./gradlew assembleDebug          # debug APK 빌드
./gradlew test testDebugUnitTest # 단위 테스트 + Konsist 아키텍처 테스트
./gradlew ktlintCheck lint       # 정적 검사
node --test .github/scripts/*.test.mjs   # 저장소 정책 테스트
```

`local.properties` 의 `sdk.dir` 만 채우면 위 넷이 모두 통과한다. `app/google-services.json` 은 커밋하지 않는다 — CI 는 `.github/actions/setup-ci-config` 가 secret 없이 스텁을 만들고, 로컬에서는 `node .github/scripts/create-ci-config.mjs --workspace . --github-env /dev/null` 로 같은 것을 만든다.

### 모듈

28개. `:app` · `:baselineprofile` · `:konsist`, `core` 7개(`common` `data` `datastore` `domain` `model` `network` `ui`), 그리고 feature 6개가 각각 `data`·`domain`·`presentation` 로 나뉜다.

레이어 의존 방향은 `:konsist` 의 아키텍처 테스트가 강제한다. `build-logic` 의 `careercompass.*` 규약 플러그인이 모듈 공통 설정을 소유하므로, 모듈 build 파일에는 그 모듈만의 것을 적는다.

### 아직 빨간 것

정책 테스트 411개 중 409개 통과. 남은 2개는 실제 작업이 있어야 풀린다.

| 실패 | 필요한 것 |
| --- | --- |
| `baseline-profile-policy` — 커밋된 프로파일 | `app/src/main/generated/baselineProfiles/baseline-prof.txt` — 화면이 생긴 뒤 Baseline Profile workflow 를 한 번 돌려 수집한다. 100줄 넘는 실제 수집본을 요구하므로 지어낼 수 없다 |
| `baselineprofile-policy` — API 경계 smoke | `app/src/androidTest/.../ApiBoundarySmokeAndroidTest.kt` — 온보딩 화면의 `onboarding_welcome_start` 시맨틱을 검증하므로 그 화면이 먼저 필요하다 |

## 분담

FE 2인. 경계는 **모듈 소유권**이고, 단일 정본은 `.github/scripts/reconcile-issue-metadata.mjs` 의 `ASSIGNEE_BY_MODULE` 이다. 이슈를 등록하면 `Issue Metadata Guard` 가 `area:*` 라벨과 담당자를 자동으로 맞춘다.

| 모듈 | 범위 | 담당 |
| --- | --- | --- |
| `core` | 디자인 시스템·네트워크·인증 | 정일혁 (`@1hyok`) |
| `careercompass` | 앱 셸·내비게이션 | 정일혁 (`@1hyok`) |
| `onboarding` | 스플래시·로그인·지문 로그인·온보딩 4단계 | 정일혁 (`@1hyok`) |
| `feed` | 공고 피드·필터·공고 상세(채용/공모전)·원문 | 정일혁 (`@1hyok`) |
| `platform` | CI·빌드·릴리스·저장소 운영 | 정일혁 (`@1hyok`) |
| `editor` | AI 자소서 초안 생성·에디터·재생성·저장/이력 | 이준혁 (`@Sadturtleman`) |
| `profile` | 마이·프로필 편집·경험 카드 5종·과거 자소서 | 이준혁 (`@Sadturtleman`) |
| `foryou` | For You·커리어 로드맵·강점 Export | 이준혁 (`@Sadturtleman`) |
| `notification` | 알림·알림 설정 | 이준혁 (`@Sadturtleman`) |

`core` 는 두 사람의 공통 선행 의존이다. 여기에 손대는 이슈는 `blocked_by` 로 순서를 걸어 `merge-order-guard` 가 머지 순서를 강제하게 한다.

## 머지 정책

**리뷰 승인 없이 작성자가 직접 머지한다.** 승인자를 기다리지 않는다.

머지의 게이트는 두 개뿐이다.

1. CI 필수 검사 통과
2. `merge-order-guard` — 이슈 `blocked_by` 의존성

코멘트와 `REQUEST_CHANGES` 는 누구나 남길 수 있지만 머지를 막지 않는다. 답하지 않은 변경요청이 있으면 `conflict-label` 이 `awaiting-author` 라벨만 붙인다.

리뷰가 게이트가 아니라는 것이 검증을 건너뛴다는 뜻은 아니다. 리뷰어가 잡아 주던 몫을 작성자가 진다 — PR 전에 변경 diff 와 영향 경계를 대조해 테스트 누락을 스스로 점검한다.

## 애프터노트에서 이식하며 뺀 것

이 저장소의 하네스·CI 는 [Afternote-FE](https://github.com/Afternote/Afternote-FE) 에서 통째로 가져왔다. 리뷰가 게이트가 아니므로 다음은 의도적으로 뺐다.

| 뺀 것 | 이유 |
| --- | --- |
| `review-debt-guard.yml` | 미처리 리뷰 요청이 있으면 새 PR 을 닫는다 — 리뷰 없이 머지하는 저장소에서는 방해만 된다 |
| `review-request-all.yml` | 위 가드와 한 세트. 요청이 쌓이기만 한다 |
| `latest-review-decision-{event,reconcile}.yml` | 리뷰 결정 상태 동기화 — 게이트가 없으면 무의미 |
| 애프터노트 QA 실측 증적 34건 | 이 프로젝트의 기록이 아니다. 스키마와 `evidence/README.md` 만 남겼다 |

`merge-order-guard` 는 리뷰가 아니라 **이슈 의존성** 게이트라 그대로 남겼다.

## 문서

에이전트 하네스(`AGENTS.md`·`CLAUDE.md`·`.claude/`·`.codex/`)는 각자 쓰는 것이 달라 저장소에 올리지 않는다 — `.gitignore` 가 막는다. 저장소가 공유하는 규약은 아래 문서와 `.github/` 의 정책 스크립트다.

- [`docs/convention/`](docs/convention) — presentation 패키지 구조, Composable 콜백 기본값, 리소스 네이밍
- [`docs/testing/screenshot.md`](docs/testing/screenshot.md) — Compose Preview 스크린샷 테스트
- [`docs/release/`](docs/release) — 배포·Firebase WIF
- [`docs/qa/`](docs/qa) — QA 기기 기준, 증적 스키마
