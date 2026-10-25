# 운영 프로필 컨테이너 두 개로 복제와 한 노드 중단 시 동작을 확인합니다.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
$composeArgs = @('compose', '-p', 'stockdrop-eureka-peer-test', '-f', 'compose.peer-test.yaml')

function Wait-Until([scriptblock] $Check, [string] $Description) {
    $deadline = (Get-Date).AddSeconds(180)
    do {
        try { if (& $Check) { Write-Output "PASS: $Description"; return } } catch { }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)
    throw "시간 초과: $Description"
}

function Get-Registry([int] $Port) {
    Invoke-RestMethod "http://localhost:$Port/eureka/apps/PEER-PROBE" -Headers @{Accept='application/json'} -TimeoutSec 5
}

try {
    & docker @composeArgs up -d
    if ($LASTEXITCODE -ne 0) { throw '컨테이너 시작 실패' }
    foreach ($port in 18761,18762) {
        Wait-Until { (Invoke-RestMethod "http://localhost:$port/actuator/health" -TimeoutSec 5).status -eq 'UP' } "노드 $port 시작"
    }
    # A에만 등록하여 B에서 보이는 정보가 peer 복제로 전달된 것인지 검증합니다.
    $body = @{instance=@{
        instanceId='peer-probe-1'; hostName='probe'; app='PEER-PROBE'; ipAddr='127.0.0.1'; status='UP'
        port=@{'$'=8080; '@enabled'='true'}
        dataCenterInfo=@{'@class'='com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo'; name='MyOwn'}
    }} | ConvertTo-Json -Depth 6
    Invoke-RestMethod 'http://localhost:18761/eureka/apps/PEER-PROBE' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 5 | Out-Null
    Wait-Until { @((Get-Registry 18762).application.instance).instanceId -contains 'peer-probe-1' } 'A에서 B로 등록 복제'
    & docker @composeArgs stop peer-a
    if ($LASTEXITCODE -ne 0) { throw 'A 중단 실패' }
    Wait-Until { @((Get-Registry 18762).application.instance).instanceId -contains 'peer-probe-1' } 'A 중단 후 B 레지스트리 조회'
    Invoke-RestMethod 'http://localhost:18762/eureka/apps/PEER-PROBE/peer-probe-1' -Method Put -TimeoutSec 5 | Out-Null
    Write-Output 'PASS: A 중단 후 B에서 heartbeat 수신'
    & docker @composeArgs start peer-a
    if ($LASTEXITCODE -ne 0) { throw 'A 재시작 실패' }
    Wait-Until { @((Get-Registry 18761).application.instance).instanceId -contains 'peer-probe-1' } 'A 재시작 후 레지스트리 복구'
} finally {
    # 이 스크립트 전용 프로젝트의 컨테이너와 네트워크만 정리합니다.
    & docker @composeArgs down
    Pop-Location
}
