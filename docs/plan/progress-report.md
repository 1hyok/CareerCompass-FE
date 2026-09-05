# 결과 보고서

12월에 확정할 문서다. 지금은 2026-09-06 판이고 매월 말에 갱신한다. 마지막에 한 번에 쓰면 그때 가서 기억으로 복원해야 하고, 계획표([`schedule.md`](schedule.md))의 아홉 행마다 무엇이 실제로 있었는지가 이 문서의 뼈대다.

## 한 줄

2026-09-06 기준 실제 진척은 약 30% 다. 계획표의 9월 목표는 85% 이므로 55%p 모자라고, 계획표 자신의 곡선으로 읽으면 5월 말 수준이다.

## 계획 대비 실적

| 기간 | 목표 | 실제 | 무엇이 갈랐나 |
|---|---|---|---|
| 4월 | 15% | 달성 | 기능 명세서와 유스케이스 명세서가 나왔다 |
| 5월 | 30% | 달성 | Figma 57화면, ERD, 아키텍처, `API_SPEC v0.1` 이 나왔다 |
| 6월 | 50% | 부분 | 앱은 개발 환경과 핵심 화면까지 갔다. 이 행의 나머지인 DB 구축과 서버 MVP 로직은 시작되지 않았다 |
| 7월 | 70% | 미달 | 붙을 서버가 없다. `core:network` 의 `BASE_URL` 은 아직 `https://api.careercompass.invalid/` 다 |
| 8월 | 80% | 미달 | 실서버 통합 테스트를 한 적이 없다. 앱 내부 검증(단위·스크린샷·계약 스모크)만 돈다 |
| 9월 | 85% | 부분 | 앱 쪽 리팩터링(MVI 전환, Navigation 3 이관)과 보안 점검을 끝냈다. API 응답 속도 최적화는 잴 서버가 없다 |
| 10월 | 90% | 미착수 | 베타를 열려면 앱이 실서버에 붙어 있어야 한다 |
| 11월 | 95% | 부분 | 스토어 자산과 등록 문안은 만들었다. 클라우드 인프라 배포는 서버 몫이다 |
| 12월 | 100% | 진행 중 | 이 문서와 사용자 매뉴얼이 12월 행의 산출물이다 |

## 30% 는 어떻게 센 것인가

네 갈래에 가중치를 주고 완성도를 곱했다. 가중치는 추정이므로 다투어도 된다. 어느 쪽으로 흔들어도 40% 를 넘지 않는다.

| 갈래 | 가중치 | 완성도 | 기여 | 근거 |
|---|---|---|---|---|
| 기획·디자인 | 15% | 95% | 14.3 | 명세서·유스케이스·시안 57화면·API 스펙·정본 판정이 끝났다 |
| 앱 (Android) | 30% | 50% | 15.0 | 프로덕션 코틀린 295개가 `core`·`onboarding`·`feed`·`app` 에 있고, `editor`·`profile`·`foryou`·`notification` 네 모듈은 0개다. 시안 기준 48화면 중 19화면 |
| 서버 | 35% | 0% | 0 | 커밋 3개가 전부 문서이고 이슈 53건이 모두 열려 있다 |
| AI | 20% | 0% | 0 | 커밋 1개(README)이고 이슈 31건이 모두 열려 있다 |
| 합계 | | | 29.3% | |

다르게 재면 이렇다.

- 기능이 실제로 도는지로 재면 0% 다. 명세서 F1~F4 중 사용자가 끝까지 쓸 수 있는 기능이 없다. 수집도 분석도 생성도 서버가 하는 일이고, 앱은 가짜 응답 위에서 검증된다. 시연을 요구받으면 이 숫자가 보이는 숫자다.
- 앱 담당자 한 사람 몫으로 재면 90% 를 넘는다. `core`·`onboarding`·`feed`·`careercompass`·`platform` 은 구현이 끝났고, 그 담당의 열린 이슈는 전부 남을 기다리는 것이다.

## 기능별 구현 범위

칸마다 그것을 추적하는 이슈와 한 줄 이유를 달았다. 이슈 번호는 2026-09-06 에 세 저장소를 조회해 확인한 것이다.

