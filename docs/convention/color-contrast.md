# 색 대비 검증 (WCAG 2.1 AA)

`core/ui` 의 역할 토큰(`CareerCompassColors`)이 실제로 그려지는 **전경/배경 조합**을 라이트·다크 두 팔레트에서 재고, 통과하지 못한 조합의 **토큰 값을 고친 기록**이다. 판정은 사람 눈이 아니라 `CareerCompassColorsTest` 가 매 빌드마다 다시 한다 — 이 문서는 그 표의 근거와 예외를 적는다.

기준은 WCAG 2.1 AA 다.

| 대상 | 기준 | 성공 기준 |
|---|---|---|
| 본문·작은 글자 | 4.5:1 | 1.4.3 Contrast (Minimum) |
| 큰 글자(≥18.66sp Bold, ≥24sp) | 3:1 | 1.4.3 |
| 아이콘·경계·의미를 지는 도형 | 3:1 | 1.4.11 Non-text Contrast |
| 색만으로 뜻을 나르지 않기 | — | 1.4.1 Use of Color |

## 어떻게 재는가

- 대비비는 **WCAG 상대 휘도 공식을 직접 구현**한다(`CareerCompassColorsTest.relativeLuminance`). 라이브러리를 붙이지 않는다 — 공식이 짧고, 값을 어디서 얻었는지가 근거의 일부이기 때문이다.
  1. sRGB 채널을 0~1 로 정규화하고 `c ≤ 0.03928 ? c/12.92 : ((c+0.055)/1.055)^2.4` 로 선형화한다.
  2. `L = 0.2126R + 0.7152G + 0.0722B`.
  3. `(밝은 L + 0.05) / (어두운 L + 0.05)`.
- 검사 대상은 **역할 쌍을 손으로 늘어놓은 목록이 아니다.** `onX` 는 `x` 위에 놓인다는 이름 규칙을 테스트가 직접 검사하고(`namedOnRoles_pairWithTheirOwnContainer`), 규칙 밖 전경 역할이 표에서 빠지면 `everyForegroundRole_appearsInTheContrastTable` 가 깨진다. 표의 각 행에는 그 조합이 **실제로 그려지는 자리**를 적는다 — 쓰이지 않는 조합으로 통과율을 만들지 않는다.
- 같은 표를 라이트·다크 두 팔레트가 돈다. 밝기 대비의 방향이 반대라 한쪽만 재면 반쪽이다.

## 새 토큰·새 조합을 더할 때

1. `CareerCompassColors` 에 역할을 더했으면 `CareerCompassColorsTest.roles()` 에도 더한다. 빠뜨리면 커버리지 검사가 잡는다.
2. 그 역할이 **전경**이면 `CONTRAST_PAIRS` 에 「어느 배경 위에 놓이는지」를 실제 사용처와 함께 적는다. `onX` 이름을 쓰면 `x` 와의 짝은 자동으로 요구된다.
3. 기준은 **글자 크기와 쓰임으로 고른다** — 본문 4.5, 큰 글자·아이콘·경계·그래픽 3.0. 진행바처럼 「채움 vs 트랙」이 뜻을 지는 자리는 채움과 트랙 사이를 재고 3:1 을 요구한다.
4. 통과하지 못하면 **조합을 바꾸지 말고 토큰 값을 고친다.** 값을 고치면 골든(스크린샷) 기준선이 깨지므로 PR 에 `screenshot-baseline` 라벨을 붙여 CI 가 기준선을 다시 만들게 한다.
5. 기준을 적용하지 않을 조합은 「면제」 표에 **성공 기준의 예외 조항과 함께** 적는다. 조용히 빼지 않는다.

## 고친 토큰 (라이트 팔레트)

다크 팔레트는 바탕이 검정이라 brand/500·400 이 이미 7:1 을 넘는다 — 고칠 값이 없었다. 무너진 쪽은 전부 라이트였다.

