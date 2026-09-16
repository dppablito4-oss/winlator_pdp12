$ErrorActionPreference = 'Stop'

$candidateToolRoots = @(
    (Join-Path $PSScriptRoot '.tools'),
    (Join-Path (Split-Path -Parent $PSScriptRoot) '.tools'),
    (Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) '.tools')
)

foreach ($toolRoot in $candidateToolRoots) {
    $jdkRoot = Join-Path $toolRoot 'jdk17'
    $sdkRoot = Join-Path $toolRoot 'android-sdk'
    $jdk = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdk -and (Test-Path -LiteralPath $sdkRoot)) {
        $env:JAVA_HOME = $jdk.FullName
        $env:ANDROID_HOME = $sdkRoot
        $env:ANDROID_SDK_ROOT = $sdkRoot
        break
    }
}

Push-Location $PSScriptRoot
try {
    & .\gradlew.bat clean assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

$sourceApk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
$outputApk = Join-Path $PSScriptRoot 'Winlator-Steam-H200-v11.2-honor200.2-debug.apk'
Copy-Item -LiteralPath $sourceApk -Destination $outputApk -Force

$hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash
Write-Output "APK: $outputApk"
Write-Output "SHA256: $hash"