| 기능 | 앱 | 서버 | AI |
|---|---|---|---|
| F1-1·F1-2 가입과 프로필 | 온보딩 4단계 입력 구현. 마이 탭의 프로필 관리는 미구현. `profile` 담당 미착수 ([#174](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/174)~[#177](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/177)) | 담당 미착수. 인증 [BE #10](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/10)~[BE #14](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/14), 프로필 [BE #15](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/15)~[BE #17](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/17) | 해당 없음 |
| F1-3 경험 카드 | 온보딩에서 등록·수정 구현. 마이 탭 목록은 미구현. `profile` 담당 미착수 ([#178](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/178)·[#179](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/179)) | 담당 미착수 ([BE #18](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/18)) | 해당 없음 |
| F1-4 과거 지원서 | 업로드와 라벨 지정 구현. 목록 관리는 미구현. `profile` 담당 미착수 ([#180](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/180)·[#181](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/181)) | 담당 미착수 ([BE #19](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/19)·[BE #20](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/20)) | 항목 분류 담당 미착수 ([AI #21](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/21)~[AI #23](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/23)) |
| F2-1·F2-2 게시판 등록과 수집 | 등록·감지 결과·수정·일시중지 구현 | 크롤러 담당 미착수 ([BE #21](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/21)~[BE #27](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/27)) | 해당 없음 |
| F2-3 피드와 공고 상세 | 목록·필터·정렬·상세·원문·오프라인·엣지 상태 구현 | 담당 미착수 ([BE #28](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/28)~[BE #30](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/30)) | 해당 없음 |
| F2-4 알림 | 미구현. `notification` 담당 미착수 ([#194](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/194)~[#197](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/197)) | 담당 미착수 ([BE #44](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/44)~[BE #47](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/47)) | 해당 없음 |
| F3 적합도 분석 | 4축 표시·충족 여부·미표시 사유·분석 중 상태 구현 | 담당 미착수 ([BE #31](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/31)~[BE #35](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/35)) | 담당 미착수. 파싱 [AI #7](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/7)~[AI #9](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/9), 채점 [AI #11](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/11)~[AI #15](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/15) |
| F4 자기소개서 | 미구현. `editor` 담당 미착수 ([#182](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/182)~[#189](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/189)) | 담당 미착수 ([BE #36](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/36)~[BE #40](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/40)) | 담당 미착수. 양식 추출 [AI #10](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/10), 생성 [AI #16](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/16)~[AI #20](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/20) |
| 추가 기능 3종 | 미구현. `foryou` 담당 미착수 ([#190](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/190)~[#193](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/193)) | 담당 미착수 ([BE #41](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/41)~[BE #43](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/43)) | 담당 미착수 ([AI #24](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/24)~[AI #26](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/26)) |

이 표의 미구현·미착수 칸은 이유가 전부 한 종류다. 담당이 착수 전이라 그 모듈에 코드가 없다. 앱의 네 모듈은 이준혁, 서버는 조영탁, AI 는 서성덕 몫이고 세 사람 다 첫 커밋 전이다. 선행 대기와 우리가 미룬 것은 이 표에 없다. 그 둘은 아래 남은 것과 그 이유에 있다.

어느 기능 행에도 안 들어가는 공통 기반도 통째로 열려 있다. 서버 골격과 공통 응답·페이징 규약([BE #1](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/1)~[BE #9](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/9)), AI 게이트웨이([AI #1](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/1)~[AI #6](https://github.com/Team-CareerCompass/CareerCompass-AI/issues/6)) 다.

앱에서 구현이라고 적은 것은 화면과 상태 처리까지다. 그 화면이 실제 데이터로 도는 것은 서버가 선 뒤에 [#265](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/265) 에서 확인한다. 구현이라고 적은 칸에도 서버 계약이 빈 데가 있지만, 화면과 상태 처리가 서 있으므로 그것을 미구현으로 내리지 않는다. 그런 공백은 칸이 아니라 이슈로 따로 추적한다. 공고 조회에 검색어·마감일 파라미터가 없어 피드가 클라이언트에서 거르는 것은 [#159](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/159)([BE #28](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/28) 과 [#285](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/285) 를 blocked_by 로 걸고 있다), refresh 재사용 정책이 안 정해진 것은 [#79](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/79)([BE #11](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/11)·[BE #12](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/12) 를 걸고 있다) 다.

## 역할별 실제 기여

저장소 기록이다. 2026-09-06 기준.

| 저장소 | 커밋 | 이슈 | 사람이 쓴 커밋의 작성자 |
|---|---|---|---|
| CareerCompass-FE | 384 (봇 51 포함) | 121 닫힘 / 31 열림 | 정일혁 333 |
| CareerCompass-BE | 3 | 0 닫힘 / 53 열림 | 정일혁 3 (문서) |
| CareerCompass-AI | 1 | 0 닫힘 / 31 열림 | 정일혁 1 (문서) |

세 저장소의 커밋이 전부 정일혁의 것이다. 앱의 절반(`editor`·`profile`·`foryou`·`notification`)은 이준혁 담당으로 어사인돼 있고, 서버는 조영탁, AI 는 서성덕 담당이다. 담당 지도의 정본은 `.github/scripts/reconcile-issue-metadata.mjs` 의 `ASSIGNEE_BY_MODULE` 이고, 이슈 담당자도 그것으로 자동 배정된다.

## 남은 것과 그 이유

이유를 세 갈래로 가른다. 담당 미착수는 그 몫을 맡은 사람이 첫 커밋 전인 것, 선행 대기는 남이 먼저 해야 우리가 움직일 수 있는 것, 우리가 미룬 것은 지금 손댈 수 있는데 뒤로 둔 것이다.

| 남은 것 | 왜 아직인가 |
|---|---|
| 앱과 서버 연동 | 선행 대기. 서버 주소가 없다. [#265](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/265) 가 [BE #7](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/7) 을 blocked_by 로 걸고 있고, 주소가 나오면 하루 안에 붙는다. 우리가 미룬 몫은 dev·prod BASE_URL 분리([#286](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/286)) 다 |
| 통합 테스트 | 선행 대기. 같은 이유다. 실서버 없이는 계약 대조까지가 한계다. 라운드 대본과 드리프트 기록 양식([#287](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/287))은 서버 없이 쓸 수 있으므로 우리가 미룬 몫이다 |
| 앱의 네 모듈 | 담당 미착수. 이준혁 담당 이슈 24건([#174](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/174)~[#197](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/197))이 열려 있다 |
| 베타 테스트 | 선행 대기. 앱이 실서버에 붙어야 열 수 있고, [#266](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/266) 이 [#265](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/265) 를 blocked_by 로 걸고 있다. 운영 규칙과 관찰형 태스크 대본([#288](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/288))은 지금 쓸 수 있으므로 우리가 미룬 몫이다 |
| 스토어 등록 | 선행 대기와 우리가 미룬 것이 섞여 있다. 스크린샷은 남은 화면([#185](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/185)), 개인정보처리방침은 서버의 LLM 전송 범위 결정([BE #48](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/48)) 뒤다([#274](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/274) 가 둘을 blocked_by 로 걸고 있다). 게시처와 절차([#290](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/290)), Play Console 계정과 앱 등록([#292](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/292)), 문의 이메일([#293](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/293))은 우리가 미룬 몫이다 |

## 아직 못 적는 것

검증 수치가 비어 있다. 통합 QA 라운드([#265](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/265))와 베타 테스트([#266](https://github.com/Team-CareerCompass/CareerCompass-FE/issues/266))가 돌아야 나온다. 그 둘이 끝나면 이 절에 참가자 수, 발견 건수, 그중 고친 것, 남긴 것을 적는다.

## 갱신

매월 말에 계획 대비 실적 표와 진척도를 다시 센다. 숫자가 바뀌면 [`schedule.md`](schedule.md) 의 실제 진척 표도 함께 고친다. 12월에 이 문서를 확정하고 발표 자료의 수치를 여기에 맞춘다.
