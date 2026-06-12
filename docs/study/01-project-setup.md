# 01. 프로젝트 셋업과 에이전트 운영 구조

## 1. 왜 문서를 4종이나 만들었나

AI 에이전트(Claude Code)와 함께 개발할 때 가장 큰 문제는 **세션마다 기억이 초기화된다**는 것이다.
사람이라면 어제 한 일을 기억하지만, 에이전트는 매번 백지에서 시작한다. 그래서 "기억"을 파일로 외재화했다:

| 파일 | 역할 | 비유 |
|------|------|------|
| `.claude/CLAUDE.md` | 절대 원칙, 컨벤션, 완료 기준 | 헌법 — 바뀌면 안 되는 것 |
| `.claude/PROGRESS.md` | 현재 상태, 다음 할 일, 블로킹 항목 | 작업 보드 |
| `.claude/DECISIONS.md` | 내린 결정과 그 *이유* | 판례집 — 같은 고민 반복 방지 |
| `.claude/agents/theology-checker.md` | 독립 검사 에이전트의 기준 | 감사실 규정 |

핵심 설계 포인트:

- **결정은 이유와 함께 기록한다.** "개역한글을 쓴다"만 적으면 나중에 "개역개정으로 바꾸자"는
  논의가 다시 일어난다. "저작권 만료라서"가 같이 적혀 있으면 번복 비용이 보인다.
- **에이전트가 혼자 못 바꾸는 영역을 명시한다.** 프롬프트 내용, 신학 기준, QA 케이스, API 키.
  이런 건 "사람 확인 필요" 섹션에 적고 멈추는 게 규칙이다. 자동화의 범위를 *의도적으로* 좁힌 것.
- **만든 자와 검사하는 자 분리 (D-005).** 코드를 만든 에이전트가 자기 산출물을 검증하면
  자기 가정에 갇힌다. 그래서 신학 검사는 별도 에이전트가 "위반 가능성을 적극적으로 찾으라"는
  지시를 받고 수행한다. 실제로 05장에서 이 구조가 치명적 결함을 잡아냈다.

## 2. git 셋업에서 배운 것들

### 빈 레포에서 main 브랜치로

git의 기본 브랜치 이름은 환경에 따라 `master`일 수 있다. 커밋이 하나도 없는 상태(unborn branch)에서는
`git branch -M main`이 아니라 심볼릭 레퍼런스를 직접 바꾼다:

```bash
git symbolic-ref HEAD refs/heads/main
```

### 실행 권한 비트는 git이 기억한다

Windows에서 zip으로 받은 `mvnw`(Maven 래퍼 스크립트)는 실행 권한이 없는 채로 풀린다.
이대로 커밋하면 Linux CI에서 `./mvnw: Permission denied`가 난다. git 인덱스에 직접 기록:

```bash
git update-index --chmod=+x backend/mvnw
# 100644 (일반 파일) → 100755 (실행 가능)
```

### CRLF/LF 경고의 의미

커밋할 때 `LF will be replaced by CRLF` 경고가 잔뜩 나왔다. 이건 **저장소에는 LF로 저장되고,
Windows 작업 폴더에서만 CRLF로 보인다**는 뜻이라 무해하다. `.gitattributes`가 처리해 준다.

### GitHub 레포를 API로 만들기

`gh` CLI가 없어도, Windows 자격 증명 관리자(Git Credential Manager)에 저장된 토큰을 꺼내
GitHub REST API를 직접 호출할 수 있다:

```bash
# 저장된 자격 증명 조회 (GCM_INTERACTIVE=never로 팝업 방지)
printf "protocol=https\nhost=github.com\n\n" | git credential fill

# 레포 생성
curl -H "Authorization: token $TOKEN" https://api.github.com/user/repos \
  -d '{"name":"malssumbeot","private":true}'
```

배운 점: **한글이 든 JSON을 셸 인라인으로 보내면 인코딩이 깨질 수 있다** (Windows 코드페이지 문제).
UTF-8 파일로 저장한 뒤 `--data-binary @file.json`으로 보내면 안전하다.

## 3. 사업적 결정도 코드처럼 기록한다

D-002(카톡 챗봇 → RN 앱 전환) 같은 결정은 기술이 아니라 사업 판단이다.
그래도 DECISIONS.md에 "왜"(푸시·구독 결제에 유리, 검증 기간 2~3개월 증가는 수용)와
"영향"(webhook 모듈 → REST API + 인증 + 푸시로 대체)을 남겼다.
나중에 "왜 카톡으로 안 했지?"라는 질문이 나오면 이 기록이 답한다.
