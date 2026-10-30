param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
)

$ErrorActionPreference = 'Stop'

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { $null }
$javac = if ($javaHome) { Join-Path $javaHome 'bin\javac.exe' } else { (Get-Command javac.exe).Source }
$java = if ($javaHome) { Join-Path $javaHome 'bin\java.exe' } else { (Get-Command java.exe).Source }
$outputDirectory = Join-Path $RepositoryRoot 'build\keycode-self-test'
$mainSource = Join-Path $RepositoryRoot 'src\main\java\dev\xef2\visualkeymap\input\CanonicalInputCode.java'
$testSource = Join-Path $RepositoryRoot 'src\test\java\dev\xef2\visualkeymap\input\CanonicalInputCodeSelfTest.java'
$legacyPath = Join-Path $RepositoryRoot 'docs\porting-26.3\evidence\legacy-extended-key-probe.log'
$extensionPath = Join-Path $RepositoryRoot 'docs\porting-26.3\evidence\proposed-26.3-extension-map.tsv'

[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

& $javac --release 25 -encoding UTF-8 -d $outputDirectory $mainSource $testSource
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outputDirectory dev.xef2.visualkeymap.input.CanonicalInputCodeSelfTest $legacyPath $extensionPath
if ($LASTEXITCODE -ne 0) {
    throw "self-test failed with exit code $LASTEXITCODE"
}
