# 적합도 점수 경계 (F2-3 · F3-2)

적합도 점수를 **가르는 수**가 어디에 몇 개 있는지 정한다. 이슈 [#200](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/200) 의 판정이고, [#141](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/141)(4축 충족 경계)·[#100](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/100)(산출 불가 사유 분기)의 뒤를 잇는다.

## 문제 — 경계가 두 벌이었다

| 명세 | 경계 |
|---|---|
| F3-2 「점수 해석 레이블」 | 80↑ 매우 적합 / 60~79 적합 / 40~59 보통 / 39↓ 낮음 |
| F2-3 「필터 조건 · 적합도」 | 60점 이상 / **70점 이상** / 80점 이상 |

**70 은 어떤 레이블의 경계도 아니다.** 「70점 이상」으로 거르면 「적합」(60~79) 구간이 한가운데서 잘린 목록이 나오는데, 남은 공고 카드에는 여전히 「적합」이라고 적힌다. 사용자가 **고른 값**과 화면이 **부르는 이름**이 다른 눈금 위에 있어서, 「왜 어떤 적합은 빠졌는가」를 화면이 설명할 말을 갖지 못한다.

## 판정 — 경계는 F3-2 표 **하나**다

레이블 경계(80·60·40)를 정본으로 삼고, 다른 곳은 값을 베끼지 않고 그 표에서 **읽어 온다**. 정본은 `core/model` 의 `SuitabilityLabel.minScore` 다.

같은 수를 두 곳에 적어 두면 한쪽만 고쳐지는 날이 온다 — 이 문서가 고치고 있는 사고가 정확히 그 모양이었다. 그래서 프레젠테이션에 있던 사본 상수(`HIGH_SCORE_THRESHOLD` 80 · `MID_SCORE_THRESHOLD` 60)는 걷어 냈고, `SUITABILITY_AXIS_FULFILLED_THRESHOLD` 도 `SuitabilityLabel.Suitable.minScore` 를 읽는다.

| 점수 | 레이블 | 축 충족(F3-3) | 필터 선택지 | 게이지 색 | 카드 점수 칩 강조 |
|---|---|---|---|---|---|
| 80~100 | 매우 적합 | 충족 | **80점 이상 (매우 적합)** | `success` (brand) | High |
| 60~79 | 적합 | 충족 | **60점 이상 (적합)** | `info` | Mid |
| 40~59 | 보통 | 미충족 | — | `warning` | Low |
| 0~39 | 낮음 | 미충족 | — | `mutedContent` | Low |

## 필터 옵션 — 60 · 80 (70 삭제)

`FeedQuery.ALLOWED_MIN_SCORES = {60, 80}`, `FeedMinScoreFilter = {All, AtLeast60, AtLeast80}`.

- 선택지 문구에 레이블 이름을 함께 적는다 — 「60점 이상 (적합)」·「80점 이상 (매우 적합)」. 고른 값과 카드에 뜨는 이름이 같은 경계라는 사실이 화면에 드러나야 한다.
- **40 은 넣지 않았다.** 「39점 이하만 빼는」 필터라 걸어도 거의 아무것도 걸러지지 않는다. 없던 선택지를 늘리는 대신 스펙이 준 개수(전체 + 2개)를 지킨다.

### 명세 F2-3 에서 벗어난다

70 을 지우는 것은 명세 이탈이다. 대안은 「70 을 유지하고 그 의미를 정의한다」였는데, 그러려면 F3-2 에 70 경계의 레이블을 새로 만들어 레이블을 5단계로 늘려야 한다. 그 5단계는 카드·상세·필터 어디에도 쓰이지 않는 이름 하나를 위해 전 화면의 표기를 흔든다. **명세서 F2-3 표를 60·80 으로 고쳐 달라는 요청을 이슈 #200 에 남겼다.**

### 저장된 옛 조건에 70 이 남아 있는 경우

조회 조건은 `SavedStateHandle` 에만 저장된다(`FeedInputDraft`). 오프라인 스냅샷(`FeedSnapshot`)은 공고 목록만 담고 조건을 담지 않으므로 다른 복원 경로는 없다.

**이 자리가 이 변경에서 가장 다치기 쉽다.** 예전 앱이 저장한 70 을 그대로 넘기면 `FeedQuery` 의 `require` 가 **살아나는 순간** 터진다 — 되읽기 실패가 아니라 앱 시작이 깨지는 것이라 사용자에게는 「앱을 켜면 죽는다」로 보이고 빠져나갈 길이 없다.

`FeedInputDraft.restoredMinScore` 가 `ALLOWED_MIN_SCORES` 로 거르고, 통과 못 한 값은 **「전체」로 접는다.** 가까운 60 으로 접지 않는 이유 — 그건 사용자가 고르지 않은 조건을 앱이 대신 고르는 것이다. 「전체」는 조건이 없어졌다는 사실 그대로이고, 시트를 열면 「전체」가 선택돼 있어 무엇이 걸려 있는지 화면이 정직하게 말한다. `Int?.toMinScoreFilter()` 도 모르는 값에 예외를 던지지 않고 「전체」로 접는다.

`FeedInputRestoreTest` 의 「사라진 선택지 70 이 저장돼 있어도 죽지 않고 전체로 되살아난다」가 이 경로를 지킨다.

## 게이지 색 구간

공고 상세의 점수 게이지 막대는 **레이블마다 색이 다르다**. 예전에는 점수와 무관하게 언제나 `primary` 한 색이라 40점과 95점이 같은 초록으로 차올랐다.

- 색이 갈리는 지점은 전부 레이블 경계다. 게이지는 `CareerCompassScoreLevel`(3단계)이 아니라 **서버가 준 `SuitabilityLabel` 을 그대로** 받는다 — 배지 문구도 같은 값에서 나오므로 색과 글자가 어긋날 자리가 없다.
- **새 색 토큰을 만들지 않았다.** `core/ui` 의 역할 색만 쓴다(`success` · `info` · `warning` · `mutedContent`). `success` 는 `primary` 와 같은 brand500 이라 「매우 적합」은 여태 쓰던 초록 그대로다.
- 배지 톤도 같은 표를 쓴다: Brand / Info / Warning / Neutral. 배지와 막대가 나란히 있는데 한쪽만 색을 갈면 같은 점수를 두고 두 말을 하게 된다.
- **색만으로 구분되게 두지 않는다.** 같은 줄의 배지가 「매우 적합」·「적합」·「보통」·「낮음」을 글자로 적고, 접근성 문구(`feed_posting_detail_suitability_content_description`)에도 점수와 함께 실린다. 색은 훑어볼 때를 돕는 덧표시일 뿐이다.
- 큰 점수 숫자는 구간 색을 따르지 않고 `onSurface` 로 둔다. 「보통」의 주황(#F59E0B)은 흰 바탕에서 대비 2:1 근처라, 화면에서 가장 중요한 값이 가장 안 읽히게 된다.

목록 카드의 점수 칩은 글자 없이 숫자만 실어 4단계를 그릴 자리가 없다. 3단계로 묶되 **강조가 갈리는 지점은 여전히 레이블 경계**(80·60)뿐이라 두 화면이 같은 눈금 위에 있다.

## 「점수 산출 불가」 표시

F2-3 은 점수가 안 보이는 이유를 「프로필 미입력」과 「파싱 실패」로 나눈다. 화면이 실제로 가를 수 있는 것은 **둘 중 하나뿐이다.**

| 사유 | 표시 | 사용자가 할 수 있는 일 |
|---|---|---|
| 프로필 미입력 (희망 직무·관심 태그가 모두 빔) | 카드 「프로필 필요」 / 상세 「프로필을 입력하면 적합도를 확인할 수 있어요」 + 마이 탭 이동 | **있다** — 프로필을 채우면 점수가 나온다 |
| 분석 전 | 카드·상세 「분석 중」 | 없다(기다린다) |
| 파싱 실패 | **「분석 중」으로 접힌다** | 없다 |
| 프로필을 아직 못 받음(캐시 없음) | 「분석 중」 | 모르는 것을 미입력이라고 단정하지 않는다 |

### 파싱 실패를 가르지 못하는 이유 — 계약에 자리가 없다

- `GET /postings` 의 `score`·`scoreLabel` 은 그냥 nullable 이다. 「왜 없는지」를 실은 필드가 없다.
- `GET /postings/{id}` 의 `parsed`·`suitability` 도 같다 — 「아직 안 끝났다」와 「끝났는데 실패했다」가 **같은 모양(null)으로 온다.**
- `PARSING_FAILED`(422)는 있지만 그건 **요청 하나가 실패한 것**이지 공고 하나의 상태가 아니다. 지금은 `CoreDataFailure.ParsingFailed` → `FeedFailureReason.Generic` 으로 접혀 화면 전체의 「잠시 뒤 다시 시도」가 된다.

없는 필드를 클라이언트가 지어내지 않는다. 점수가 오래 안 나오는 이유를 추측해 「실패했어요」라고 적으면, 실제로는 분석 중이던 공고에 대해 앱이 거짓말을 하게 된다. **그래서 파싱 실패는 「분석 중」 안에 접어 두고, 영구 실패한 공고에는 영원히 「분석 중」이라고 적힌다는 사실을 아는 채로 둔다.**

**BE 에 요청할 계약 변경** — 공고 항목에 분석 상태를 나타내는 필드(예: `analysisStatus: pending | failed | done`)를 넣어 주면 그때 「분석할 수 없는 공고예요 · 원문 보기」로 가른다. 이슈 #200 에 남겼다.

### 서버 점수와 서버 레이블이 어긋나면

`Posting.scoreLabel`·`Suitability.label` 은 서버가 준다. 공고 상세는 **레이블을 정본으로** 쓴다 — 게이지 색·배지 톤·배지 문구가 모두 한 값에서 나오므로 한 화면 안에서는 색과 글자가 늘 같은 말을 한다. 목록 카드는 레이블 글자를 그리지 않아 점수만으로 강조를 정한다. 둘이 어긋나는 응답은 서버 버그이고, 클라이언트가 어느 한쪽을 고쳐 그리지 않는다.

## 코드 지도

| 무엇 | 어디 |
|---|---|
| 경계 정본 | `core/model/.../posting/Posting.kt` — `SuitabilityLabel` |
| 필터 선택지(도메인 불변식) | `feature/feed/domain/.../model/FeedQuery.kt` — `ALLOWED_MIN_SCORES` |
| 필터 선택지(화면) | `feature/feed/presentation/.../feedfilter/FeedFilterContract.kt` — `FeedMinScoreFilter` |
| 옛 조건 되읽기 | `feature/feed/presentation/.../feed/FeedInputDraft.kt` — `restoredMinScore` |
| 게이지 색·배지 톤 | `feature/feed/presentation/.../postingdetail/component/PostingToneMapping.kt` |
| 축 충족 경계 | `feature/feed/presentation/.../postingdetail/PostingDetailContract.kt` |
| 산출 불가 판정 | `feature/feed/presentation/.../shared/model/SuitabilityJudgement.kt` |
| 경계를 지키는 테스트 | `feature/feed/presentation/src/test/.../postingdetail/SuitabilityBoundaryTest.kt` |
