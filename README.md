# CareerCompass-FE

건국대학교 졸업 프로젝트 **CareerCompass** (팀 CamBridge) 의 Android 클라이언트.

개인 맞춤 장학금·공모전·채용공고를 모아 적합도를 분석하고, 경험 카드를 바탕으로 자기소개서 초안을 만들어 주는 앱이다.

기획·디자인 산출물(Figma 13페이지, 기능 스펙, 유스케이스, 발표자료)은 코드 저장소에 두지 않는다. 별도 문서 폴더에서 관리한다.

## 현재 상태

저장소 운영 하네스와 CI/CD 파이프라인만 올라가 있고 **Android 프로젝트는 아직 없다.** Gradle 모듈이 생기기 전까지 대부분의 워크플로는 실패한다 — 정상이다.

```bash
node --test .github/scripts/*.test.mjs
```

현재 409개 중 398개 통과. 남은 11개는 전부 `settings.gradle.kts`·`app/`·`core/network/`·`gradle/wrapper/`·`build-logic/`·`baselineprofile/` 부재로 인한 `ENOENT` 이며, 프로젝트 골격이 들어오면 함께 풀린다.

## 분담

FE 2인. 경계는 **모듈 소유권**이고, 단일 정본은 `.github/scripts/reconcile-issue-metadata.mjs` 의 `ASSIGNEE_BY_MODULE` 이다. 이슈를 등록하면 `Issue Metadata Guard` 가 `area:*` 라벨과 담당자를 자동으로 맞춘다.

| 모듈 | 범위 | 담당 |
| --- | --- | --- |
| `core` | 디자인 시스템·네트워크·인증 | 정일혁 |
| `careercompass` | 앱 셸·내비게이션 | 정일혁 |
| `onboarding` | 스플래시·로그인·지문 로그인·온보딩 4단계 | 정일혁 |
| `feed` | 공고 피드·필터·공고 상세(채용/공모전)·원문 | 정일혁 |
| `platform` | CI·빌드·릴리스·저장소 운영 | 정일혁 |
| `editor` | AI 자소서 초안 생성·에디터·재생성·저장/이력 | 이준혁 |
| `profile` | 마이·프로필 편집·경험 카드 5종·과거 자소서 | 이준혁 |
| `foryou` | For You·커리어 로드맵·강점 Export | 이준혁 |
| `notification` | 알림·알림 설정 | 이준혁 |

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

- [`AGENTS.md`](AGENTS.md) — 이슈 등록·착수 경계, 머지 정책, 워크트리 위치
- [`docs/convention/`](docs/convention) — presentation 패키지 구조, Composable 콜백 기본값, 리소스 네이밍
- [`docs/testing/screenshot.md`](docs/testing/screenshot.md) — Compose Preview 스크린샷 테스트
- [`docs/release/`](docs/release) — 배포·Firebase WIF
- [`docs/qa/`](docs/qa) — QA 기기 기준, 증적 스키마
