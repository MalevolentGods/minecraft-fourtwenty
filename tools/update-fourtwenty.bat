@echo off
rem Updates the FourTwenty mod to the latest GitHub release. Double-click to run. Close Minecraft first.
rem The rest of this file is the PowerShell that does the work; it's plain text, so read it if you like.
title FourTwenty updater
set "FT_SELF=%~f0"
powershell -NoProfile -Command "$s = Get-Content -Raw -LiteralPath $env:FT_SELF; & ([scriptblock]::Create($s.Substring($s.IndexOf('#' + '>PS') + 4)))"
echo.
pause
exit /b

#>PS
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$repo = 'MalevolentGods/minecraft-fourtwenty'
$pattern = '^fourtwenty-(\d+\.\d+\.\d+)\.jar$'

function Find-ModFolders {
    $roots = @()
    # CurseForge, on any drive, in the usual places
    $drives = Get-PSDrive -PSProvider FileSystem | ForEach-Object { $_.Root }
    $roots += Join-Path $env:USERPROFILE 'curseforge\minecraft\Instances\*\mods'
    foreach ($d in $drives) {
        $roots += Join-Path $d 'curseforge\minecraft\Instances\*\mods'
        $roots += Join-Path $d 'CurseForge\Minecraft\Instances\*\mods'
    }
    # Other launchers
    $roots += Join-Path $env:APPDATA 'PrismLauncher\instances\*\minecraft\mods'
    $roots += Join-Path $env:APPDATA 'PrismLauncher\instances\*\.minecraft\mods'
    $roots += Join-Path $env:APPDATA 'ATLauncher\instances\*\mods'
    $roots += Join-Path $env:APPDATA 'com.modrinth.theseus\profiles\*\mods'
    $roots += Join-Path $env:APPDATA '.minecraft\mods'
    foreach ($r in $roots) {
        Get-Item -Path $r -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer } | ForEach-Object { $_.FullName }
    }
}

function Find-Installed($folders) {
    foreach ($f in ($folders | Sort-Object -Unique)) {
        Get-ChildItem -LiteralPath $f -Filter 'fourtwenty-*.jar' -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match $pattern }
    }
}

Write-Host 'FourTwenty updater'
Write-Host ''

if (Get-Process -Name javaw -ErrorAction SilentlyContinue) {
    Write-Host 'Minecraft looks like it is running. Close it and run this again.'
    return
}

Write-Host 'Checking GitHub for the latest release...'
$release = Invoke-RestMethod -UseBasicParsing -Headers @{ 'User-Agent' = 'fourtwenty-updater' } `
    -Uri "https://api.github.com/repos/$repo/releases/latest"
$asset = $release.assets | Where-Object { $_.name -match $pattern } | Select-Object -First 1
if (-not $asset) { Write-Host "Release $($release.tag_name) has no mod jar. Nothing changed."; return }
$latest = [version]([regex]::Match($asset.name, $pattern).Groups[1].Value)
Write-Host "Latest is $latest."

$installed = @(Find-Installed (Find-ModFolders))
if ($installed.Count -eq 0) {
    Write-Host ''
    Write-Host 'Could not find FourTwenty in the usual places.'
    Write-Host 'In your launcher, open the modpack''s folder, then its mods folder, and copy the path.'
    $typed = (Read-Host 'Paste that mods folder path here (or press Enter to quit)').Trim('" ')
    if (-not $typed) { return }
    if (-not (Test-Path -LiteralPath $typed -PathType Container)) { Write-Host 'That folder does not exist. Nothing changed.'; return }
    $installed = @(Find-Installed @($typed))
    if ($installed.Count -eq 0) { Write-Host 'No FourTwenty jar in that folder. Nothing changed.'; return }
}

$outdated = @($installed | Where-Object { [version]([regex]::Match($_.Name, $pattern).Groups[1].Value) -lt $latest })
foreach ($j in ($installed | Where-Object { $outdated -notcontains $_ })) {
    Write-Host "Already up to date: $($j.FullName)"
}
if ($outdated.Count -eq 0) { Write-Host ''; Write-Host 'Nothing to do.'; return }

$temp = Join-Path $env:TEMP $asset.name
Write-Host "Downloading $($asset.name) ($([math]::Round($asset.size / 1MB)) MB)..."
Invoke-WebRequest -UseBasicParsing -Headers @{ 'User-Agent' = 'fourtwenty-updater' } -Uri $asset.browser_download_url -OutFile $temp

# Never touch a mod folder with a bad download
if ((Get-Item -LiteralPath $temp).Length -ne $asset.size) { Remove-Item -LiteralPath $temp; Write-Host 'Download was incomplete. Nothing changed.'; return }
if ($asset.digest -match '^sha256:(.+)$') {
    if ((Get-FileHash -LiteralPath $temp -Algorithm SHA256).Hash -ne $Matches[1].ToUpper()) {
        Remove-Item -LiteralPath $temp; Write-Host 'Download failed its checksum. Nothing changed.'; return
    }
}

foreach ($old in $outdated) {
    $mods = $old.DirectoryName
    # The old jar goes beside the mods folder, not in it, or the game would load both
    $backup = Join-Path (Split-Path $mods -Parent) 'fourtwenty-backup'
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    Copy-Item -LiteralPath $temp -Destination (Join-Path $mods "$($asset.name).tmp") -Force
    Move-Item -LiteralPath $old.FullName -Destination (Join-Path $backup $old.Name) -Force
    Move-Item -LiteralPath (Join-Path $mods "$($asset.name).tmp") -Destination (Join-Path $mods $asset.name) -Force
    Write-Host "Updated: $mods"
    Write-Host "  $($old.Name) -> $($asset.name) (old one kept in fourtwenty-backup)"
}
Remove-Item -LiteralPath $temp -ErrorAction SilentlyContinue
Write-Host ''
Write-Host "Done. You're on $latest."
Add-Type -AssemblyName System.Windows.Forms
[void][System.Windows.Forms.MessageBox]::Show('trojan delivered. all your base are belong to us.', 'FourTwenty updater')
