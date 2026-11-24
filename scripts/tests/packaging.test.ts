import { afterEach, expect, test } from "bun:test";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve, sep } from "node:path";

const directories: string[] = [];
afterEach(() => {
    for (const dir of directories.splice(0)) {
        if (!resolve(dir).startsWith(resolve(tmpdir()) + sep)) throw new Error("Temporary test path outside the expected directory");
        rmSync(dir, { recursive: true, force: true });
    }
});

test("packaging detects source and payload changes even when the timestamps match", () => {
    const dir = mkdtempSync(join(tmpdir(), "lyra-identity-")); directories.push(dir);
    mkdirSync(join(dir, "src"), { recursive: true });
    mkdirSync(join(dir, "assets"), { recursive: true });
    const image = join(dir, "build/inno-staging/app");
    mkdirSync(image, { recursive: true });
    writeFileSync(join(dir, "Cargo.toml"), "[package]\nname='lyraflow'\nversion='2.0.0'\n");
    writeFileSync(join(dir, "Cargo.lock"), "");
    writeFileSync(join(dir, "build.rs"), "fn main() {}\n");
    writeFileSync(join(dir, "src/main.rs"), "fn main() {}");
    writeFileSync(join(image, "LyraFlow.exe"), "test fixture, never executed");
    const helper = resolve("scripts/packaging/AppImageIdentity.ps1").replaceAll("'", "''");
    const root = dir.replaceAll("'", "''");
    const code = `
      $ErrorActionPreference='Stop'; . '${helper}'; Write-AppImageIdentity -Root '${root}';
      Assert-AppImageIdentity -Root '${root}';
      $source=Join-Path '${root}' 'src/main.rs'; $date=(Get-Item $source).LastWriteTime;
      [IO.File]::WriteAllText($source,'changed'); (Get-Item $source).LastWriteTime=$date;
      $rejected=$false; try { Assert-AppImageIdentity -Root '${root}' } catch { $rejected=$true };
      if (-not $rejected) { throw 'Accepted modified source' };
      Write-AppImageIdentity -Root '${root}';
      Add-Content -LiteralPath (Join-Path '${root}' 'build/inno-staging/app/LyraFlow.exe') -Value 'changed';
      $rejected=$false; try { Assert-AppImageIdentity -Root '${root}' } catch { $rejected=$true };
      if (-not $rejected) { throw 'Accepted modified payload' };
    `;
    const result = Bun.spawnSync(["powershell", "-NoProfile", "-Command", code]);
    expect(result.stderr.toString()).toBe("");
    expect(result.exitCode).toBe(0);
});

test("main GPUI branch rejects historical Compose variants cleanly", () => {
    const { taskCommand } = require("../mise-task");
    expect(() => taskCommand("dev", ["isolated", "--plan"])).toThrow("v-kmp");
    expect(() => taskCommand("check", ["desktop"])).toThrow("v-kmp");
    expect(taskCommand("test", ["scripts"])).toEqual(["bun", "test", "scripts/tests"]);
});

test("Aurora verifies the written notes and refuses a mismatched remote response without network", () => {
    const dir = mkdtempSync(join(tmpdir(), "lyra-web-")); directories.push(dir);
    writeFileSync(
        join(dir, "Cargo.toml"),
        '[package]\nname = "lyraflow"\nversion = "2.0.0"\n\n[package.metadata.lyraflow]\nversion_code = 200\n',
    );
    writeFileSync(join(dir, "notes.md"), "Actualización: mañana y canción.");
    writeFileSync(join(dir, "LyraFlow.exe"), "fixture");
    const helper = resolve("scripts/release/AuroraRelease.ps1").replaceAll("'", "''");
    const root = dir.replaceAll("'", "''");
    const code = `
        $ErrorActionPreference='Stop'; . '${helper}';
        $env:LYRAFLOW_AURORA_APP_ID='test-app'; $env:AURORA_RELEASE_TOKEN='test-only';
        $script:written=$null;
        function Invoke-RestMethod { param($Uri,$Method,$ContentType,$Headers,$Body,$TimeoutSec,$MaximumRedirection)
            if ($Method -ne 'Post') { throw 'Unexpected HTTP operation' };
            $script:written=[Text.Encoding]::UTF8.GetString($Body) | ConvertFrom-Json;
        }
        function Get-AuroraReleaseHistory { return @($script:written) }
        $taskArguments=@{Root='${root}';Version='2.0.0';NotesPath=(Join-Path '${root}' 'notes.md');Installer=(Join-Path '${root}' 'LyraFlow.exe')};
        Publish-AuroraRelease @taskArguments;
        if ($script:written.versionCode -ne 200 -or $script:written.releaseNotes -ne (Get-Content $taskArguments.NotesPath -Raw -Encoding UTF8)) { throw 'Payload changed' };
        function Get-AuroraReleaseHistory { $script:written.releaseNotes='mismatch'; return @($script:written) }
        $rejected=$false; try { Publish-AuroraRelease @taskArguments } catch { $rejected=$true };
        if (-not $rejected) { throw 'Remote mismatch accepted' };
    `;
    const result = Bun.spawnSync(["powershell", "-NoProfile", "-Command", code]);
    expect(result.stderr.toString()).toBe("");
    expect(result.exitCode).toBe(0);
});
