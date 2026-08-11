param(
    [Parameter(Mandatory = $true)]
    [string] $NativePattern
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$repository = Split-Path -Parent $PSScriptRoot
$jarPath = Resolve-Path -LiteralPath (Join-Path $repository 'target/MusicPlayer-2.0.0.0.jar')
$archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $entries = @($archive.Entries | ForEach-Object { $_.FullName })
    $swtNatives = @($entries | Where-Object {
        $_ -notmatch '/' -and $_ -match '(?i)^(lib)?swt.*\.(dll|so|jnilib|dylib)$'
    })

    if ($swtNatives.Count -eq 0) {
        throw 'Shaded JAR contains no root-level SWT native libraries.'
    }

    $unexpectedNatives = @($swtNatives | Where-Object { $_ -notmatch $NativePattern })
    if ($unexpectedNatives.Count -gt 0) {
        throw "Shaded JAR contains SWT native libraries for another platform: $unexpectedNatives"
    }

    if ($entries -notcontains 'com/xu/music/player/main/MusicPlayer.class') {
        throw 'Shaded JAR is missing the application main class.'
    }

    if ($entries -notcontains 'com/xu/music/player/image/addMusic.png') {
        throw 'Shaded JAR is missing the add-song icon.'
    }

    $manifestEntry = $archive.GetEntry('META-INF/MANIFEST.MF')
    if ($null -eq $manifestEntry) {
        throw 'Shaded JAR is missing META-INF/MANIFEST.MF.'
    }

    $reader = [System.IO.StreamReader]::new($manifestEntry.Open())
    try {
        $manifest = $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }

    if ($manifest -notmatch 'Main-Class: com\.xu\.music\.player\.main\.MusicPlayer') {
        throw 'Shaded JAR has an unexpected Main-Class manifest entry.'
    }
} finally {
    $archive.Dispose()
}

Write-Output "Shaded JAR verification passed: $($swtNatives -join ', ')"
