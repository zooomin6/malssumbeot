# 02. Spring Boot 스캐폴딩

## 1. Spring Initializr를 curl로 쓰기

[start.spring.io](https://start.spring.io)는 웹 UI뿐 아니라 HTTP API로도 프로젝트를 생성해 준다:

```bash
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d baseDir=backend \
  -d groupId=com.malssumbeot \
  -d packageName=com.malssumbeot \
  -d javaVersion=17 \
  -d dependencies=web,data-jpa,postgresql,flyway,validation \
  -o backend.zip
```

생성물에는 **Maven 래퍼(`mvnw`)** 가 포함된다. 래퍼는 "이 프로젝트가 쓸 Maven 버전을
프로젝트 안에 고정"하는 장치라, 팀원/CI가 Maven을 따로 설치할 필요가 없다.

## 2. 왜 Gradle이 아니라 Maven인가

개발 머신에 JDK 25만 설치돼 있었다. Gradle은 JVM 버전 호환에 민감해서
(Gradle 9.x부터 JDK 25 지원) Initializr가 주는 구버전 래퍼가 안 돌 위험이 있었다.
Maven은 어느 JDK에서나 안정적으로 돈다. **빌드 도구 선택이 취향이 아니라
환경 제약에서 나온 사례** — 이런 판단 근거를 D-009로 기록했다.

관련 개념: `pom.xml`의 `<java.version>17</java.version>`은 **JDK 25로 빌드하되
Java 17 문법/바이트코드로 제한**한다는 뜻이다 (`javac --release 17`).
런타임 JDK와 타겟 버전은 분리할 수 있다.

## 3. Boot 4.x가 기본인데 3.5.15로 내린 이유

2026년 현재 Initializr 기본값은 Spring Boot 4.x다. 하지만 프로젝트 헌법(CLAUDE.md)이
"Spring Boot 3.x"를 명시한다. 에이전트 개발에서 중요한 원칙: **도구의 기본값보다 프로젝트
명세가 우선**이다. Maven Central 메타데이터를 조회해 3.x 최신 패치(3.5.15)를 골랐다:

```bash
curl -s https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml \
  | grep -oE '<version>3\.5\.[0-9]+</version>' | tail -5
```

참고로 4.x는 의존성 이름도 다르다 (`spring-boot-starter-webmvc` vs 3.x의 `spring-boot-starter-web`).
버전을 내릴 때 pom을 통째로 다시 썼다.

## 4. 설정 관리: application.yml

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:55432/malssumbeot}
```

- `${환경변수:기본값}` 문법 — **비밀값은 코드에 넣지 않고 환경변수로 주입**하는 12-factor 원칙.
  로컬은 기본값으로 돌고, 운영에서는 환경변수가 덮어쓴다.
- `jpa.hibernate.ddl-auto: validate` — 스키마 생성은 Flyway가 담당하고(03장),
  Hibernate는 엔티티와 실제 스키마가 일치하는지 *검증만* 한다. 불일치하면 부팅 실패 →
  마이그레이션 누락을 배포 시점에 잡는다.
- `open-in-view: false` — JPA 기본값의 흔한 함정. 뷰 렌더링까지 DB 커넥션을 잡고 있는
  동작을 끈 것. API 서버에서는 끄는 게 정석.

## 5. 패키지 구조 = 도메인 구조

```
com.malssumbeot
 ├─ bible/          # 성경 조회·검증 (04장)
 ├─ orchestrator/   # Claude 호출·의도 분류 (05장)
 ├─ crisis/         # 위기 감지 (06장)
 └─ prompt/         # 프롬프트 조립 (예정)
```

레이어(controller/service/repository)가 아니라 **도메인 기준으로 먼저 나눴다**.
파이프라인 순서(위기 감지 → 의도 분류 → 프롬프트 분기 → 응답 생성 → 구절 검증)가
패키지 구조에 그대로 드러나는 게 목표.