| 역할 | 전 | 후 | 왜 |
|---|---|---|---|
| `primary` | `brand/500` `#10B981` | `brand/700` `#047857` | 채움만이 아니라 **전경**으로 나간다 — 탭바 선택 라벨(11sp), 적합도 게이지 숫자, 분석 축 점수(14sp Bold), 진행 표시, 브랜드 마크 위의 흰 글리프. 흰 바탕에서 2.54:1 이었다. |
| `onPrimary` | `neutral/950` | `neutral/0` | `primary` 를 내리면 검정 글자가 3.61:1 로 떨어진다. 흰 글자로 뒤집어 5.48:1. |
| `primaryEmphasis` | `brand/600` `#059669` | `brand/700` `#047857` | 강조 **문구**로 10곳 넘게 쓰인다(정렬 선택, 프로필 배너 행동, 로그인 안내). 3.77:1 로 본문 기준 미달. |
| `error` | `red/500` `#EF4444` | `red/600` `#DC2626` | 마감 임박(12sp SemiBold)·기간 입력 오류 같은 **작은 글자**로 쓰인다. 3.76:1 로 미달. |
| `onError` | `neutral/950` | `neutral/0` | `error` 를 내리면 검정 글자가 4.10:1. 흰 글자로 4.83:1. |

바뀐 값의 전/후 대비:

| 조합 | 기준 | 전(라이트) | 후(라이트) |
|---|---|---|---|
| `onAction / primary` | 4.5:1 | **2.54:1** | 5.48:1 |
| `onPrimary / primary` | 4.5:1 | 7.80:1 | 5.48:1 |
| `primary / surface` | 4.5:1 | **2.54:1** | 5.48:1 |
| `primary / subtleSurface` | 3.0:1 | **2.43:1** | 5.25:1 |
| `primary / surfaceVariant` | 3.0:1 | **2.33:1** | 5.03:1 |
| `primary / subtleOutline` | 3.0:1 | **2.01:1** | 4.35:1 |
| `primaryEmphasis / surface` | 4.5:1 | **3.77:1** | 5.48:1 |
| `primaryEmphasis / subtleSurface` | 4.5:1 | **3.61:1** | 5.25:1 |
| `primaryEmphasis / primaryContainer` | 4.5:1 | **3.58:1** | 5.21:1 |
| `error / surface` | 4.5:1 | **3.76:1** | 4.83:1 |
| `error / subtleSurface` | 4.5:1 | **3.61:1** | 4.63:1 |
| `onError / error` | 4.5:1 | 5.26:1 | 4.83:1 |

`primary` 가 라이트에서 `actionPrimary`·`primaryEmphasis` 와 같은 `brand/700` 이 된 것은 의도한 결과다. 세 역할이 라이트에서 값을 공유해도 **다크에서는 `brand/500`·`brand/500`·`brand/400` 으로 갈린다** — 역할은 값이 아니라 의미로 남는다. 다크 팔레트는 이미 `primary`·`actionPrimary` 가 같은 값이었다.

### 값이 아니라 조합을 고친 곳 — 적합도 칩 High 라벨

`CareerCompassScoreChip` 의 High 단계는 라벨을 `mutedContent`(중립 muted)로 두고 컨테이너만 `primaryContainer`(브랜드 틴트)를 썼다. 결과는 라이트 4.50:1(경계값)·다크 **3.85:1** 이다.

여기서는 토큰 값을 고치지 않았다. `mutedContent` 자체는 중립 바탕(`surface`·`subtleSurface`) 위에서 4.54~7.85:1 로 멀쩡하고, 어긋난 것은 **중립 muted 를 브랜드 컨테이너 위에 얹은 조합** 쪽이다. 값으로 고치려면 둘 중 하나인데 둘 다 더 나쁘다.

- 다크 `mutedContent` 를 `neutral/300` 으로 올리면 `onSurfaceVariant` 와 같아져 본문·보조·흐린 글자의 3단 위계가 다크에서 사라진다.
- 다크 `primaryContainer` 를 더 어둡게 내리면 화면 바탕(`neutral/950`)과 2.04:1 → 1.3:1 이 되어 칩 자체가 안 보인다.

그래서 라벨을 그 컨테이너 계열의 짝인 `onSurfaceVariant` 로 바꿨다 — 라이트 7.42:1 · 다크 6.56:1. 「조합을 피하는 게 아니라 값을 고친다」는 원칙의 예외이고, 예외인 이유를 여기 남긴다.

## 라이트/다크 조합표

45개 조합이다. 「근거」는 그 조합을 찾은 자리이고, 표 자체는 `CareerCompassColorsTest.CONTRAST_PAIRS` 가 정본이다.

