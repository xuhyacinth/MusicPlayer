param(
    [Parameter(Mandatory = $true)]
    [string] $NativePattern,

    [Parameter(Mandatory = $true)]
    [ValidateSet('win32', 'linux', 'macosx')]
    [string] $ExpectedSwtOS,

    [Parameter(Mandatory = $true)]
    [ValidateSet('x86_64', 'aarch64')]
    [string] $ExpectedSwtArchitecture
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression.FileSystem

$repository = Split-Path -Parent $PSScriptRoot
$jarPath = Resolve-Path -LiteralPath (Join-Path $repository 'target/MusicPlayer-2.0.0.0.jar')
$archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $entries = @($archive.Entries)
    $entryNames = @($entries | ForEach-Object { $_.FullName })
    $swtNativeEntries = @($entries | Where-Object {
        $_.FullName -notmatch '/' -and $_.FullName -match '(?i)^(lib)?swt.*\.(dll|so|jnilib|dylib)$'
    })
    $swtNatives = @($swtNativeEntries | ForEach-Object { $_.FullName })

    if ($swtNatives.Count -eq 0) {
        throw 'Shaded JAR contains no root-level SWT native libraries.'
    }

    $unexpectedNatives = @($swtNatives | Where-Object { $_ -notmatch $NativePattern })
    if ($unexpectedNatives.Count -gt 0) {
        throw "Shaded JAR contains SWT native libraries for another platform: $unexpectedNatives"
    }

    $macNativeEntries = @($swtNativeEntries | Where-Object {
        $_.FullName -match '(?i)\.(jnilib|dylib)$'
    })
    if ($macNativeEntries.Count -gt 0) {
        $expectedCpuType = switch ($ExpectedSwtArchitecture) {
            'x86_64' { [uint32] 0x01000007 }
            'aarch64' { [uint32] 0x0100000C }
        }

        foreach ($nativeEntry in $macNativeEntries) {
            $stream = $nativeEntry.Open()
            try {
                $header = [byte[]]::new(8)
                $bytesRead = 0
                while ($bytesRead -lt $header.Length) {
                    $read = $stream.Read($header, $bytesRead, $header.Length - $bytesRead)
                    if ($read -eq 0) {
                        break
                    }
                    $bytesRead += $read
                }
            } finally {
                $stream.Dispose()
            }

            if ($bytesRead -ne $header.Length) {
                throw "macOS native library has an incomplete Mach-O header: $($nativeEntry.FullName)"
            }
            if ($header[0] -ne 0xCF -or $header[1] -ne 0xFA -or
                $header[2] -ne 0xED -or $header[3] -ne 0xFE) {
                throw "macOS native library is not a little-endian Mach-O 64-bit binary: $($nativeEntry.FullName)"
            }

            $cpuType = [BitConverter]::ToUInt32($header, 4)
            if ($cpuType -ne $expectedCpuType) {
                throw "macOS native library architecture mismatch for $($nativeEntry.FullName): expected $ExpectedSwtArchitecture, CPU type 0x$($cpuType.ToString('X8')) found."
            }
        }
    }

    if ($entryNames -notcontains 'com/xu/music/player/main/MusicPlayer.class') {
        throw 'Shaded JAR is missing the application main class.'
    }

    if ($entryNames -notcontains 'com/xu/music/player/image/addMusic.png') {
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

    if ($manifest -cnotmatch '(?m)^Main-Class: com\.xu\.music\.player\.main\.MusicPlayer\r?$') {
        throw 'Shaded JAR has an unexpected Main-Class manifest entry.'
    }
    if ($manifest -cnotmatch "(?m)^SWT-OS: $([Regex]::Escape($ExpectedSwtOS))\r?$") {
        throw "Shaded JAR has an unexpected SWT-OS manifest entry; expected $ExpectedSwtOS."
    }
    if ($manifest -cnotmatch "(?m)^SWT-Arch: $([Regex]::Escape($ExpectedSwtArchitecture))\r?$") {
        throw "Shaded JAR has an unexpected SWT-Arch manifest entry; expected $ExpectedSwtArchitecture."
    }
} finally {
    $archive.Dispose()
}

Write-Output "Shaded JAR verification passed: $($swtNatives -join ', ')"
