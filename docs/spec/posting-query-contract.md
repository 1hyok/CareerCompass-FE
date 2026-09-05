# 공고 조회의 검색어·마감일 파라미터 (F2-3 · API_SPEC §5)

`GET /postings` 가 검색어와 마감일을 받으려면 파라미터가 무엇이어야 하는지 정한다. 이슈 [#285](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/285) 의 판정이고, 본체는 [#159](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/159), 서버 쪽 자리는 [BE #28](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/28) 이다.

BE #28 은 「쿼리 파라미터를 스펙에 추가한다」까지만 적혀 있고 이름도 의미도 미정이다. FE 가 먼저 요구를 적지 않으면 서버가 임의로 정하고, 나중에 표현할 수 없는 조건을 발견해 다시 협의하게 된다. 이 문서의 이름은 전부 제안이다. 서버가 다른 이름을 고르면 클라이언트 쪽은 한 줄 수정이고, 이 문서가 다투는 것은 이름이 아니라 **의미**다.

## 지금 클라이언트가 하는 일

조회 조건은 `FeedQuery` 한 값에 모여 있고, 그중 서버로 나가는 것과 클라이언트가 직접 거르는 것이 갈린다.

| 조건 | 어디서 걸리는가 | 근거 |
| --- | --- | --- |
| 게시판·유형·최소 적합도·읽지 않음·정렬·커서·개수 | 서버 (`GET /postings`) | `FeedQuery.toPostingQuery` → `PostingApiService.getPostings` |
| 마감일 | 클라이언트 | `FeedQuery.filterClientSide` |
| 검색어 | 클라이언트 | 같은 함수 |

`PostingApiService.getPostings` 가 실제로 싣는 것은 `boardIds[]` · `types[]` · `minScore` · `unreadOnly` · `sort` · `cursor` · `limit` 일곱뿐이다. 검색어와 마감일은 실릴 자리가 없다.

### 그래서 페이지 경계에서 조건이 샌다

서버는 조건을 모른 채 `limit`(기본 20, `PostingQuery.DEFAULT_LIMIT`)만큼 주고, 클라이언트가 그 20건에서 마감일과 검색어로 거른다. 한 페이지가 통째로 비는 일이 흔하다.

그 자리를 메우려고 `FeedViewModel.fetchUntilNonEmpty` 가 다음 커서를 따라 최대 5페이지(`MAX_EMPTY_PAGE_FOLLOW_UPS`)까지 이어 읽는다. 그러고도 목록이 한 건도 늘지 않으면 `FeedLoadMoreState.Paused` 로 서서 사용자에게 「더 찾아보기」를 넘긴다. 자동으로 계속 따라가면 걸러질 페이지만 끝없이 받게 되고, 조용히 멈추면 화면이 「끝」이라고 거짓말을 하기 때문이다.

이 우회는 세 가지를 못 한다. 총 몇 건인지 말할 수 없고, 몇 페이지를 더 받아야 결과가 나오는지 예측할 수 없으며, 사용자가 「더 찾아보기」를 누르는 횟수가 데이터 사용량이 된다.

## 제안하는 파라미터

| 이름 | 형식 | 예 | 뜻 |
| --- | --- | --- | --- |
| `q` | string | `q=인턴` | 검색어. 앞뒤 공백을 지운 값. 빈 문자열이면 보내지 않는다 |
| `dueFrom` | date | `dueFrom=2026-09-06` | 마감일 하한. 그날을 포함한다 |
| `dueTo` | date | `dueTo=2026-09-13` | 마감일 상한. 그날을 포함한다 |
| `includeNoDueDate` | boolean | `includeNoDueDate=true` | 마감일이 없는 공고를 결과에 넣는가 |
| `includeExpired` | boolean | `includeExpired=true` | 오늘 이전에 마감한 공고를 결과에 넣는가 |

날짜 형식은 `YYYY-MM-DD` 다. 응답의 `dueDate` 가 이미 그 형식이고(`WireTime.parseDate` 가 `LocalDate.parse` 로 읽는다) 요청도 같은 눈금을 써야 두 방향이 어긋나지 않는다.

`dueFrom` 과 `dueTo` 는 **한쪽만 와도 유효해야 한다.** 클라이언트의 「직접 지정」은 한쪽 끝만 고를 수 있게 만들어 뒀다(`FeedDeadlineFilter.Range` 는 둘 중 하나만 있으면 성립하고, 둘 다 비면 값 자체를 만들지 못한다). 두 끝을 모두 요구하면 「11월부터 마감하는 공고」를 보려는 사람이 반대쪽 끝을 아무 날이나 찍게 되어 조건이 왜곡된다.

## 검색 대상 필드는 제목과 키워드

지금 클라이언트가 거르는 규칙은 한 줄이다. `posting.title.contains(searchQuery, ignoreCase = true)`. 제목 하나를 대소문자 무시로 부분 일치시키고, 그 밖의 필드는 보지 않는다.

그런데 검색 입력창의 안내 문구는 `공고 제목·키워드 검색`(`feed_search_placeholder`)이다. 화면은 이미 키워드를 약속하고 있고 코드가 제목만 본다. 이 차이는 지금 아무도 눈치채지 못하는데, 서버 검색이 붙는 순간 어느 쪽이 정본인지 정해야 한다.

판정은 **제목과 파싱 키워드(`parsed.keywords`)까지, 원문 본문(`rawContent`)은 제외**다.

원문을 빼는 이유는 두 가지다. 첫째, 목록 카드는 제목만 그린다. 본문에서 걸린 공고는 왜 걸렸는지 화면이 설명할 말을 갖지 못하고, 사용자에게는 상관없는 공고가 섞인 것으로 보인다. 둘째, 오프라인 스냅샷이 갖는 것은 목록 항목(`Posting`)뿐이라 본문이 아예 없다. 온라인은 본문까지 뒤지고 오프라인은 제목만 보면 같은 검색어가 두 결과를 낸다.

매칭 규칙도 함께 정해 달라. 클라이언트는 부분 문자열 일치다. 서버가 형태소 분석기나 전문 검색 인덱스를 쓰면 「데이터」로 「빅데이터」가 걸리지 않는 식으로 결과가 갈린다. 어느 쪽이든 좋지만 정해서 스펙에 적혀 있어야 오프라인 경로를 어떻게 할지 정할 수 있다.

## `dueFrom`·`dueTo` 두 개로 표현되지 않는 것

클라이언트의 마감일 필터는 프리셋 넷과 직접 지정 하나다. 두 파라미터에 그대로 옮겨지지 않는 자리가 셋 있다.

| 프리셋 | 화면 문구 | 클라이언트 판정 | 파라미터로 |
| --- | --- | --- | --- |
| `All` | 전체 | 마감 지난 것만 뺀다. 마감일 없는 공고는 남는다 | `dueFrom`=오늘, `includeNoDueDate`=true |
| `WithinWeek` | 7일 이내 | 오늘부터 7일 이내(당일 포함). 마감일 없는 공고 제외 | `dueFrom`=오늘, `dueTo`=오늘+7, `includeNoDueDate`=false |
| `WithinMonth` | 30일 이내 | 오늘부터 30일 이내(당일 포함). 마감일 없는 공고 제외 | `dueFrom`=오늘, `dueTo`=오늘+30, `includeNoDueDate`=false |
| `IncludeExpired` | 마감 지난 공고 포함 | 전부 통과 | `includeExpired`=true, `includeNoDueDate`=true |
| `Range` | 직접 지정 | 고른 범위 안(양 끝 포함). 한쪽 끝은 열릴 수 있고, 마감일 없는 공고 제외 | `dueFrom`·`dueTo` 중 있는 것만, `includeNoDueDate`=false |

### 1. 마감일이 없는 공고

`Posting.dueDate` 는 nullable 이다. 마감일을 못 뽑은 공고가 실제로 온다는 뜻이고, 프리셋마다 그 취급이 다르다. 「전체」는 포함하고 나머지 셋은 제외한다.

날짜 범위 조건만으로는 그 구분을 말할 수 없다. `dueDate >= :dueFrom` 은 `dueDate` 가 null 인 행에 대해 참이 아니라서 조용히 빠지고, 그러면 「전체」가 마감일 없는 공고를 통째로 잃는다. 사용자에게는 공고가 사라진 것으로 보이는데 화면에는 아무 조건도 걸려 있지 않다.

그래서 별도의 불리언이 필요하다. 기본값은 서버가 정해 스펙에 적어 달라. 클라이언트는 어느 쪽이든 매번 명시해 보낼 수 있다.

### 2. 지정 범위에는 「마감 지남 숨김」을 얹지 않는다

F2-3 의 「공고 상태 관리」는 마감 공고를 기본 숨김으로 둔다. 클라이언트는 그 규칙을 「직접 지정」에는 적용하지 않는다. 날짜를 직접 고른 조회에까지 오늘 기준 숨김을 얹으면 지난 범위를 고른 결과가 언제나 빈 목록이 되어 필터가 거짓말을 하기 때문이다.

서버도 같은 예외를 져야 한다. `dueFrom` 이나 `dueTo` 가 하나라도 오면 「오늘 이전 숨김」을 걸지 않는다. 그러지 않으면 3월 마감 공고를 찾는 조회가 언제나 0건으로 오고, 클라이언트는 그 0 이 공고가 없어서인지 서버가 숨겨서인지 구분할 수 없다.

정리하면 마감 지남 숨김의 기본값은 조건부다. 지정 범위가 없을 때만 켜지고, `includeExpired=true` 로 끌 수 있다.

### 3. 「오늘」은 클라이언트가 정한다

클라이언트의 오늘은 기기 시계다. `FeedClockModule` 이 `Clock.systemDefaultZone()` 을 단일 정본으로 제공하고, 7일·30일 프리셋과 마감 지남 판정이 모두 그 시계로 잰다.

서버가 UTC 로 오늘을 잡으면 한국 시각 오전 9시 이전에는 하루가 어긋나서 「7일 이내」가 6일이나 8일이 된다.

제안은 이렇다. 프리셋의 날짜 계산은 클라이언트가 하고, 오늘을 기기에서 읽어 `dueFrom`·`dueTo` 를 절대 날짜로 만들어 보낸다. 서버는 「오늘」을 해석하지 않고 받은 범위만 적용한다. 시간대 정본이 한 곳(기기)에만 있게 된다.

서버가 오늘을 알아야 하는 자리는 하나 남는다. 지정 범위가 없을 때의 마감 지남 숨김이다. 그 판정의 시간대를 `Asia/Seoul` 로 고정해 스펙에 적어 달라. 사용자도 수집 대상 게시판도 국내라 UTC 로 재면 하루가 어긋나기만 한다.

`withinDays=7` 같은 상대 조건 파라미터는 만들지 말아 달라. 절대 날짜와 상대 날짜가 함께 있으면 둘이 동시에 왔을 때 어느 쪽이 이기는지 또 정해야 하고, 클라이언트는 「직접 지정」 때문에 어차피 절대 날짜를 보낼 수 있어야 한다.

## 서버 필터가 대체하는 범위와 오프라인에 남는 범위

계약이 도착하면 온라인 경로의 우회는 걷어 낸다. 오프라인 경로는 걷어 내지 못한다.

| 코드 | 계약 도착 뒤 |
| --- | --- |
| `GetFeedPageUseCase` 의 `filterClientSide` 호출 | 지운다 |
| `FeedViewModel.fetchUntilNonEmpty`(빈 페이지 5장 추적) | 지운다 |
| `FeedLoadMoreState.Paused` 와 「더 찾아보기」 | 지운다 |
| `FeedViewModel.showOfflineSnapshot` 의 `filterClientSide` 호출 | 남는다 |
| `FeedQuery.filterClientSide` 함수 자체 | 남는다 |

오프라인 스냅샷(`FeedSnapshot`)은 기본 조건 첫 페이지 한 장의 사본이다(최대 20건, `FeedSnapshot.MAX_POSTINGS`). 서버에 못 닿는 상태에서 사용자가 조건을 걸면 그 조건을 적용할 곳은 클라이언트뿐이라, 마감일 규칙은 오프라인에서 계속 클라이언트 코드가 판정한다. 지워지는 것은 함수가 아니라 온라인 경로의 호출이다.

마감일은 그래도 괜찮다. 판정에 필요한 값(`dueDate`)이 목록 항목에 실려 있어 서버와 같은 규칙을 돌릴 수 있다.

검색은 다르다. 서버가 파싱 키워드까지 뒤지기로 하면 `Posting` 에는 `keywords` 가 없어서 오프라인이 그 규칙을 흉내 낼 수 없다. 같은 검색어가 온라인과 오프라인에서 다른 목록을 내는데, 사용자에게는 둘 다 「검색 결과」로 보인다. 그래서 오프라인에서는 검색을 잠그고 스냅샷 전체를 보여 준다. 지금 오프라인은 북마크만 잠겨 있고(`feed_offline_read_only`) 검색은 열려 있으므로, 잠금과 안내 문구는 #159 에서 함께 만든다.

## 서버 담당자가 답해 줘야 할 것

- 파라미터 이름 다섯의 확정값. 다른 이름이어도 좋다.
- 검색 대상 필드가 제목과 키워드로 정해지는가. 원문 본문을 넣겠다면 그 이유.
- 검색 매칭이 부분 문자열인가 토크나이저인가.
- `includeNoDueDate` 의 기본값.
- 지정 범위 조회에 마감 지남 숨김을 얹지 않는다는 예외를 서버도 지는가.
- 마감 지남 판정의 시간대. 제안은 `Asia/Seoul` 고정이다.

## 코드 지도

| 무엇 | 어디 |
| --- | --- |
| 서버 쿼리 값 | `core/model/.../posting/Posting.kt` 의 `PostingQuery` |
| Retrofit 파라미터 | `core/network/.../service/PostingApiService.kt` 의 `getPostings` |
| 요청 조립 | `core/data/.../repoimpl/posting/PostingRepositoryImpl.kt` |
| 조회 조건에서 서버 쿼리로 | `feature/feed/domain/.../model/FeedQuery.kt` 의 `toPostingQuery` |
| 클라이언트 필터 규칙 | 같은 파일의 `filterClientSide` |
| 마감일 프리셋 | 같은 파일의 `FeedDeadlineFilter` |
| 빈 페이지 추적과 멈춤 | `feature/feed/presentation/.../feed/FeedViewModel.kt` |
| 오프라인 스냅샷 필터 | 같은 파일의 `showOfflineSnapshot` |
| 「오늘」의 시계 | `feature/feed/data/.../di/FeedClockModule.kt` |
| 응답 날짜 형식 | `core/data/.../mapper/WireTime.kt` |
