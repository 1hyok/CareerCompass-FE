# presentation 패키지 구조 규칙

**기능(화면) 폴더가 기본 단위다. 타입(`screen/` · `viewmodel/` · `component/`)으로 먼저 쪼개지 않는다.**

`feature/*/presentation` 모듈에 적용한다. data · domain 계층은 대상이 아니다.

## 왜 기능별인가

- **변경이 같이 일어난다** — 화면 하나를 고치면 Screen · ViewModel · UiState 가 거의 항상 함께 바뀐다. 기능별이면 커밋 diff 가 한 폴더에 모인다.
- **삭제가 쉽다** — 기능이 사라지면 폴더째 지운다. 타입별에서는 네 곳을 뒤져야 하고, 그래서 참조 0건인 잔재가 남는다.
- **`internal` 이 실제로 쓰인다** — 폴더가 경계를 긋지 않으면 가시성도 좁혀지지 않는다. 타입별 구조에서는 모든 것이 사실상 공개된다.
- **타입별은 스스로도 일관되지 않는다** — `FeedFilterUiState` 를 `viewmodel/` 에 둘지 `model/` 에 둘지 매번 갈린다. 규칙이 없으니 어디를 봐야 할지 그때그때 다르다.

## 목표 구조

```
feature/<name>/presentation/src/main/kotlin/com/cambridge/feature/<name>/presentation/
  <기능>/                     # 예: postingdetail/ · draftedit/ · experiencecard/
    XxxScreen.kt
    XxxViewModel.kt
    XxxUiState.kt
    component/                # 이 기능만 쓰는 컴포넌트
  shared/                     # 2개 이상 기능이 쓰는 것만
    component/
    model/
    util/
  navigation/                 # 모듈에 하나
  reporting/                  # 모듈에 하나
```

## 규칙

### R1. 1단계는 기능 폴더다

`screen` · `viewmodel` · `component` · `model` 을 1단계에 두지 않는다. 1단계 이름은 화면 묶음의 이름(`postingdetail` · `draftedit` · `experiencecard`)이다.

기능 단위는 **함께 바뀌는 화면 묶음**이다. 자소서 초안 생성 · 에디터 · 재생성 · 저장 이력은 한 흐름이므로 `draft/` 하나다. 화면마다 폴더를 만들지 않는다.

### R2. 깊이는 2단계까지다

presentation 패키지 루트 기준으로 최대 2단계다.

```
draft/DraftEditorScreen.kt                  # 1단계 — OK
draft/component/DraftToolbar.kt             # 2단계 — OK
draft/editor/toolbar/style/Xxx.kt           # 4단계 — 금지
```

2단계에 올 수 있는 이름은 타입 폴더(`component` · `model` · `util`)뿐이다. **기능 폴더 안에 기능 폴더를 두지 않는다.** 기능 폴더가 커져 쪼개고 싶으면 1단계에 형제 폴더로 만든다(`draft/` 가 커지면 `drafthistory/` 가 아니라 `history/`).

### R3. `shared/` 는 2개 이상 기능이 쓸 때만이다

한 기능만 쓰면 그 기능 폴더로 내린다. "공용이 될 것 같아서" 는 근거가 아니다 — 두 번째 사용처가 생길 때 옮긴다.

사용처의 1단계 폴더를 센다.

```bash
M=feature/feed/presentation/src/main/kotlin/com/cambridge/feature/feed/presentation
git grep -lw 'PostingCard' -- "$M" | sed "s#$M/##" | cut -d/ -f1 | sort -u
```

출력이 2줄 이상이면 `shared/`, 1줄이면 그 기능 폴더다. 출력이 비면 참조가 없다는 뜻이므로 옮기지 말고 지운다.

### R4. `*UiState` 는 그 화면의 기능 폴더에 둔다

`*UiState` · `*UiEvent` · `*UiEffect` 는 ViewModel 과 같은 폴더다. `viewmodel/` 과 `model/` 로 갈라 두지 않는다.

여러 기능이 공유하는 UI 모델만 `shared/model/` 에 둔다. 판정 기준은 R3 과 같다. `mapper/` · `util/` 같은 나머지 타입 폴더도 마찬가지다.

### R5. `navigation/` · `reporting/` 은 모듈에 하나다

`Route` · `NavGraph` · `NavActions` 는 모듈 진입점이라 기능별로 쪼개지 않는다. 실패 리포팅(`*FailureReporting.kt`)도 모듈 단위다.

## 새 모듈

이 저장소의 presentation 모듈은 전부 빈 상태로 시작했다. 소급 이관이 필요한 모듈이 없으므로, 첫 화면을 넣을 때부터 이 구조로 만들면 유지된다.
