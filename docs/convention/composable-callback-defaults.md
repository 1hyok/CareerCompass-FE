# 컴포저블 콜백 디폴트 규칙

**컴포저블의 `on*` 콜백 파라미터에 no-op 디폴트(`onXxx: (...) -> Unit = {}`)를 두지 않는다.** 선택적 상호작용은 디폴트가 아니라 타입(nullable 핸들러·오버로드)으로 모델링한다.

`app` · `feature/*` 모듈의 `src/main` 에 적용하고, `konsist` 의 `NoOpCallbackDefaultKonsistTest` 가 강제한다.

## 왜 금지인가

no-op 디폴트는 **배선을 빠뜨려도 컴파일이 통과**하게 만든다. 화면은 그려지고, 버튼도 눌리는데, 아무 일도 일어나지 않는다 — 실패가 조용해서 QA 나 사용자 신고로만 드러난다.

이 실패는 세 단계로 번진다.

- 화면을 새로 붙이면서 콜백 하나를 NavGraph 에 연결하지 않는다 — 컴파일이 통과하므로 아무도 모른다.
- 한 번 전수 대조로 미배선을 걷어내도 **디폴트가 남아 있는 한 통로가 열려 있어** 같은 부류가 다시 들어온다.
- 신설 코드가 주변 패턴을 답습해 새 컴포저블에도 `= {}` 가 붙는다.

디폴트가 없으면 같은 실수가 **컴파일 에러**로 드러난다. 폴백으로 덮지 않고 값 명시·타입으로 강제한다는 이 저장소의 방향과 같다.

## 처분 기준

### 화면 컴포저블 — 디폴트 전면 제거

실호출부가 NavGraph 한 곳인 화면 컴포저블은 콜백을 전부 required 로 한다. 프리뷰 · screenshotTest · unit test 는 `{}` 를 **명시**한다.

```kotlin
// Before
fun PostingDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},      // 배선 누락이 조용히 no-op
)

// After
fun PostingDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,           // 누락 = 컴파일 에러
)

// Preview / screenshotTest
PostingDetailScreen(onBackClick = {}, onEditClick = {})
```

`{}` 명시는 디폴트와 렌더링이 동일하므로 스크린샷 baseline 이 유지된다. reference PNG 재생성이 필요한 처분이라면 그 처분이 틀린 것이다(골든 정본은 CI 컨테이너 — `docs/testing` 참고).

### 다중 호출부 리프 컴포넌트 — 건별 판정

- **전 호출부가 실값을 넘긴다** → 디폴트만 제거한다. 선택성이 실재하지 않았던 것이다.
- **상호작용이 진짜 선택적이다** → `= {}` 로 "눌러도 아무 일 없는 버튼"을 그리는 대신, 상호작용 UI 자체를 접는다:

```kotlin
// nullable 핸들러 + UI 숨김
fun EditDropdownMenu(
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)?,        // null = 편집 항목을 그리지 않는다
) {
    if (onEditClick != null) {
        CustomDropdownItem(text = "수정", onClick = onEditClick)
    }
}
```

nullable 핸들러에도 **`= null` 디폴트를 두지 않는다** — 호출부가 "핸들러 있음 / 의도적으로 없음" 을 매번 명시해야 미배선과 의도적 생략이 구분된다. `showEditItem: Boolean` 같은 플래그 + no-op 콜백 짝은 nullable 핸들러 하나로 합친다(플래그와 콜백이 어긋나는 상태를 타입에서 제거).

상호작용 조합이 많아 nullable 이 지저분해지면 오버로드 분리(상호작용 있는 시그니처 / 없는 시그니처)를 쓴다.

### 콜백 홀더 클래스도 같다

`FeedHomeActions` 처럼 콜백을 프로퍼티로 묶은 클래스의 `val onXxx: () -> Unit = {}` 도 같은 방식으로 미배선을 숨긴다. 가드가 클래스 주 생성자까지 본다.

## 강제 수단

`konsist/src/test/kotlin/com/cambridge/konsist/NoOpCallbackDefaultKonsistTest.kt` 가 app · feature `src/main` 의 `@Composable` 함수 파라미터와 클래스 주 생성자 파라미터를 스캔해, `on`+대문자 이름에 no-op 람다 기본값(`{}` · `{ }` · `{ _ -> }`)이 있으면 실패시킨다.

테스트 안 `LEGACY_NO_OP_DEFAULT_FILES` 는 관대 판정할 파일 목록이다. **이 저장소는 비어 있다** — 가드를 빈 소스에서 켰기 때문이다. 목록에 올린 파일은 위반이 있어도 없어도 통과하므로, 청소 PR 과 목록 갱신 PR 의 머지 순서가 develop 을 red 로 만드는 상황에서만 쓰고 청소가 끝나면 즉시 뺀다. 빼는 걸 잊으면 「해소된 항목은 경고로 알린다」 테스트가 CI 로그에 경고를 남긴다.
