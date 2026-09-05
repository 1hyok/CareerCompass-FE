# Navigation 3 규칙

**화면 사이의 이동은 피처가 소유하는 로컬 Navigation 3 스택이 처리하고, 앱 셸과는 최소 경계(`FeatureStackBoundary`)로만 만난다.**

애프터노트가 추진 중인 같은 이관(Afternote/Afternote-FE#1698, PR #1824)의 계약을 그대로 들여왔다(#259). 베이스는 `core/ui` 의 `com.careercompass.core.ui.navigation` 에 있다. 루트 `NavHost` 는 아직 Navigation 2 이고, 루트를 `NavDisplay` 로 바꾸고 Nav2 를 걷어내는 것은 #260 이 한다.

## 왜인가

Nav2 에서 온보딩·피드 그래프는 루트 `NavController` 하나를 앱 셸이 쥐고 `popUpTo` 옵션 조합으로 백스택을 만들었다. 결과 상태가 옵션 조합의 부산물이라 그래프를 읽어도 스택 모양이 보이지 않았고, 그래프 스코프 ViewModel 은 `getBackStackEntry<Graph>()` 로 셸이 엔트리를 꺼내 줘야 했다. Nav3 는 백스택이 그냥 리스트다. 피처가 제 스택을 갖고 모양을 직접 만들면, 그 모양을 컴포지션 없는 JVM 테스트로 못박을 수 있다.

## `core:ui` 의 계약

| 이름 | 하는 일 |
| --- | --- |
| `FeatureStackBoundary` | 로컬 스택이 셸에 돌려주는 두 가지. 바닥에서의 back(`exit`)과 바텀바 판정용 깊이(`onAtRootChanged`) |
| `FeatureNavDisplay` | 표준 entry decorator 목록과 바닥 back 처리를 한 곳에 모은 표시부. 로컬 스택은 전부 이걸 거친다 |
| `NavBackStackOps` | Nav2 `popUpTo` 자리의 스택 조작 4종: `popOrExit` · `replaceAllWith` · `popUpTo` · `pushSingleTop` |

`careercompass.android.navigation` convention 이 Nav3 runtime/UI · lifecycle ViewModel add-on · serialization 을 함께 얹는다.

## 피처 하나의 모양

```kotlin
@Serializable
public sealed interface FeedRoute : NavKey {
    @Serializable public data object Home : FeedRoute
    @Serializable public data class PostingDetail(val postingId: Long) : FeedRoute
}

internal class FeedLocalNavActions(backStack, boundary, externalActions) : FeedNavActions {
    override fun navigateToPostingDetail(postingId: Long) { backStack.add(FeedRoute.PostingDetail(postingId)) }
    override fun popBack() = backStack.popOrExit(boundary)
    override fun navigateToProfileTab() = externalActions.navigateToProfileTab()
}

@Composable
public fun FeedNavHost(boundary: FeatureStackBoundary, externalActions: FeedExternalActions, ...) {
    val backStack = rememberNavBackStack(FeedRoute.Home)
    val actions = remember(backStack, boundary, externalActions) { FeedLocalNavActions(backStack, boundary, externalActions) }
    FeatureNavDisplay(backStack = backStack, boundary = boundary, entryProvider = entryProvider { entry<FeedRoute.Home> { ... } })
}
```

- `XxxRoute : NavKey` 가 스택의 키다. `@Serializable` 이어야 `rememberNavBackStack` 이 프로세스 재생성 뒤 복원한다. `private` 이면 안 된다(아래 함정).
- `XxxNavActions` 는 화면 콜백이 요청하는 이동의 계약이고, `XxxLocalNavActions` 가 그것을 스택 조작으로 잇는다. 컴포저블이 아니라 평범한 클래스다. JVM 테스트(`XxxLocalNavActionsTest`)가 스택 모양을 그대로 잰다.
- `XxxExternalActions` 는 로컬 스택이 스스로 갈 수 없는 곳(다른 그래프·탭)과 셸만 내릴 수 있는 판정(세션 종료)만 남긴다. 앱 셸이 구현한다.
- `XxxNavHost` 가 스택과 표시부를 갖는다. 루트에는 host destination 하나만 등록한다.

## 공유 ViewModel 수명

Nav3 엔 그래프라는 중간 계층이 없다. 그래프 스코프였던 ViewModel 은 두 가지로 옮긴다.

- 여러 화면이 공유하고 피처를 벗어나면 정리돼야 하는 것(`OnboardingViewModel`)은 **host 스코프**다. `XxxNavHost` 몸통에서 `hiltViewModel()` 을 부른다. host 를 담은 루트 엔트리가 백스택에서 내려갈 때 정리되므로 이관 전과 같은 수명이다.
- 화면 하나의 것은 `entry { }` 안에서 만든다. entry 범위라 그 화면이 스택에서 빠질 때 정리되고, 위에 다른 화면이 쌓이는 동안은 살아 있다.

폼 초안용 `SavedStateHandle` 은 그대로 쓴다. entry 스코프 owner 가 저장 상태 레지스트리를 물고 있어 프로세스 재생성 성질이 유지된다.

## 라우트 인자

Nav3 entry 엔 Nav2 의 `savedStateHandle.toRoute<T>()` 자동 채움이 없다. 인자를 받는 ViewModel 은 키를 assisted 주입으로 받는다.

```kotlin
@HiltViewModel(assistedFactory = PostingDetailViewModel.Factory::class)
public class PostingDetailViewModel @AssistedInject constructor(@Assisted route: FeedRoute.PostingDetail, ...) {
    @AssistedFactory
    public interface Factory { public fun create(route: FeedRoute.PostingDetail): PostingDetailViewModel }
}

entry<FeedRoute.PostingDetail> { key ->
    PostingDetailScreen(viewModel = hiltViewModel<PostingDetailViewModel, PostingDetailViewModel.Factory>(creationCallback = { it.create(key) }))
}
```

`Screen` 의 `viewModel` 파라미터는 기본값을 잃고 호출부(host)가 주입한다. 테스트는 `SavedStateHandle(mapOf("postingId" to id))` 대신 `route = FeedRoute.PostingDetail(id)` 로 만든다.

## 셸이 로컬 스택에 부탁하는 진입

셸은 피처의 로컬 백스택에 직접 push 할 수 없다. 딥링크 상세와 온보딩 완료의 「게시판 먼저 등록하기」는 `FeedEntryRequest` 로 host 에 넘기고, host 가 스택에 반영한 뒤 1회 소비 콜백으로 비운다. host 는 피드 그래프 안에서만 그려지므로 로그인·온보딩 중에는 적용되지 않는다. 딥링크의 인증 게이트가 그대로 지켜진다.

## 바텀바

피드의 Nav2 destination 은 `FeedGraphRoute` 하나뿐이라 destination 만으로는 상세가 쌓였는지 알 수 없다. `FeatureNavDisplay` 가 `boundary.onAtRootChanged(isAtRoot)` 로 깊이를 올리고, 셸이 `AppState.shouldShowBottomBar(destination, isFeedStackAtRoot)` 로 합성한다. host 가 컴포지션에서 빠지면(탭 이탈) `true` 로 되돌린다. 안 되돌리면 다른 탭의 판정이 이 피처의 마지막 깊이에 오염된다.

## 실측으로 확인한 함정

애프터노트 #959 파일럿과 PR #1824 가 AAR 을 열어 1.1.6 API 에서 확인한 것이다. `FeatureNavDisplay` 와 `FeatureNavDisplayTest` 가 이 함정을 한 번에 닫는다.

1. `NavDisplay.onBack` 은 `() -> Unit` 이다. 문서·블로그에 도는 `{ count -> ... }` 형태는 이 버전에서 컴파일되지 않는다.
2. `entryDecorators` 를 넘기면 기본 목록을 **통째로 대체**한다. 기본값은 `rememberSaveableStateHolderNavEntryDecorator()` 하나뿐이라, ViewModel 스코프용 `rememberViewModelStoreNavEntryDecorator()` 만 넣으면 `rememberSaveable` 이 조용히 깨진다. 둘 다 넣는다.
3. `NavKey` 는 `private` 이면 안 된다. `rememberNavBackStack` 이 리플렉션으로 직렬화하는데 kotlinx.serialization 이 private 선언의 `INSTANCE` 에 접근하지 못해 저장 시점에 `IllegalAccessException` 이 난다.
4. 바닥에서의 back 은 `boundary.exit()` 로 가지 않는다. `NavDisplay` 는 `previousEntries` 가 비면 back 핸들러 자체를 끄므로, 스택 크기 1 에서 제스처·시스템 back 은 이 표시부를 지나쳐 상위(루트 `NavHost` · 액티비티)로 흘러간다. `exit()` 에 실제로 도달하는 것은 화면 안 back 버튼(`popOrExit`)뿐이다.
5. Nav3 1.1.6 의 back 은 `androidx.activity` 가 아니라 `androidx.navigationevent` 를 탄다. 테스트에서 `LocalOnBackPressedDispatcherOwner` 를 갈아 끼워도 콜백이 하나도 안 붙는다. `NavigationEventDispatcher` 를 쓴다.

## 회귀 기준의 층

| 층 | 어디서 | 보는 것 |
| --- | --- | --- |
| 스택 모양 | `XxxLocalNavActionsTest` (JVM) | Nav2 `popUpTo` 조합이 만들던 결과 스택. 교체·수렴·single top·바닥 back |
| 표시부 기전 | `core:ui` `FeatureNavDisplayTest` (Robolectric) | `rememberSaveable` 보존 · entry ViewModel 정리 시점 · 깊이 신호 · 프로세스 재생성 복원 · 바닥 back 의 흐름 |
| 루트가 보는 모양 | app 계측 `AppNavigationAndroidTest` | 시작 목적지 분기 · 탭 전환 · 로그아웃 · 세션 만료 · 딥링크 인증 게이트 |

## MVI 와의 접점

네비게이션 의도는 `Intent` 가 아니라 `Screen` 의 콜백으로 올린다(`docs/convention/mvi.md`). ViewModel 이 시점을 아는 이동(저장 성공 뒤 pop)은 `UiState` 의 nullable 신호로 두고 `Screen` 이 `ObserveSignal` 로 소비해 `XxxNavActions` 를 부른다. ViewModel 은 백스택을 모른다.
