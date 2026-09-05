# 테스트만 보는 프로덕션 선언

**`src/main` 에 새로 넣은 선언은 `src/main` 어디선가 참조돼야 한다.** 테스트만 부르거나 아무도 부르지 않는 프로덕션 함수는 죽은 코드이고, 테스트 편의 때문에 넓어진 시그니처·visibility 는 설계를 테스트에 맞춘 것이다.

## 가드

Repository Quality 의 `Reject test-only production declarations` 스텝이 `.github/scripts/validate-test-only-production-declarations.mjs` 를 돌린다(애프터노트 #1895 를 #243 으로 들여왔다).

- PR 이 `src/main` 에 **새로 추가한** 함수 선언을 diff 에서 뽑고, 그 이름이 PR 이후의 `src/main` 어디서도 참조되지 않으면 실패한다. 기존 선언은 보지 않는다.
- 참조는 이름 기준이다. 리플렉션·DI 그래프처럼 이름이 소스에 나오지 않는 소비처는 못 본다. 그런 선언은 아래 면제로 처리한다.
- 로컬에서 미리 보려면 작업 디렉터리에서 돌린다. `--local` 모드는 `GITHUB_WORKSPACE` 가 아니라 cwd 를 본다.

```bash
node .github/scripts/validate-test-only-production-declarations.mjs --local develop
```

## 면제

PR 라벨 `test-only-production-exempt` 를 붙이면 같은 위반이 error 가 아니라 warning 으로 내려간다. `issue-assignee-exempt` 와 같은 자리다. 면제는 「지금은 소비처가 없지만 다음 PR 이 쓴다」 처럼 이유가 PR 본문에 적혀 있을 때만 쓴다. 이 저장소의 MVI 베이스(#244)가 그 경우다. 화면 전환 이슈가 상속하기 전까지 `MviViewModel` 은 테스트만 참조한다.

## 테스트가 불편할 때

프로덕션을 고치지 않는다. 생성자 시그니처·visibility·DI 조립을 「테스트가 그렇게 쓰고 있어서」 로 정하지 않는다. 테스트 쪽을 고친다. fake, `@TestInstallIn`, 파일 분리가 그 수단이다. `presentation` 이 `core:datastore` 나 data 구현을 직접 참조하는 것은 `PresentationLayerDependencyKonsistTest` 가 따로 막는다.
