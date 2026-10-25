# EC2 두 대 배포

## 네트워크와 노드 설정

두 EC2는 서로의 사설 주소와 TCP 8761 포트에 접근할 수 있어야 합니다.
보안 그룹에서 peer 서버와 AccountService·Gateway 등 허용된 클라이언트의 보안 그룹만 8761에 접근하도록 설정합니다.
현재 HTTP API와 대시보드에는 애플리케이션 인증이 없으므로 인터넷 전체에 포트를 개방하지 않습니다.
관리자는 VPN 또는 SSH 터널로 대시보드에 접근합니다.

각 서버의 `.env`를 다음과 같이 지정합니다. 아래 IP는 예시이며 실제 주소로 교체합니다.

| 환경변수 | EC2 A | EC2 B |
|---|---|---|
| EUREKA_HOSTNAME | 10.0.1.10 | 10.0.2.10 |
| EUREKA_INSTANCE_ID | eureka-a | eureka-b |
| EUREKA_PEER_URL | http://10.0.2.10:8761/eureka/ | http://10.0.1.10:8761/eureka/ |

EUREKA_HOSTNAME에는 프로토콜이나 포트를 넣지 않습니다. 안정적인 사설 IP 또는 내부 DNS를 사용합니다.
컨테이너 내부 IP나 서로 다른 EC2에서 해석할 수 없는 Compose 서비스 이름은 사용하지 않습니다.
peer URL에는 자기 자신이 아닌 상대 노드 주소를 지정합니다.

## 이미지 생성과 실행

개발 또는 빌드 서버에서 테스트 후 이미지를 생성합니다. Docker 빌드 자체는 테스트를 실행하지 않습니다.

```powershell
.\gradlew.bat test
docker build -t stockdrop/eureka:local .
```

초기 검증은 각 EC2에서 소스를 빌드하여 실행할 수 있습니다.

```sh
cp .env.example .env
# .env를 해당 EC2 값으로 편집합니다.
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 eureka
```

운영에서는 빌드한 동일 이미지를 레지스트리에 push하고 각 서버가 pull하는 방식을 권장합니다.
EUREKA_IMAGE를 레지스트리 주소와 고정 태그 또는 digest로 설정합니다.

```sh
docker compose pull
docker compose up -d --no-build
```

두 서버를 순차적으로 업데이트합니다. 첫 노드 시작 시 상대가 아직 실행 중이 아니면 peer 연결 오류가 발생할 수 있습니다.
상대 시작 후 연결과 복제가 회복되는지 확인합니다. healthcheck는 컨테이너 상태만 표시하며 unhealthy 상태 자체가 자동 재시작을 유발하지는 않습니다.

## peer와 장애 검증

로컬 Docker에서 이미지를 빌드한 뒤 PowerShell로 자동 검증할 수 있습니다.
테스트 전용 Compose는 localhost의 18761/18762를 사용하고 종료 시 컨테이너를 정리합니다.

```powershell
./scripts/Test-Peers.ps1
```

아래 항목은 실제 EC2 배포 후에도 확인합니다.

1. 양쪽 `/actuator/health` 응답과 대시보드를 확인합니다.
2. 양쪽 대시보드에서 상대 peer가 가용 상태인지 확인하고 복제 오류 로그가 지속되지 않는지 확인합니다.
3. 테스트 서비스의 Eureka Client를 A에만 연결하여 등록하고 B의 `/eureka/apps`에서도 동일 인스턴스가 보이는지 확인합니다. 등록과 캐시 갱신에는 시간이 걸릴 수 있습니다.
4. 테스트 클라이언트에 A와 B 주소를 모두 지정한 뒤 A 컨테이너를 중단합니다. B에서 레지스트리 조회 및 서비스 재등록이 유지되는지 확인합니다.
5. A를 다시 실행하고 레지스트리 동기화가 회복되는지 확인합니다.

Eureka는 메모리 기반이며 DB나 영속 볼륨을 사용하지 않습니다. 두 노드가 모두 재시작되면 클라이언트 재등록으로 레지스트리가 복구됩니다.
peer 복제는 강한 일관성이나 즉각적인 장애 감지를 보장하지 않습니다. self-preservation을 켜 두므로 heartbeat 감소 시 오래된 등록이 남을 수 있습니다.
적은 인스턴스 수에서는 대시보드에 self-preservation 경고가 표시될 수 있으며, 경고만으로 복제 실패를 판단하지 않습니다.

## AccountService 및 별도 Gateway 연동

각 프로젝트에서 Boot 버전과 호환되는 Spring Cloud BOM과 `spring-cloud-starter-netflix-eureka-client`를 추가합니다.
AccountService의 기존 `spring.application.name: account-service`는 유지합니다.

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://10.0.1.10:8761/eureka/,http://10.0.2.10:8761/eureka/
  instance:
    hostname: ${SERVICE_HOSTNAME}
    prefer-ip-address: false
    instance-id: ${SERVICE_INSTANCE_ID}
    non-secure-port: ${SERVICE_PORT}
```

SERVICE_HOSTNAME과 SERVICE_PORT는 Gateway 등 호출자가 실제로 접근할 수 있는 EC2 주소와 호스트 공개 포트입니다.
컨테이너 포트와 호스트 포트가 다르면 호스트 포트를 광고해야 합니다. 인스턴스 ID는 서비스 복제본마다 고유해야 합니다.
Gateway 라우팅과 LoadBalancer 설정은 Gateway 프로젝트에서 구성합니다.
현재 작업은 Eureka 프로젝트만 변경하며 AccountService에는 아직 클라이언트를 추가하지 않았습니다.
