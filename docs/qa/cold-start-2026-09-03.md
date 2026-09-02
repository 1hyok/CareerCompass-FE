# 콜드 스타트 실측 — 2026-09-03

이슈 [#74](https://github.com/Team-CamBridge/CareerCompass-FE/issues/74) 의 완료 조건인 「release 콜드 스타트 측정값과 방법」 기록이다.
2026-09-02 에 이슈에 적힌 **debug 5.3초**는 빌드 종류와 에뮬레이터 오버헤드가 대부분이었다는 것이 이 측정의 결론이다.

## 환경

| 항목 | 값 |
| --- | --- |
| 기기 | 에뮬레이터 `Pixel_7_Claude_QA`(`emulator-5554`), API 35 |
| 호스트 | macOS(Darwin 25.6.0), 14코어 / 48GB |
| 대상 | `com.cambridge.careercompass_fe/.MainActivity` |
| 시작 분기 | **세션 없음 → 로그인 화면**(서버가 자리표시자 주소라 실제 로그인 불가) |
| 측정 명령 | `adb shell am force-stop` → `adb shell am start -W` 의 `TotalTime` |
| 반복 | 매 회차 앞에 워밍업 1회(집계 제외), 이후 5회 |

## 결과

| 빌드 | 회차 | 중앙값 | 최소 | 최대 | 같은 APK 안의 폭 |
| --- | --- | --- | --- | --- | --- |
| debug — develop(변경 전) | 3라운드 × 5회 = 15 | **2,637 ms** | 692 | 3,605 | 2,913 |
| debug — 이 PR(변경 후) | 3라운드 × 5회 = 15 | **2,882 ms** | 2,544 | 3,287 | 743 |
| release — 설치 직후 | 5 | **1,239 ms** | 982 | 1,263 | 281 |
| release — `speed-profile` 강제 컴파일 후 | 5 | **1,296 ms** | 1,215 | 1,621 | 406 |

원자료(ms, 측정 순서대로):

```
debug before  R1 2255 2483 692 2230 2355 | R2 3324 3585 3322 3605 2655 | R3 2637 2598 2774 2422 3334
debug after   R1 2740 3001 2629 2881 3032 | R2 2971 3150 3287 2805 3280 | R3 3281 2563 2882 2544 2754
release       설치 직후 1263 982 1245 1038 1239
release       speed-profile 1345 1248 1215 1296 1621
```

## 읽는 법

**release 가 debug 의 절반 이하다.** 중앙값 1,239 ms 대 2,637 ms 다. 이슈에 적힌 5.3초와 견주면 4배 이상 빠르다.
즉 5.3초는 앱 시작 경로의 비용이 아니라 debug 빌드(비최적화 dex·`versionNameSuffix` 계산·디버거 대기)와
당시 에뮬레이터 상태가 만든 값이었다. **앞으로 콜드 스타트를 논할 때는 release 수치를 쓴다.**

**debug 의 전후 차이는 측정으로 가릴 수 없다.** 중앙값은 변경 후가 245 ms 느리게 나왔지만, 같은 APK 를
연달아 재는 동안의 폭이 develop 에서 2,913 ms, 이 PR 에서 743 ms 다. 라운드 간 드리프트(같은 APK 인데
R1 2,255 ms → R2 3,324 ms)가 빌드 간 차이보다 크다. 순서를 뒤집어 3라운드를 교차로 재도 부호가 일정하지
않았다(R1 후자 느림 → R2 후자 빠름 → R3 후자 느림). **이 차이는 노이즈로 읽어야 한다.**

**그리고 이 분기에서는 원래 차이가 나올 수 없다.** 이 PR 의 시작 경로 변경은 「세션이 있을 때 네트워크를
기다리지 않는다」인데, 측정한 분기는 세션이 없어 `MainViewModel.resolve()` 가 곧바로 `Login` 을 돌려준다.
변경 전에도 네트워크를 타지 않는 경로다. 서버가 자리표시자라 실제 로그인 상태를 만들 수 없어 「세션 있음」
분기는 계측 테스트의 fake 주입으로만 재현되고, 거기서 확인하는 것은 시간이 아니라 **동작**이다 —
`MainViewModelTest` 의 「캐시가 완료면 `refreshProfile` 을 기다리지 않고 Main 으로 확정한다」가 그 증거다.

**Baseline Profile 은 이 측정에서 차이를 보이지 않았다.** `speed-profile` 강제 컴파일 뒤가 오히려 57 ms
느리지만 폭 안이다. 커밋된 프로필이 이미 설치 시점에 적용되고 있어(`ProfileInstaller`) 강제 컴파일이
더할 것이 없었다고 보는 편이 자연스럽다. 프로필의 값을 제대로 재려면 프로필을 뺀 release 와 비교해야 하고,
그건 `baselineprofile` 모듈의 macrobenchmark 로 할 일이다(후속).

## 이 PR 이 실제로 줄인 것

1. **시작 경로의 네트워크 왕복** — 세션이 있고 마지막으로 알려진 온보딩 상태가 있으면 `GET /users/me` 를
   기다리지 않고 목적지를 확정한다. 스플래시 유지 조건이 그만큼 일찍 풀린다. 단위 테스트로 고정했다.
2. **`WorkManagerInitializer`** — 아무 모듈도 쓰지 않는 `androidx.work` 의존을 뺐다. App Startup 은
   의존만 있어도 초기화기를 매 콜드 스타트에 실행한다. debug APK 가 21,518,927 → 20,939,563 바이트로
   579 KB 줄었다.

## 재현

```bash
./gradlew :app:assembleDebug
adb -s emulator-5554 uninstall com.cambridge.careercompass_fe
adb -s emulator-5554 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am force-stop com.cambridge.careercompass_fe
adb -s emulator-5554 shell am start -W -n com.cambridge.careercompass_fe/.MainActivity
```

`TotalTime` 을 읽고, `force-stop` → `am start -W` 를 5회 반복해 중앙값을 쓴다. 첫 실행은 dexopt·파일
캐시가 섞이므로 버린다.

release 빌드는 서명 키와 소셜 로그인 키가 있어야 한다. **측정용으로 임시 키를 쓸 때는 커밋되지 않는
경로에만 둔다** — 이 측정에서는 워크트리 `build/`(gitignore) 의 임시 keystore 를 `local.properties` 의
`RELEASE_*` 로 가리키고, `KAKAO_NATIVE_APP_KEY`·`GOOGLE_WEB_CLIENT_ID` 는 자리표시자를 환경 변수로 넘겼다.
그렇게 만든 APK 는 소셜 로그인이 동작하지 않으므로 시작 시간 측정 외에는 쓰지 않는다.

## 한계

- 에뮬레이터 한 대의 수치다. 실기기 수치가 아니다(`docs/qa/device-baseline.md` 의 출시 전 1회 스모크 몫).
- 「세션 있음」 분기의 시간은 재지 못했다 — 실제 서버가 붙은 뒤에 다시 잰다.
- 호스트에서 Gradle 빌드가 함께 돌던 시간대라 debug 라운드의 드리프트가 컸다. 다음 측정은 빌드가 없는
  상태에서 돈다.

## 후속

- `baselineprofile` 모듈에 `StartupBenchmark`(macrobenchmark)를 추가해 프로필 유/무를 같은 실행에서 비교한다.
- 서버가 붙으면 「세션 있음 + 온보딩 완료」 분기의 release 콜드 스타트를 잰다.
