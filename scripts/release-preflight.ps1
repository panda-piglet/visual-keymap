[CmdletBinding()]
param(
    [string]$Repository = $env:GITHUB_REPOSITORY,
    [string]$Commit = '',
    [string]$BuildResult = 'success',
    [string]$ExpectedMinecraftVersion = '26.3',
    [string]$ExpectedModVersion = '1.4.1-community-beta.1',
    [string]$ReportDirectory = 'build/release-preflight'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $root

if (-not [System.IO.Path]::IsPathRooted($ReportDirectory)) {
    $ReportDirectory = Join-Path $root $ReportDirectory
}
New-Item -ItemType Directory -Force -Path $ReportDirectory | Out-Null

$checks = [System.Collections.Generic.List[object]]::new()
function Add-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Details
    )
    $script:checks.Add([pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Details = $Details
    })
}

function Read-GradleProperties {
    param([string]$Path)
    $result = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }
        $parts = $trimmed.Split('=', 2)
        if ($parts.Count -eq 2) {
            $result[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $result
}

function Escape-MarkdownCell {
    param([string]$Value)
    if ($null -eq $Value) {
        return ''
    }
    return ($Value -replace '\|', '\|') -replace "`r?`n", '<br>'
}

if ([string]::IsNullOrWhiteSpace($Commit)) {
    $Commit = (git rev-parse HEAD).Trim()
}
if ([string]::IsNullOrWhiteSpace($Repository)) {
    $Repository = '(local checkout)'
}

$properties = Read-GradleProperties -Path (Join-Path $root 'gradle.properties')
$actualMinecraftVersion = [string]$properties['minecraft_version']
$actualModVersion = [string]$properties['mod_version']
$actualLoaderVersion = [string]$properties['loader_version']
$actualFabricVersion = [string]$properties['fabric_version']

Add-Check -Name 'Gradle build and tests' `
    -Passed ($BuildResult -eq 'success') `
    -Details "Build step outcome: $BuildResult"
Add-Check -Name 'Minecraft target version' `
    -Passed ($actualMinecraftVersion -eq $ExpectedMinecraftVersion) `
    -Details "Expected $ExpectedMinecraftVersion; gradle.properties has $actualMinecraftVersion"
Add-Check -Name 'Planned mod version' `
    -Passed ($actualModVersion -eq $ExpectedModVersion) `
    -Details "Expected $ExpectedModVersion; gradle.properties has $actualModVersion"

$productionJars = @(
    Get-ChildItem -LiteralPath (Join-Path $root 'build/libs') -Filter '*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|dev|javadoc)\.jar$' }
)
Add-Check -Name 'Single production JAR' `
    -Passed ($productionJars.Count -eq 1) `
    -Details "Found $($productionJars.Count) production JAR(s)"

$jar = if ($productionJars.Count -eq 1) { $productionJars[0] } else { $null }
$jarHash = ''
$jarSize = 0
$jarRelativePath = ''
$jarMetadataVersion = ''
$jarMinecraftConstraint = ''
$jarEntries = @()

if ($null -ne $jar) {
    $jarHash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToUpperInvariant()
    $jarSize = $jar.Length
    $jarRelativePath = [System.IO.Path]::GetRelativePath($root, $jar.FullName).Replace('\', '/')

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $jarEntries = @($archive.Entries | ForEach-Object { $_.FullName })
        $fabricEntry = $archive.GetEntry('fabric.mod.json')
        if ($null -eq $fabricEntry) {
            Add-Check -Name 'fabric.mod.json in JAR' -Passed $false -Details 'Entry is missing'
        }
        else {
            $reader = [System.IO.StreamReader]::new($fabricEntry.Open())
            try {
                $fabricMetadata = $reader.ReadToEnd() | ConvertFrom-Json
            }
            finally {
                $reader.Dispose()
            }
            $jarMetadataVersion = [string]$fabricMetadata.version
            $jarMinecraftConstraint = [string]$fabricMetadata.depends.minecraft
            Add-Check -Name 'fabric.mod.json in JAR' -Passed $true -Details 'Entry is present and valid JSON'
            Add-Check -Name 'JAR mod version' `
                -Passed ($jarMetadataVersion -eq $ExpectedModVersion) `
                -Details "Expected $ExpectedModVersion; JAR metadata has $jarMetadataVersion"
            Add-Check -Name 'JAR Minecraft dependency' `
                -Passed ($jarMinecraftConstraint -eq "~$ExpectedMinecraftVersion") `
                -Details "Expected ~$ExpectedMinecraftVersion; JAR metadata has $jarMinecraftConstraint"
        }
    }
    finally {
        $archive.Dispose()
    }

    $hasLicense = @($jarEntries | Where-Object { $_ -match '^META-INF/LICENSE(?:[_.-].*)?$' }).Count -gt 0
    $hasNotice = @($jarEntries | Where-Object { $_ -match '^META-INF/NOTICE(?:[_.-].*)?$' }).Count -gt 0
    $nestedJars = @($jarEntries | Where-Object { $_ -match '\.jar$' })
    $testEntries = @($jarEntries | Where-Object {
        $_ -match '(^|/)(test|tests|testmod)/' -or
        $_ -match '(SelfTest|TestProvider|Phase4ProductionSmoke|ThrowingProvider)'
    })

    Add-Check -Name 'LICENSE packaged in JAR' `
        -Passed $hasLicense `
        -Details 'Expected a META-INF/LICENSE entry'
    Add-Check -Name 'NOTICE packaged in JAR' `
        -Passed $hasNotice `
        -Details 'Expected a META-INF/NOTICE entry'
    Add-Check -Name 'No nested third-party JARs' `
        -Passed ($nestedJars.Count -eq 0) `
        -Details "Found $($nestedJars.Count) nested JAR(s)"
    Add-Check -Name 'No test probes in production JAR' `
        -Passed ($testEntries.Count -eq 0) `
        -Details "Found $($testEntries.Count) suspicious test entry/entries"
}
else {
    Add-Check -Name 'fabric.mod.json in JAR' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'JAR mod version' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'JAR Minecraft dependency' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'LICENSE packaged in JAR' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'NOTICE packaged in JAR' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'No nested third-party JARs' -Passed $false -Details 'No unique production JAR to inspect'
    Add-Check -Name 'No test probes in production JAR' -Passed $false -Details 'No unique production JAR to inspect'
}

