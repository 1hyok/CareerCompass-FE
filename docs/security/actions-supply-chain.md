# Actions 공급망 정책

워크플로가 부르는 외부 액션은 전부 40자 커밋 SHA 로 고정한다. 태그(`@v7`)는 업스트림이 옮길 수 있고, 브랜치 참조는 force-push 한 번에 다른 코드가 CI 에서 실행된다. 이 저장소는 그 규칙을 두 겹으로 지킨다.

- `.github/scripts/supply-chain-policy.test.mjs` 가 워크플로 전수를 읽어 SHA 고정과 허용 목록을 검사한다. 목록 밖 액션은 GitHub 쪽에서는 run 시작 시점에 죽어 로그에 사유가 남지 않으므로, 그 침묵을 diff 옆의 red 로 바꾸는 장치다.
- 조직 수준 감사 스크립트 `.github/scripts/audit-org-action-pinning.mjs` 가 조직 하위 저장소 전부의 기본 브랜치와 열린 PR head 를 훑어 floating 참조를 센다. 애프터노트에서 조직 `sha_pinning_required` 를 켜기 전에 만든 절차를 그대로 들여왔다(#243).

## 조직 정책을 켜기 전에 감사하는 이유

조직의 `sha_pinning_required` 는 하위 저장소 전부에 즉시 걸린다. floating 참조가 남은 저장소의 run 은 job 이 만들어지기 전에 `startup_failure` 로 죽고, job 이 없으니 로그에 사유도 남지 않는다. admin 만 설정 화면에서 원인을 볼 수 있다. 열린 PR 의 head 까지 훑는 이유도 같다. 정책은 머지 전 브랜치의 run 에도 걸리므로, 열린 PR 에 남은 floating 참조는 정책을 켜는 순간 그 PR 의 CI 를 죽인다.

## 감사 (2026-09-05)

```bash
GH_TOKEN=$(gh auth token) node .github/scripts/audit-org-action-pinning.mjs \
  --org Team-CareerCompass --output /tmp/org-pinning-audit.json
echo "exit=$?"   # floating 이 하나라도 있으면 1
```

| 항목 | 값 |
| --- | --- |
| 감사 시각 | 2026-09-05T04:56:59Z |
| 대상 조직 | `Team-CareerCompass` (저장소 4개, 그중 아카이브 1개) |
| 스캔 리비전 | 4개 (기본 브랜치 4개, 열린 PR 0건) |
| 총 `uses` | 129건 (SHA 고정 103, 로컬 `./` 26, floating 0) |

| 저장소 | 기본 브랜치 | 워크플로 파일 | `uses` | floating |
| --- | --- | ---: | ---: | ---: |
| `CareerCompass-FE` | `develop` | 33 | 129 | 0 |
| `CareerCompass-BE` | `main` | 0 | 0 | 0 |
| `CareerCompass-AI` | `main` | 0 | 0 | 0 |
| `CareerCompass-Design` (아카이브) | `main` | 0 | 0 | 0 |

FE 밖의 세 저장소에는 아직 워크플로가 없다. 그래서 지금은 조직 정책을 켜도 깨지는 곳이 없지만, BE·AI 가 CI 를 세우는 순간 이 표는 낡는다. 켜기 직전에 다시 돌린다.

## 지금 상태 (2026-09-05)

| 범위 | `allowed_actions` | `sha_pinning_required` |
| --- | --- | --- |
| `orgs/Team-CareerCompass` | `all` | `false` |
| `repos/Team-CareerCompass/CareerCompass-FE` | `all` | `false` |

저장소 수준의 허용 목록과 SHA 고정은 아직 GitHub 설정에 켜져 있지 않다. 지키는 것은 정책 테스트 하나다. 설정을 켤 때는 아래 순서를 따른다.

## 켜는 절차

1. 위 감사를 다시 돌려 floating 0건과 exit 0 을 확인한다.
2. 저장소 수준부터 켠다. 허용 목록은 `supply-chain-policy.test.mjs` 의 패턴(`docker/build-push-action@*` · `docker/setup-buildx-action@*` · `google-github-actions/auth@*` 와 GitHub 소유·verified 액션)과 같아야 한다.

```bash
gh api --method PUT repos/Team-CareerCompass/CareerCompass-FE/actions/permissions \
  -F enabled=true -f allowed_actions=selected -F sha_pinning_required=true
gh api --method PUT repos/Team-CareerCompass/CareerCompass-FE/actions/permissions/selected-actions \
  -F github_owned_allowed=true -F verified_allowed=true \
  -f 'patterns_allowed[]=docker/build-push-action@*' \
  -f 'patterns_allowed[]=docker/setup-buildx-action@*' \
  -f 'patterns_allowed[]=google-github-actions/auth@*'
```

3. BE·AI 저장소에 워크플로가 생겼다면 그쪽도 SHA 고정으로 옮긴 뒤에만 조직 수준을 켠다. 조직 `allowed_actions` 는 `all` 로 둔다. 조직 목록은 하위 저장소 목록의 합집합이 돼 가장 느슨해지고, `patterns_allowed` 는 공개 저장소에만 적용되며, 조직 목록이 저장소 목록을 덮어쓰는지 문서에 없다.

```bash
gh api --method PUT orgs/Team-CareerCompass/actions/permissions \
  -f enabled_repositories=all -f allowed_actions=all -F sha_pinning_required=true
gh api orgs/Team-CareerCompass/actions/permissions
```

4. 켠 뒤 BE·AI 의 run 이 `startup_failure` 없이 도는지 실제 run 으로 확인한다. 되돌릴 때는 같은 명령에 `sha_pinning_required=false` 를 준다.
