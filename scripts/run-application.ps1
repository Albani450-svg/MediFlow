param(
    [Parameter(Position = 0)]
    [ValidateSet('jalan', 'periksa', 'dummy')]
    [string]$Perintah = 'jalan'
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

function Cari-Alat([string]$nama) {
    if ($env:JAVA_HOME) {
        $calon = Join-Path $env:JAVA_HOME "bin\$nama.exe"
        if (Test-Path -LiteralPath $calon) { return $calon }
    }
    $ditemukan = Get-Command $nama -ErrorAction SilentlyContinue
    if ($ditemukan) { return $ditemukan.Source }
    throw "JDK tidak ditemukan. Pasang JDK 17 atau lebih baru, atau isi JAVA_HOME."
}

$javac = Cari-Alat 'javac'
$java = Cari-Alat 'java'
New-Item -ItemType Directory -Force -Path 'api\out' | Out-Null
$berkas = @(Get-ChildItem -Recurse -Filter *.java -Path 'api\src' | ForEach-Object { $_.FullName })
& $javac --release 17 -encoding UTF-8 -cp 'api\lib\*' -d 'api\out' @berkas
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

switch ($Perintah) {
    'periksa' {
        & $java -cp 'api\out;api\lib\*' mediflow.VisitFlowCheck
        exit $LASTEXITCODE
    }
    'dummy' {
        Write-Host '[mediflow] Mengganti isi 15 tabel medicflow_db dengan data uji.'
        & $java -cp 'api\out;api\lib\*' mediflow.LoadDemoData
        exit $LASTEXITCODE
    }
    default {
        if (-not $env:MEDIFLOW_TANPA_BROWSER) {
            Start-Process -FilePath powershell -WindowStyle Hidden -ArgumentList @(
                '-NoProfile', '-Command', "Start-Sleep -Seconds 2; Start-Process 'http://127.0.0.1:8088/'"
            ) | Out-Null
        }
        & $java -cp 'api\out;api\lib\*' mediflow.ApplicationServer
        exit $LASTEXITCODE
    }
}
