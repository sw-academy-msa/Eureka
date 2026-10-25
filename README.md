# STOCKDROP Eureka Server

Java 21 / Spring Boot 4.1.1 / Spring Cloud 2025.1.4 / Gradle 9.8.2 기반 서비스 디스커버리 서버입니다.
EC2 두 대에 Docker로 배포하고 서로 peer로 등록합니다. Gateway는 별도 프로젝트입니다.

## 로컬 실행

Java 21을 JAVA_HOME에 지정한 뒤 실행합니다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test bootJar
```

- 대시보드: http://localhost:8761
- 레지스트리: http://localhost:8761/eureka/apps
- 상태 확인: http://localhost:8761/actuator/health

기본 local 프로필은 단독 실행입니다. 운영 Compose는 필수 노드 환경변수를 요구합니다.
로컬 bootRun은 .env를 자동으로 읽지 않으며, Compose는 .env를 읽어 환경변수를 전달합니다.

## 운영 배포

각 EC2에서 `.env.example`을 `.env`로 복사한 뒤 실제 사설 주소와 고유 ID를 지정합니다.
이미지 빌드와 배포, peer 검증, 서비스 연동은 [배포 안내](docs/deployment.md)를 따릅니다.
코드 규칙은 [AGENTS.md](AGENTS.md)에 있습니다.
