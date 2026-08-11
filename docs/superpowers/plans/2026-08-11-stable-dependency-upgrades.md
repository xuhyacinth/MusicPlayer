# Stable Dependency Upgrades Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade verified stable runtime and build dependencies without adopting preview, alpha, milestone or unrelated major-version migrations.

**Architecture:** Upgrade in three independently reversible groups: common Java libraries, the coupled JFace/SWT platform stack, and Maven build plugins. Run the full unit suite and shaded-package checks after every group, then independently package every SWT profile so host auto-activation cannot contaminate foreign-platform artifacts.

**Tech Stack:** Java 25, Maven 3.9, SWT/JFace, SQLite JDBC, Hutool, JTransforms, SLF4J, Maven Clean/Compiler/JAR/Surefire/Shade plugins.

---

## File Map

- Modify `pom.xml`: stable library, SWT/JFace and Maven plugin versions.
- Create `scripts/verify-shaded-jar.ps1`: reusable manifest, resource and target-SWT-native inspection.
- Modify `README.md`: verified dependency table, build environment and final validation boundary.
- Verify `target/MusicPlayer-2.0.0.0.jar`: manifest, application resources and isolated SWT native entries.

Execute this plan only after `2026-08-11-playlist-import-and-missing-song-handling.md` is complete, so every dependency group is tested against the finished feature set.

### Task 1: Capture a Clean Java 25 Baseline

**Files:**
- Verify only: `pom.xml`
- Verify only: `src/main/java`
- Verify only: `src/test/java`

- [ ] **Step 1: Confirm the worktree starts clean**

Run:

```powershell
git status --short --branch
```

Expected: no modified or untracked implementation files; only committed design and plan history may be ahead of the remote branch.

- [ ] **Step 2: Run Maven explicitly on the installed JDK 25**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn -version
    mvn clean test
    mvn clean package
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: Maven reports Java 25.0.4, 14 test classes and 42 tests pass after the functional plan, and `target/MusicPlayer-2.0.0.0.jar` is created.

- [ ] **Step 3: Record the available updates without changing the POM**

Run:

```powershell
mvn org.codehaus.mojo:versions-maven-plugin:2.18.0:display-dependency-updates
mvn org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates
```

Expected: the report includes the selected stable upgrades and may also display `slf4j 2.1.0-alpha1` or `surefire 3.6.0-M1`; those non-stable releases remain intentionally excluded.

### Task 2: Upgrade Common Runtime Libraries

**Files:**
- Modify: `pom.xml`
- Create: `scripts/verify-shaded-jar.ps1`

- [ ] **Step 1: Change only the common-library versions**

Update the existing SLF4J property:

```xml
<slf4j.version>2.0.18</slf4j.version>
```

Update the inline dependency versions:

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.47</version>
</dependency>

<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.53.2.1</version>
    <exclusions>
        <exclusion>
            <artifactId>slf4j-api</artifactId>
            <groupId>org.slf4j</groupId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>com.github.wendykierp</groupId>
    <artifactId>JTransforms</artifactId>
    <version>3.2</version>
</dependency>
```

Keep MP3SPI 1.9.5.4, JFLAC 1.5.2 and JUnit 4.13.2 unchanged.

- [ ] **Step 2: Resolve and inspect the selected artifacts**

Run:

```powershell
mvn -DskipTests dependency:resolve
mvn dependency:tree "-Dincludes=cn.hutool:hutool-all,org.xerial:sqlite-jdbc,com.github.wendykierp:JTransforms,org.slf4j:slf4j-api,org.slf4j:slf4j-simple"
```

Expected: the tree contains Hutool 5.8.47, SQLite JDBC 3.53.2.1, JTransforms 3.2 and SLF4J 2.0.18, with no SLF4J 2.1 alpha artifact.

- [ ] **Step 3: Run the complete test and package gates on JDK 25**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn clean test
    mvn clean package
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: 42 tests pass and the Windows shaded JAR is produced.

- [ ] **Step 4: Commit the common-library upgrade**

```powershell
git add pom.xml
git commit -m "构建：升级稳定版基础依赖"
```

### Task 3: Upgrade JFace and SWT as One Platform Unit

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Update the coupled UI versions**

Change the SWT property:

```xml
<swt.version>3.134.0</swt.version>
```

Change the JFace dependency version while retaining its existing SWT and OSGi exclusions:

```xml
<dependency>
    <groupId>org.eclipse.platform</groupId>
    <artifactId>org.eclipse.jface</artifactId>
    <version>3.39.100</version>
    <exclusions>
        <exclusion>
            <artifactId>org.eclipse.swt</artifactId>
            <groupId>org.eclipse.platform</groupId>
        </exclusion>
        <exclusion>
            <artifactId>org.eclipse.osgi</artifactId>
            <groupId>org.eclipse.platform</groupId>
        </exclusion>
    </exclusions>
