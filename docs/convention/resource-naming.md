# 리소스 네이밍 규칙

안드로이드 리소스(`<string>` · `drawable` 등)는 **모듈 하나에 프리픽스 하나**를 쓴다. 문서로만 합의하지 않고 `android.resourcePrefix` 로 강제한다.

## 왜 모듈별인가

리소스는 이름이 같으면 R 병합 시 앱 모듈 값이 라이브러리 값을 덮는다 — 모듈이 달라도 네임스페이스가 갈리지 않는다. 이름의 첫 토큰이 **모듈 경계를 표시하지 못하면** 세 가지가 조용히 일어난다.

1. **다른 모듈 몫이 남의 모듈에 눌러앉는다.** `feed` 화면에서 쓰는 문자열을 `editor` 모듈에 만들어도 아무도 막지 않는다. 프리픽스가 걸려 있으면 `feed_*` 를 `editor` 에 만드는 순간 lint 가 막는다.
2. **모듈 간 같은 이름이 값까지 같은 중복으로 자란다.** `login_kakao_failed` 같은 이름은 `onboarding` 과 `profile` 양쪽에서 자연스럽게 나오고, 한쪽이 죽은 정의로 남아도 드러나지 않는다.
3. **화면별 프리픽스는 한 모듈 안에서만 일관돼 보인다.** `signup_*` · `withdraw_*` 처럼 화면 단위로 나누면 첫 토큰이 여러 모듈에 걸쳐 경계를 표시하지 못한다.

## 프리픽스 표

리소스를 가진 모듈만 대상이다(`*/data` · `*/domain` 은 리소스가 없다).

| 모듈 | 프리픽스 |
|---|---|
| `core/ui` | `core_ui_` |
| `core/common` | `core_common_` |
| `feature/onboarding/presentation` | `onboarding_` |
| `feature/feed/presentation` | `feed_` |
| `feature/editor/presentation` | `editor_` |
| `feature/profile/presentation` | `profile_` |
| `feature/foryou/presentation` | `foryou_` |
| `feature/notification/presentation` | `notification_` |

- `core/*` 는 `core_<모듈>_`, `feature/*` 는 `<기능>_` 이다. core 는 모듈이 여럿이고 이름이 짧아(`ui` · `common`) 한 토큰으로는 충돌하기 쉽다.
- **`app` 모듈은 대상에서 제외한다.** 이름을 바꿀 수 없는 것들뿐이다 — `app_name` 은 매니페스트 `android:label` 이 참조하는 관례 이름, `fcm_*` 는 매니페스트 메타데이터가 `@string/…` 으로 참조, `ic_launcher_background`/`_foreground` 는 adaptive icon 규약 이름이다. 앱 모듈은 R 병합의 최종 목적지라 프리픽스의 실익도 없다.

## 강제 수단

모듈 `build.gradle.kts` 의 `android` 블록에 한 줄이면 된다.

```kotlin
android {
    namespace = "com.cambridge.feature.feed.presentation"
    resourcePrefix = "feed_"
}
```

**위반은 경고가 아니라 오류이고 빌드를 실패시킨다.**

```
feature/feed/presentation/src/main/res/values/strings.xml:3: Error: Resource named 'posting_detail_title'
does not start with the project's resource prefix 'feed_' … [ResourceName]
```

모든 모듈이 리소스가 비어 있는 상태에서 프리픽스를 걸고 시작했으므로, 이 저장소에는 소급 리네임이 필요한 모듈이 없다. 새 모듈을 만들 때 `resourcePrefix` 를 같은 자리에 함께 적으면 그 상태가 유지된다.

## 신규 리소스

프리픽스가 걸린 모듈에서는 lint 가 막으므로 따로 외울 것이 없다. 위반하면 `lintDebug` 가 실패한다.