| 조합 (전경 / 배경) | 기준 | 라이트 | 다크 | 판정 | 근거(쓰이는 자리) |
| --- | --- | --- | --- | --- | --- |
| `onPrimary / primary` | 4.5:1 | 5.48:1 | 7.80:1 | 통과 | 필터 개수 배지 10sp Bold · FeedScreen.kt:382 |
| `onPrimaryContainer / primaryContainer` | 4.5:1 | 9.23:1 | 6.38:1 | 통과 | 프로필 배너 본문 · FeedScreen.kt:877 |
| `onSurface / surface` | 4.5:1 | 19.80:1 | 16.44:1 | 통과 | 카드·시트 본문 |
| `onSurfaceVariant / surfaceVariant` | 4.5:1 | 7.17:1 | 10.21:1 | 통과 | Neutral 배지·읽음 배지 · FeedReadBadge.kt:63 |
| `onSuccess / success` | 4.5:1 | 7.80:1 | 7.80:1 | 통과 | 역할 계약(현재 채움 사용처 없음) |
| `onSuccessContainer / successContainer` | 4.5:1 | 5.21:1 | 6.38:1 | 통과 | Brand 배지 · CareerCompassBadge.kt |
| `onWarning / warning` | 4.5:1 | 9.22:1 | 9.22:1 | 통과 | 역할 계약(현재 채움 사용처 없음) |
| `onWarningContainer / warningContainer` | 4.5:1 | 4.51:1 | 6.29:1 | 통과 | 경고 배너 · FeedScreen.kt:834 |
| `onError / error` | 4.5:1 | 4.83:1 | 5.26:1 | 통과 | 역할 계약(현재 채움 사용처 없음) |
| `onErrorContainer / errorContainer` | 4.5:1 | 5.30:1 | 5.28:1 | 통과 | 오류 카드 · OnboardingErrorCard.kt:68 |
| `onInfo / info` | 4.5:1 | 5.38:1 | 5.38:1 | 통과 | 역할 계약 |
| `onInfoContainer / infoContainer` | 4.5:1 | 5.49:1 | 5.74:1 | 통과 | Info 배지 · CareerCompassBadge.kt |
| `inverseOnSurface / inverseSurface` | 4.5:1 | 19.80:1 | 18.16:1 | 통과 | 선택된 태그·Dark 버튼 |
| `onAction / actionPrimary` | 4.5:1 | 5.48:1 | 7.80:1 | 통과 | Primary 버튼 16sp |
| `onAction / actionDanger` | 4.5:1 | 6.47:1 | 5.26:1 | 통과 | Danger 버튼 |
| `onAction / primary` | 4.5:1 | 5.48:1 | 7.80:1 | 통과 | 브랜드 마크 글리프·완료 체크 · OnboardingBrandMark.kt:62 |
| `onSurface / subtleSurface` | 4.5:1 | 18.97:1 | 18.16:1 | 통과 | 화면 제목·본문 |
| `onSurface / surfaceVariant` | 4.5:1 | 18.16:1 | 13.88:1 | 통과 | ScoreChip Mid 점수 |
| `onSurface / primaryContainer` | 4.5:1 | 18.79:1 | 8.91:1 | 통과 | 생체인증 원 안 이모지 · BiometricLoginScreen.kt:180 |
| `onSurface / successContainer` | 4.5:1 | 18.79:1 | 8.91:1 | 통과 | 강점 코멘트 본문 · PostingDetailScreen.kt:446 |
| `onSurface / warningContainer` | 4.5:1 | 17.78:1 | 8.32:1 | 통과 | 약점 코멘트 본문 · PostingDetailScreen.kt:446 |
| `onSurfaceVariant / surface` | 4.5:1 | 7.81:1 | 12.09:1 | 통과 | 읽은 공고 제목 · FeedScreen.kt:678 |
| `onSurfaceVariant / subtleSurface` | 4.5:1 | 7.49:1 | 13.36:1 | 통과 | 화면 보조 문구 · FeedScreen.kt:200 |
| `onSurfaceVariant / primaryContainer` | 4.5:1 | 7.42:1 | 6.56:1 | 통과 | ScoreChip High 라벨 |
| `mutedContent / surface` | 4.5:1 | 4.74:1 | 7.11:1 | 통과 | 카드 메타 12sp SemiBold · FeedScreen.kt:738 |
| `mutedContent / subtleSurface` | 4.5:1 | 4.54:1 | 7.85:1 | 통과 | 적합도 자리표시 칩 11sp · FeedSuitabilityChip.kt:81 · 상태 화면 본문(실패 `CareerCompassFailureState` 포함, #222) · CareerCompassStateView.kt |
| `primaryEmphasis / surface` | 4.5:1 | 5.48:1 | 9.33:1 | 통과 | 정렬 선택 문구·북마크 아이콘 · FeedSortMenuContent.kt:110 |
| `primaryEmphasis / subtleSurface` | 4.5:1 | 5.25:1 | 10.30:1 | 통과 | 더보기 스피너·진행 세그먼트 |
| `primaryEmphasis / primaryContainer` | 4.5:1 | 5.21:1 | 5.06:1 | 통과 | 프로필 배너 행동 문구 11sp · FeedScreen.kt:882 |
| `primary / surface` | 4.5:1 | 5.48:1 | 7.07:1 | 통과 | 축 점수 14sp Bold · SuitabilityBreakdownRow.kt:95 |
| `primary / subtleSurface` | 3.0:1 | 5.25:1 | 7.80:1 | 통과 | 브랜드 마크 원 · OnboardingBrandMark.kt:55 |
| `actionPrimary / subtleSurface` | 4.5:1 | 5.25:1 | 7.80:1 | 통과 | 스텝 카운터 11sp SemiBold · OnboardingStepScaffold.kt:229 |
| `actionDanger / surface` | 4.5:1 | 6.47:1 | 4.76:1 | 통과 | 마감 임박 12sp SemiBold · FeedScreen.kt:733 |
| `error / surface` | 4.5:1 | 4.83:1 | 4.76:1 | 통과 | 상세 마감 12sp SemiBold · PostingDetailScreen.kt:294 |
| `error / subtleSurface` | 4.5:1 | 4.63:1 | 5.26:1 | 통과 | 기간 입력 오류 문구 · FeedDeadlineRangeEditor.kt:84 |
| `onSuccessContainer / primaryContainer` | 4.5:1 | 5.21:1 | 6.38:1 | 통과 | ScoreChip High 점수 13sp Bold |
| `onWarningContainer / surface` | 4.5:1 | 5.02:1 | 12.43:1 | 통과 | 수집 주기 안내 · BoardRegisterScreen.kt:414 |
| `interactiveOutline / surface` | 3.0:1 | 4.74:1 | 3.78:1 | 통과 | 입력 테두리 · DirectInputSheet.kt:118 |
| `interactiveOutline / subtleSurface` | 3.0:1 | 4.54:1 | 4.18:1 | 통과 | 검색·필터 버튼 테두리 · FeedScreen.kt:280 |
| `outline / subtleSurface` | 3.0:1 | 4.54:1 | 4.18:1 | 통과 | 진행바 미완료 구간 · OnboardingStepScaffold.kt:202 |
| `outlineStrong / surface` | 3.0:1 | 7.81:1 | 7.11:1 | 통과 | M3 outlineVariant · Theme.kt:119 |
| `primary / surfaceVariant` | 3.0:1 | 5.03:1 | 5.97:1 | 통과 | 적합도 게이지 채움 vs 트랙 · SuitabilityGauge.kt:84 |
| `primary / subtleOutline` | 3.0:1 | 4.35:1 | 4.09:1 | 통과 | 분석 진행 표시 vs 트랙 · CareerCompassStateView.kt:103 |
| `inverseSurface / surfaceVariant` | 3.0:1 | 18.16:1 | 13.88:1 | 통과 | 미충족 막대 vs 트랙 · SuitabilityBreakdownRow.kt:120 |
| `inverseSurface / subtleSurface` | 3.0:1 | 18.97:1 | 18.16:1 | 통과 | 선택된 경험 유형 pill · OnboardingStep3Screen.kt:155 |

## 기준을 적용하지 않는 조합 (면제)

「재지 않았다」가 아니라 「성공 기준이 요구하지 않는다」는 뜻이다. 값은 그대로 재서 적는다.

| 조합 (전경 / 배경) | 라이트 | 다크 | 면제 근거 |
| --- | --- | --- | --- |
| `disabledContent / disabledContainer` | 2.31:1 | 3.19:1 | 비활성 컴포넌트 — WCAG 1.4.3/1.4.11 예외 |
| `disabledContent / surface` | 2.52:1 | 3.78:1 | 비활성 컴포넌트 — 카드가 surface 인 채 내용만 흐려진다 |
| `subtleOutline / surface` | 1.26:1 | 1.73:1 | 장식 구분선·카드 테두리 — 정보를 지지 않는다 |
| `subtleOutline / subtleSurface` | 1.21:1 | 1.91:1 | 장식 구분선 |
| `surfaceVariant / surface` | 1.09:1 | 1.18:1 | 장식 테두리 · FeedScreen.kt:633 |
| `success / surface` | 2.54:1 | 7.07:1 | 「오늘 수집」 6dp 장식 점 — 같은 뜻을 문구가 진다 |
| `success / subtleSurface` | 2.43:1 | 7.80:1 | 같은 점이 화면 바탕 위에 설 때 |

- **비활성(disabled)** — 1.4.3 은 "비활성 사용자 인터페이스 구성 요소의 일부인 텍스트"를, 1.4.11 은 "비활성 구성 요소"를 명시적으로 제외한다. 다만 다크는 바탕이 어두워 비활성 글자가 아예 묻히기 쉬워 `darkPalette_keepsDisabledPairReadable` 로 3:1 만은 따로 지킨다(3.19:1).
- **장식 구분선·테두리** — `subtleOutline`·`surfaceVariant` 테두리는 정보를 지지 않는다. 카드의 경계가 사라져도 카드 안의 글자·간격이 그룹을 말한다. 여기서 3:1 을 요구하면 모든 실선이 진한 회색이 되어 시안이 무너진다.
- **「오늘 수집」 점** — `FeedScreen` 의 6dp 점은 `clearAndSetSemantics {}` 로 접근성 트리에서 빠져 있고, 같은 줄의 수집일 문구가 같은 사실을 말한다. 색을 못 보아도 잃는 정보가 없다.

## 색만으로 뜻을 나르는 자리 (1.4.1)

코드에서 「색이 뜻을 지는 자리」를 찾아 색 아닌 단서가 이미 있는지 확인했다. **이미 글자·모양이 함께 나가는 자리는 그대로 두었다.**

| 자리 | 색 신호 | 색 아닌 단서 | 조치 |
|---|---|---|---|
| 적합도 칩 단계(High·Mid·Low) | 컨테이너 색 | 점수 숫자뿐, 단계 자체는 색만 | **붙였다** — 채운 눈금 3·2·1 (`CareerCompassScoreChip.filledSteps`) |
| 상세 마감 임박 | `error` 글자색 | 절대 날짜뿐 | **붙였다** — 「(마감 임박)」 문구 |
| 피드 카드 마감 임박 | `actionDanger` 글자색 | 「D-2」·「오늘 마감」 문구가 임박을 진다 | 그대로 |
| 적합도 게이지 단계 | 배지 색 | `CareerCompassBadge` 의 단계 문구 | 그대로 |
| 분석 축 충족/미충족 | 막대·점수 색 | 「충족」·「미충족」 문구 + 찬 원/빈 원 | 그대로 |
| 성공·경고·오류 배지 | 컨테이너 색 | `CareerCompassBadge` 는 `label` 이 필수다 — 문구 없는 배지를 만들 수 없다 | 그대로 |
| 게시판 상태 배지 | 배지 색 | 「활성」·「일시중지」·「수집 실패 N회」 문구 | 그대로 |
| 읽음 표시 | 흐린 제목 | 「읽음」 배지(체크 아이콘 + 문구) + `stateDescription` | 그대로 |
| 북마크 on/off | 아이콘 색 | 채운/빈 아이콘 **모양** + `stateDescription` | 그대로 |
| 필터 활성 | 아이콘 색 | 개수 배지 숫자 + `stateDescription` | 그대로 |
| 선택된 정렬·경험 유형 | 강조색 | 체크 아이콘 / 컨테이너 반전 + 굵기 + `stateDescription` | 그대로 |
| 온보딩 진행바 | 세그먼트 색 | 「N/M」 카운터 + `ProgressBarRangeInfo` | 그대로 |
| 「오늘 수집」 점 | 초록 점 | 같은 줄의 수집일 문구 | 그대로 |

### 적합도 칩에 눈금을 붙인 이유

세 단계의 컨테이너 색은 라이트에서 서로 이만큼 떨어져 있다.

| 쌍 | 라이트 | 다크 |
|---|---|---|
| `primaryContainer` ↔ `surfaceVariant` | 1.03:1 | 1.56:1 |
| `surfaceVariant` ↔ `subtleSurface` | 1.04:1 | 1.31:1 |
| `primaryContainer` ↔ `subtleSurface` | 1.01:1 | 2.04:1 |

라이트에서는 **정상 시야로도** 세 칩이 사실상 같은 흰색이다. 색각 이상을 논하기 전에 이미 색이 단서가 아니었다. 그래서 단계를 채운 눈금 개수(3·2·1)로 다시 말한다. 눈금은 `dp` 로 그려 글꼴 배율을 타지 않고, 칩 전체가 하나의 접근성 노드라 스크린 리더에는 기존 `contentDescription` 만 읽힌다.

## 색각 이상 시뮬레이션 — 한 것과 못 한 것

**한 것.** 선형 sRGB 에서 Viénot–Brettel–Mollon(1999) 변환을 코드로 구현해(`CareerCompassColorsTest.Vision`) 두 가지를 자동 검사한다.

- **전색맹(achromatopsia)** — 색을 휘도로만 볼 때도 45개 조합 전부가 기준을 넘는다(`contrastSurvivesTotalColorBlindness`). 「색조 차이에 기대어 통과한 조합」이 하나도 없다는 뜻이다.
- **적록(deuteranopia)** — 의미를 지는 액센트끼리 얼마나 붙는지 잰다. 아래가 측정값이다.

| 쌍 | 정상 | 적록(녹색맹) | 청황(3형) | 전색맹 |
|---|---|---|---|---|
| 라이트 `success` ↔ `error` | 1.90:1 | 8.85:1 | 2.14:1 | 1.90:1 |
| 라이트 `warning` ↔ `error` | 2.25:1 | **1.25:1** | 2.26:1 | 2.25:1 |
| 라이트 `success` ↔ `warning` | 1.18:1 | 11.08:1 | **1.06:1** | 1.18:1 |
| 다크 `warning` ↔ `error` | 1.75:1 | **1.05:1** | 1.78:1 | 1.75:1 |
| 다크 `success` ↔ `warning` | 1.18:1 | 11.08:1 | **1.06:1** | 1.18:1 |

경고와 오류는 적록에서 1.05~1.25:1 로 사실상 같은 색이 되고, 성공과 경고는 청황에서 1.06:1 이 된다. **어떤 값을 골라도 색조로만 갈리는 세 액센트는 이 함정을 벗어나지 못한다** — 그래서 팔레트를 더 고치는 대신 배지가 문구를 반드시 함께 내보내게 두었다(`CareerCompassBadge` 의 `label` 은 필수 인자다). `redGreenSimulation_collapsesSemanticAccents` 가 이 사실을 수치로 고정한다. 이 테스트가 깨지면 「색으로도 갈리게 됐다」는 뜻이니 이 문단을 다시 써야 한다.

**못 한 것.**

- **눈으로 본 판정은 하지 않았다.** 시뮬레이션 이미지를 띄워 사람이 비교한 적이 없다. 위 표는 전부 코드가 계산한 수치다.
- **1형·2형의 정도(anomalous trichromacy)** 는 재지 않았다. 완전 이색형(dichromat) 변환만 구현했다 — 부분 색각 이상은 더 나은 조건이므로 이색형이 통과하면 함께 통과한다고 본다.
- **실제 기기의 색 프로파일·밝기·야간 모드 필터**는 계산에 들어 있지 않다. 대비비는 sRGB 값 기준이다.

## 스크린샷 골든과의 관계

토큰 값을 고치면 `:core:ui` 와 `:feature:*` 의 스크린샷 기준선이 함께 깨진다. **기준선은 로컬에서 만들지 않는다** — PR 에 `screenshot-baseline` 라벨을 붙여 CI 가 새 PNG 를 커밋한다. 어떤 골든이 왜 바뀌는지는 PR 본문에 적는다.