</dependency>
```

All four platform profiles continue to use `${swt.version}`. SWT 3.134 also needs platform metadata when `Library.isLoadable` resolves natives from a shaded JAR. Define these properties in the matching profiles:

| Profile | `swt.os` | `swt.arch` |
| --- | --- | --- |
| `windows-x64` | `win32` | `x86_64` |
| `linux-x64` | `linux` | `x86_64` |
| `macos-x64` | `macosx` | `x86_64` |
| `macos-arm64` | `macosx` | `aarch64` |

Pass the profile properties through the existing Shade manifest transformer, preserving the exact attribute names:

```xml
<manifestEntries>
    <SWT-OS>${swt.os}</SWT-OS>
    <SWT-Arch>${swt.arch}</SWT-Arch>
</manifestEntries>
```

- [ ] **Step 2: Test and package the native Windows profile on JDK 25**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn -Pwindows-x64 clean test
    mvn -Pwindows-x64 clean package
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: 42 tests pass and the application packages with SWT 3.134.0 for Windows x64.

- [ ] **Step 3: Create the reusable shaded-JAR inspection script**

Create `scripts/verify-shaded-jar.ps1`:

```powershell
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
```

- [ ] **Step 4: Inspect the Windows JAR and independently package foreign profiles**

Run each build under JDK 25 and inspect its JAR before the next `clean` replaces it:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.dll$' -ExpectedSwtOS win32 -ExpectedSwtArchitecture x86_64

    mvn "-P!windows-x64,linux-x64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.so$' -ExpectedSwtOS linux -ExpectedSwtArchitecture x86_64

    mvn "-P!windows-x64,macos-x64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.(jnilib|dylib)$' -ExpectedSwtOS macosx -ExpectedSwtArchitecture x86_64

    mvn "-P!windows-x64,macos-arm64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.(jnilib|dylib)$' -ExpectedSwtOS macosx -ExpectedSwtArchitecture aarch64
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: every build succeeds; the manifest's `SWT-OS` and `SWT-Arch` values match the selected profile, the root-level SWT native files match only the selected target extension, and macOS native headers match the selected architecture. SQLite JDBC's nested multi-platform natives under `org/sqlite/native/` are intentionally outside this SWT check.

- [ ] **Step 5: Rebuild the host JAR after cross-platform checks**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn -Pwindows-x64 clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.dll$' -ExpectedSwtOS win32 -ExpectedSwtArchitecture x86_64
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: the final artifact left in `target` is the Windows x64 package used for the local smoke test.

- [ ] **Step 6: Commit the coupled UI upgrade**

```powershell
git add pom.xml scripts/verify-shaded-jar.ps1
git commit -m "构建：升级JFace与SWT"
```

### Task 4: Upgrade Maven Build Plugins to Stable Releases

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Update exactly five plugin versions**

Apply these versions without changing plugin configuration:

```xml
<artifactId>maven-clean-plugin</artifactId>
<version>3.5.0</version>

<artifactId>maven-jar-plugin</artifactId>
<version>3.5.1</version>

<artifactId>maven-compiler-plugin</artifactId>
<version>3.15.0</version>

<artifactId>maven-surefire-plugin</artifactId>
<version>3.5.6</version>

