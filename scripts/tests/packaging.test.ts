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
    mkdirSync(join(dir, "composeApp/src"), { recursive: true });
    const image = join(dir, "composeApp/build/compose/binaries/main/app/LyraFlow");
    mkdirSync(image, { recursive: true });
    writeFileSync(join(dir, "composeApp/src/app.kt"), "first");
    writeFileSync(join(image, "LyraFlow.exe"), "test fixture, never executed");
    const helper = resolve("scripts/packaging/AppImageIdentity.ps1").replaceAll("'", "''");
    const root = dir.replaceAll("'", "''");
    const code = `
      $ErrorActionPreference='Stop'; . '${helper}'; Write-AppImageIdentity -Root '${root}';
      Assert-AppImageIdentity -Root '${root}';
      $source=Join-Path '${root}' 'composeApp/src/app.kt'; $date=(Get-Item $source).LastWriteTime;
      [IO.File]::WriteAllText($source,'changed'); (Get-Item $source).LastWriteTime=$date;
      $rejected=$false; try { Assert-AppImageIdentity -Root '${root}' } catch { $rejected=$true };
      if (-not $rejected) { throw 'Accepted modified source' };
      Write-AppImageIdentity -Root '${root}';
      Add-Content -LiteralPath (Join-Path '${root}' 'composeApp/build/compose/binaries/main/app/LyraFlow/LyraFlow.exe') -Value 'changed';
      $rejected=$false; try { Assert-AppImageIdentity -Root '${root}' } catch { $rejected=$true };
      if (-not $rejected) { throw 'Accepted modified payload' };
    `;
    const result = Bun.spawnSync(["powershell", "-NoProfile", "-Command", code]);
    expect(result.stderr.toString()).toBe("");
    expect(result.exitCode).toBe(0);
});

test("publish plan never dispatches publication and rejects misspelled options", async () => {
    const { main } = await import("../mise-task");
    expect(await main(["publish", "github", "--plan"])).toBe(0);
    await expect(main(["publish", "github", "--plna"])).rejects.toThrow("Opción desconocida");
});

test("Mise tasks resolve to canonical scripts without root launchers", async () => {
    const { taskCommand } = await import("../mise-task");
    expect(taskCommand("release", ["--plan"])).toContain("./scripts/release/build-release.ps1");
    expect(taskCommand("release", ["--plan"])).toContain("-LocalOnly");
    expect(taskCommand("dev", ["isolated", "--plan"])).toEqual(["cmd", "/c", "gradlew.bat", "devIsolated"]);
    expect(taskCommand("check", ["desktop"])).toEqual(["cmd", "/c", "gradlew.bat", ":composeApp:compileKotlinDesktop"]);
    expect(taskCommand("test", ["scripts"])).toEqual(["bun", "test", "scripts/tests"]);
    expect(() => taskCommand("release", ["--plna"])).toThrow("Opción desconocida");
    expect(() => taskCommand("release", ["publish"])).toThrow("mise publish");
});

test("Aurora verifies the written notes and refuses a mismatched remote response without network", () => {
    const dir = mkdtempSync(join(tmpdir(), "lyra-web-")); directories.push(dir);
    writeFileSync(join(dir, "gradle.properties"), "lyraflow.versionCode=119\n");
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
        $taskArguments=@{Root='${root}';Version='1.1.9';NotesPath=(Join-Path '${root}' 'notes.md');Installer=(Join-Path '${root}' 'LyraFlow.exe')};
        Publish-AuroraRelease @taskArguments;
        if ($script:written.versionCode -ne 119 -or $script:written.releaseNotes -ne (Get-Content $taskArguments.NotesPath -Raw -Encoding UTF8)) { throw 'Payload changed' };
        function Get-AuroraReleaseHistory { $script:written.releaseNotes='mismatch'; return @($script:written) }
        $rejected=$false; try { Publish-AuroraRelease @taskArguments } catch { $rejected=$true };
        if (-not $rejected) { throw 'Remote mismatch accepted' };
    `;
    const result = Bun.spawnSync(["powershell", "-NoProfile", "-Command", code]);
    expect(result.stderr.toString()).toBe("");
    expect(result.exitCode).toBe(0);
});