$javaInfo = (& java -version 2>&1 | Out-String).Trim()
$javaExitCode = $LASTEXITCODE
$gradleCommand = if ($IsWindows) { Join-Path $root 'gradlew.bat' } else { Join-Path $root 'gradlew' }
$gradleInfo = (& $gradleCommand --version --no-daemon 2>&1 | Out-String).Trim()
$gradleExitCode = $LASTEXITCODE
Add-Check -Name 'Java environment report' `
    -Passed ($javaExitCode -eq 0) `
    -Details "java -version exit code: $javaExitCode"
Add-Check -Name 'Gradle environment report' `
    -Passed ($gradleExitCode -eq 0) `
    -Details "Gradle Wrapper --version exit code: $gradleExitCode"
$timestamp = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
$ready = @($checks | Where-Object { -not $_.Passed }).Count -eq 0

$shaPath = Join-Path $ReportDirectory 'SHA256SUMS.txt'
if ($null -ne $jar) {
    Set-Content -LiteralPath $shaPath -Encoding utf8NoBOM -Value "$jarHash  $($jar.Name)"
}
else {
    Set-Content -LiteralPath $shaPath -Encoding utf8NoBOM -Value '# No unique production JAR was generated.'
}

$metadata = @(
    "Repository: $Repository"
    "Commit: $Commit"
    "Ref: $($env:GITHUB_REF)"
    "Generated UTC: $timestamp"
    "Build result: $BuildResult"
    "Preflight ready: $($ready.ToString().ToLowerInvariant())"
    "Expected Minecraft version: $ExpectedMinecraftVersion"
    "Actual Minecraft version: $actualMinecraftVersion"
    "Expected mod version: $ExpectedModVersion"
    "Actual Gradle mod version: $actualModVersion"
    "Actual JAR mod version: $jarMetadataVersion"
    "Fabric Loader version: $actualLoaderVersion"
    "Fabric API version: $actualFabricVersion"
    "JAR file: $($jar.Name)"
    "JAR size: $jarSize"
    "JAR SHA-256: $jarHash"
    ''
    'Java:'
    $javaInfo
    ''
    'Gradle:'
    $gradleInfo
)
Set-Content -LiteralPath (Join-Path $ReportDirectory 'BUILD-METADATA.txt') -Encoding utf8NoBOM -Value $metadata

$verification = [System.Collections.Generic.List[string]]::new()
$verification.Add('# Release Preflight Verification')
$verification.Add('')
$verification.Add("- Repository: ``$Repository``")
$verification.Add("- Commit: ``$Commit``")
$verification.Add("- Generated: ``$timestamp``")
$verification.Add("- Overall result: **$(if ($ready) { 'READY' } else { 'NOT READY' })**")
$verification.Add('')
$verification.Add('| Check | Result | Details |')
$verification.Add('| --- | --- | --- |')
foreach ($check in $checks) {
    $result = if ($check.Passed) { 'PASS' } else { 'FAIL' }
    $verification.Add("| $(Escape-MarkdownCell $check.Name) | $result | $(Escape-MarkdownCell $check.Details) |")
}
$verification.Add('')
$verification.Add('## Artifact')
$verification.Add('')
$verification.Add("- File: ``$($jar.Name)``")
$verification.Add("- Size: ``$jarSize`` bytes")
$verification.Add("- SHA-256: ``$jarHash``")
$verification.Add('')
$verification.Add('## Tests')
$verification.Add('')
if ($BuildResult -eq 'success') {
    $verification.Add('- `clean test build` completed successfully; Gradle executed the configured JUnit test task.')
}
else {
    $verification.Add("- Build/test step did not succeed (outcome: ``$BuildResult``).")
}
$verification.Add('')
$verification.Add('## Unmet release conditions')
$verification.Add('')
$failedChecks = @($checks | Where-Object { -not $_.Passed })
if ($failedChecks.Count -eq 0) {
    $verification.Add('- None. This is a qualified candidate for maintainer review; it is not automatically published.')
}
else {
    foreach ($failedCheck in $failedChecks) {
        $verification.Add("- **$($failedCheck.Name):** $($failedCheck.Details)")
    }
}
Set-Content -LiteralPath (Join-Path $ReportDirectory 'RELEASE-VERIFICATION.md') -Encoding utf8NoBOM -Value $verification

if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_OUTPUT)) {
    Add-Content -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8NoBOM -Value "ready=$($ready.ToString().ToLowerInvariant())"
    Add-Content -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8NoBOM -Value "jar_path=$jarRelativePath"
}

Write-Host "Release preflight result: $(if ($ready) { 'READY' } else { 'NOT READY' })"
Write-Host "Verification report: $(Join-Path $ReportDirectory 'RELEASE-VERIFICATION.md')"
if (-not $ready) {
    exit 1
}