<artifactId>maven-shade-plugin</artifactId>
<version>3.6.2</version>
```

Do not select Surefire 3.6.0-M1.

- [ ] **Step 2: Resolve plugins and generate the effective POM**

Run:

```powershell
mvn -DskipTests validate
mvn help:effective-pom -Doutput=target/effective-pom.xml
Select-String -Path 'target/effective-pom.xml' -Pattern 'maven-clean-plugin|maven-jar-plugin|maven-compiler-plugin|maven-surefire-plugin|maven-shade-plugin' -Context 0,1
```

Expected: validation succeeds and the effective POM shows 3.5.0, 3.5.1, 3.15.0, 3.5.6 and 3.6.2 for the selected plugins.

- [ ] **Step 3: Run test and package gates on JDK 25**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn clean test
    mvn clean package
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: 42 tests pass and Shade 3.6.2 produces the executable JAR.

- [ ] **Step 4: Commit the plugin upgrade**

```powershell
git add pom.xml
git commit -m "构建：升级Maven稳定版插件"
```

### Task 5: Document Versions and Run Final Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add the verified dependency table**

Add a “关键依赖版本” subsection after environment requirements:

```markdown
### 关键依赖版本

| 组件 | 版本 |
| --- | --- |
| Hutool | 5.8.47 |
| SQLite JDBC | 3.53.2.1 |
| JTransforms | 3.2 |
| Eclipse JFace | 3.39.100 |
| Eclipse SWT | 3.134.0 |
| SLF4J | 2.0.18 |

项目保留 MP3SPI 1.9.5.4、JFLAC 1.5.2 和 JUnit 4.13.2。依赖升级只采用稳定版本，不使用 Alpha、Milestone 或与当前需求无关的新主版本。
```

State that the release target is Java 25 and that final verification used installed JDK 25.0.4 with Maven 3.9.16. Keep the existing warning that Linux and macOS packages were cross-packaged on Windows and were not run on target desktops.

- [ ] **Step 2: Run the final unit and Windows package checks**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn -version
    mvn clean test
    mvn clean package
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
git diff --check
```

Expected: Maven reports Java 25.0.4, all 42 tests pass, package succeeds and no whitespace errors are reported.

- [ ] **Step 3: Repeat isolated SWT packages and inspect every artifact**

Run:

```powershell
$originalJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'D:\Env\JDK\jdk-25.0.4'
    mvn -Pwindows-x64 clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.dll$' -ExpectedSwtOS win32 -ExpectedSwtArchitecture x86_64
    mvn "-P!windows-x64,linux-x64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.so$' -ExpectedSwtOS linux -ExpectedSwtArchitecture x86_64
    mvn "-P!windows-x64,macos-x64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.(jnilib|dylib)$' -ExpectedSwtOS macosx -ExpectedSwtArchitecture x86_64
    mvn "-P!windows-x64,macos-arm64" -DskipTests clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.(jnilib|dylib)$' -ExpectedSwtOS macosx -ExpectedSwtArchitecture aarch64
    mvn -Pwindows-x64 clean package
    & '.\scripts\verify-shaded-jar.ps1' -NativePattern '\.dll$' -ExpectedSwtOS win32 -ExpectedSwtArchitecture x86_64
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
```

Expected: four isolated profile packages pass inspection and the final local artifact is Windows x64.

- [ ] **Step 4: Run the final Windows UI smoke check**

Run with JDK 25:

```powershell
& 'D:\Env\JDK\jdk-25.0.4\bin\java.exe' --enable-native-access=ALL-UNNAMED -jar target/MusicPlayer-2.0.0.0.jar
```

Expected: the window, add-song icon, database playlist, double-click playback, previous/next buttons, natural auto-advance and tray exit all work without an uncaught exception.

- [ ] **Step 5: Commit documentation and verify repository state**

```powershell
git add README.md
git commit -m "文档：记录依赖版本与验证结果"
git status --short --branch
git log -5 --oneline
```

Expected: the worktree is clean and the dependency work is represented by four Chinese-language commits.
