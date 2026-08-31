# CareerCompass-FE

건국대학교 졸업 프로젝트 **CareerCompass** (팀 CamBridge) 의 Android 클라이언트.

개인 맞춤 장학금·공모전·채용공고를 모아 적합도를 분석하고, 경험 카드를 바탕으로 자기소개서 초안을 만들어 주는 앱이다.

기획·디자인 산출물(Figma 13페이지, 기능 스펙, 유스케이스, 발표자료)은 코드 저장소에 두지 않는다. 별도 문서 폴더에서 관리한다.

## 현재 상태

저장소 운영 하네스, GitHub Actions 파이프라인과 **28개 Android 멀티모듈 골격** 위에 공통 UI와 첫 presentation 화면들이 구현돼 있다. 다만 앱의 `AppNavigation` 은 아직 `CareerCompass` 자리표시자만 보여 주며, 아래 온보딩·피드 화면은 실제 내비게이션이나 데이터 흐름에 연결되기 전이다.

```bash
./gradlew assembleDebug          # debug APK 빌드
./gradlew test testDebugUnitTest # 단위 테스트 + Konsist 아키텍처 테스트
./gradlew ktlintCheck lint       # 정적 검사
node --test .github/scripts/*.test.mjs   # 저장소 정책 테스트
```

Android 명령 전에는 `local.properties` 의 `sdk.dir` 로 SDK 위치를 제공한다. `app/google-services.json` 은 커밋하지 않는다 — CI 는 `.github/actions/setup-ci-config` 가 secret 없이 스텁을 만들고, 로컬에서 같은 검증용 설정이 필요하면 `node .github/scripts/create-ci-config.mjs --workspace . --github-env /dev/null` 로 만든다.

### 구현 범위

| 범위 | 현재 구현 | 아직 구현하지 않은 경계 |
| --- | --- | --- |
| [`:core:ui`](core/ui) | 색상·타이포그래피·간격·모양 테마와 버튼, 텍스트 필드, 배지, 태그, 적합도 칩. 단위 테스트와 Compose Preview screenshot baseline 포함 | 기능별 상태·데이터 연결 |
| [`:feature:onboarding:presentation`](feature/onboarding/presentation) | 상태·이벤트를 외부에서 받는 온보딩 Step 1 폼. 입력·오류·비활성 상태의 단위/Compose 테스트와 screenshot baseline 포함 | `data`·`domain`, ViewModel, 저장·API 연동, Step 2~4 및 앱 내비게이션 |
| [`:feature:feed:presentation`](feature/feed/presentation) | 상태·이벤트를 외부에서 받는 메인 피드. 검색·필터·정렬·북마크 intent와 loading/empty/list 상태의 단위/Compose 테스트 및 screenshot baseline 포함 | `data`·`domain`, 실제 공고 조회·저장, 상세 화면 및 앱 내비게이션 |
| [`:app`](app) · [`:baselineprofile`](baselineprofile) | 최소 앱 시작 화면, 수집된 Baseline/Startup Profile, API 26·36 경계 smoke와 API 34 접근성 smoke | 온보딩·피드의 앱 셸 연결. 현재 instrumentation smoke는 `careercompass_app_start` 자리표시자만 검사한다 |
| `editor` · `profile` · `foryou` · `notification` | 멀티모듈 build/패키지 골격 | `data`·`domain`·`presentation` 전체 구현 |

### 모듈

28개. `:app` · `:baselineprofile` · `:konsist`, `core` 7개(`common` `data` `datastore` `domain` `model` `network` `ui`), 그리고 feature 6개가 각각 `data`·`domain`·`presentation` 로 나뉜다.

레이어 의존 방향은 `:konsist` 의 아키텍처 테스트가 강제한다. `build-logic` 의 `careercompass.*` 규약 플러그인이 모듈 공통 설정을 소유하므로, 모듈 build 파일에는 그 모듈만의 것을 적는다.

### 검증 기준선

예전에 빨갛다고 적혀 있던 두 정책은 필요한 산출물과 smoke가 추가돼 현재 소스 기준으로 해소됐다. 전체 정책 테스트 수는 계속 변하므로 README에 통과 개수를 고정하지 않고 위 명령과 CI 결과를 단일 정본으로 삼는다.

| 검증 | 현재 기준 |
| --- | --- |
| Baseline Profile | [`baseline-prof.txt`](app/src/main/generated/baselineProfiles/baseline-prof.txt) 와 [`startup-prof.txt`](app/src/main/generated/baselineProfiles/startup-prof.txt) 를 커밋하고, generator·주간 workflow·release AAB 패키징 계약을 정책 테스트로 검증한다 |
| API 경계 smoke | [`ApiBoundarySmokeAndroidTest`](app/src/androidTest/java/com/cambridge/careercompass_fe/ApiBoundarySmokeAndroidTest.kt) 가 API 26·36 managed-device lane에서 앱 시작 시맨틱과 지원 범위를 확인한다 |
| 접근성 smoke | [`AccessibilitySmokeAndroidTest`](app/src/androidTest/java/com/cambridge/careercompass_fe/AccessibilitySmokeAndroidTest.kt) 가 API 34에서 현재 앱 시작 화면에 Android Accessibility Test Framework 검사를 수행한다 |
| UI 회귀 | `core:ui`, 온보딩 Step 1, 피드의 단위/Compose 테스트와 커밋된 screenshot baseline을 검증한다 |

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

## 문서

에이전트 하네스(`AGENTS.md`·`CLAUDE.md`·`.claude/`·`.codex/`)는 각자 쓰는 것이 달라 저장소에 올리지 않는다 — `.gitignore` 가 막는다. 저장소가 공유하는 규약은 아래 문서와 `.github/` 의 정책 스크립트다.

- [`docs/convention/`](docs/convention) — presentation 패키지 구조, Composable 콜백 기본값, 리소스 네이밍
- [`docs/testing/screenshot.md`](docs/testing/screenshot.md) — Compose Preview 스크린샷 테스트
- [`docs/release/`](docs/release) — 배포·Firebase WIF
- [`docs/qa/`](docs/qa) — QA 기기 기준, 증적 스키마
