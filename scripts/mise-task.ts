#!/usr/bin/env bun
import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { resolve, join } from "node:path";

const root = resolve(import.meta.dir, "..");

function readPropertiesVersion(path: string): string {
    const content = readFileSync(path, "utf-8");
    const m = content.match(/^lyraflow\.versionName\s*=\s*(\d+\.\d+\.\d+)/m);
    if (!m) throw new Error("No se pudo leer versionName en " + path);
    return m[1];
}

function sha256(filePath: string): string {
    const buffer = readFileSync(filePath);
    return createHash("sha256").update(buffer).digest("hex");
}

function runCmdSync(cmd: string[], cwd = root): { stdout: string; stderr: string; code: number } {
    const res = Bun.spawnSync(cmd, { cwd, stdout: "pipe", stderr: "pipe" });
    return {
        stdout: res.stdout.toString().trim(),
        stderr: res.stderr.toString().trim(),
        code: res.exitCode ?? 0,
    };
}

const appVersion = readPropertiesVersion(join(root, "gradle.properties"));
const appName = "LyraFlow";
const repository = "biglexj/LyraFlow";

const help = `LyraFlow: mise <dev|build|check|test|release|publish> [opciones]
  dev      Ejecutar en modo desarrollo (:composeApp:run).
  build    Generar imagen de escritorio (buildDesktop), sin instalador.
  check    Compilar escritorio y Android; desktop verifica solo escritorio.
  test     Pruebas de escritorio; scripts verifica las herramientas de tareas.
  release  Generar instalador Inno Setup en release/.
  publish  [github|all] Publicar en GitHub y sincronizar ficha en Aurora.
  --plan   Mostrar los comandos o el plan sin ejecutar acciones ni escribir.
  --check  Para publish: verificar requisitos locales y remotos sin escribir.

Ejemplos:
  mise dev
  mise dev gpui       Demo nativa Rust + GPUI (también build, check y test).
  mise dev --plan
  mise build --plan
  mise release --plan
  mise publish --plan
  mise publish github --plan
  mise publish --check
`;

async function handlePublish(args: string[]): Promise<number> {
    for (const arg of args) {
        if (arg.startsWith("-") && !["--plan", "--check"].includes(arg)) throw new Error(`Opción desconocida: ${arg}`);
    }
    const isPlan = args.includes("--plan");
    const isCheck = args.includes("--check");
    const positional = args.filter((arg) => !arg.startsWith("-"));
    const channel = positional[0] ?? "all";

    if (!["all", "github"].includes(channel)) {
        throw new Error(`Canal desconocido: ${channel}. Usa 'github' o 'all'.`);
    }

    const effectiveChannels = channel === "github" ? ["GitHub Releases"] : ["GitHub Releases", "Aurora Web (biglexj.com)"];

    console.log(`\n=== Publicación: ${appName} v${appVersion} ===`);
    console.log(`Repositorio : ${repository}`);
    console.log(`Canales     : ${effectiveChannels.join(" + ")}`);

    if (isPlan) {
        console.log("\n[Plan de publicación]");
        console.log("1. Verificar preview/main limpia, avance de main y versión no publicada.");
        console.log("2. Ejecutar pruebas, imagen vigente, Inno Setup, firma y SHA-256 mediante el pipeline oficial.");
        console.log("3. Validar RELEASE_MESSAGE.md (notas de versión públicas sin secretos).");
        console.log(`4. Publicar tag v${appVersion} y avance de main; verificar la descarga pública.`);
        if (channel === "all") {
            console.log("5. Sincronizar metadatos y enlace de descarga en Aurora (https://www.biglexj.com).");
        } else {
            console.log("5. Sincronización con Aurora omitida (canal explícito: github).");
        }
        console.log("\n(--plan completado: no se realizaron cambios ni consultas de red)");
        return 0;
    }

    if (isCheck) {
        console.log("\n[Comprobación de preparación (--check)]");
        let hasErrors = false;

        // 1. Git status
        const gitStatus = runCmdSync(["git", "status", "--porcelain"]);
        if (gitStatus.stdout) {
            console.warn("❌ Hay cambios sin confirmar. Revisa y confirma antes de publicar.");
            hasErrors = true;
        } else {
            console.log("✅ Árbol Git limpio.");
        }

        // 2. Git tag
        const tagCheck = runCmdSync(["git", "tag", "-l", `v${appVersion}`]);
        if (tagCheck.stdout.includes(`v${appVersion}`)) {
            console.warn(`❌ El tag v${appVersion} ya existe localmente.`);
            hasErrors = true;
        } else {
            console.log(`✅ Tag v${appVersion} libre localmente.`);
        }

        // 3. Artefactos en release/
        const releaseDir = join(root, "release");
        let foundInstaller: string | null = null;
        if (existsSync(releaseDir)) {
            const files = readdirSync(releaseDir);
            foundInstaller = files.includes(`LyraFlow-Windows-${appVersion}.exe`) ? `LyraFlow-Windows-${appVersion}.exe` : null;
        }

        if (foundInstaller) {
            const fullPath = join(releaseDir, foundInstaller);
            const st = statSync(fullPath);
            const hash = sha256(fullPath);
            const checksumFile = join(releaseDir, "SHA256SUMS.txt");
            if (!existsSync(checksumFile) || readFileSync(checksumFile, "utf8").trim() !== `${hash}  ${foundInstaller}`) {
                console.error("❌ El SHA-256 no coincide con SHA256SUMS.txt.");
                hasErrors = true;
            }
            console.log(`✅ Instalador presente: ${foundInstaller} (${(st.size / 1024 / 1024).toFixed(2)} MB)`);
            console.log(`   SHA-256: ${hash}`);
        } else {
            console.warn(`❌ Instalador no encontrado en release/LyraFlow-*.exe. Ejecuta 'mise release' antes de publicar.`);
            hasErrors = true;
        }

        // 4. Notas de versión
        const notesPath = join(root, "RELEASE_MESSAGE.md");
        if (existsSync(notesPath)) {
            console.log("✅ RELEASE_MESSAGE.md presente.");
        } else {
            console.warn("❌ RELEASE_MESSAGE.md no encontrado en la raíz.");
            hasErrors = true;
        }

        const branch = runCmdSync(["git", "branch", "--show-current"]);
        if (branch.code !== 0 || !["preview", "main"].includes(branch.stdout)) hasErrors = true;
        if (gitStatus.code !== 0 || tagCheck.code !== 0) hasErrors = true;
        const remoteTags = runCmdSync(["git", "ls-remote", "--tags", "origin", `refs/tags/v${appVersion}`]);
        if (remoteTags.code !== 0 || remoteTags.stdout) {
            console.error("❌ No se pudo confirmar que el tag remoto esté libre."); hasErrors = true;
        }
        const releases = runCmdSync(["gh", "api", `repos/${repository}/releases?per_page=100`, "--jq", ".[].tag_name"]);
        if (releases.code !== 0 || releases.stdout.split(/\r?\n/).includes(`v${appVersion}`)) {
            console.error("❌ No se pudo confirmar que la versión no esté publicada."); hasErrors = true;
        }
        const image = runCmdSync(["powershell", "-NoProfile", "-Command",
            ". ./scripts/packaging/AppImageIdentity.ps1; Assert-AppImageIdentity -Root (Get-Location).Path"]);
        if (image.code !== 0) {
            console.error("❌ La imagen no tiene huellas vigentes. Ejecuta mise release."); hasErrors = true;
        }
        if (channel === "all") {
            const web = runCmdSync(["powershell", "-NoProfile", "-Command",
                `. ./scripts/release/AuroraRelease.ps1; Assert-AuroraReleasePreflight -Root (Get-Location).Path -Version '${appVersion}'`]);
            if (web.code !== 0) {
                console.error("❌ Aurora no está preparada: comprueba LYRAFLOW_AURORA_APP_ID, AURORA_RELEASE_TOKEN y el historial remoto."); hasErrors = true;
            }
        }
        console.log("\n(--check completado: solo lectura; los cambios locales requieren revisión)");
        return hasErrors ? 1 : 0;
    }

    const child = Bun.spawn(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", "./scripts/release/build-release.ps1", "-Publish", "-Channel", channel],
        { cwd: root, stdin: "inherit", stdout: "inherit", stderr: "inherit" });
    return (await child.exited) ?? 1;
}

export function taskCommand(action: string, args: string[]): string[] {
    for (const arg of args) {
        if (arg.startsWith("-") && arg !== "--plan") throw new Error(`Opción desconocida: ${arg}`);
    }
    const positional = args.filter((arg) => !arg.startsWith("-"));
    if (positional.length > 1) throw new Error("Se admite una sola variante por tarea.");
    const variant = positional[0]?.toLowerCase();
    if (variant === "gpui") {
        const manifest = ["--manifest-path", "native/Cargo.toml"];
        if (action === "dev") return ["cargo", "run", ...manifest, "--locked"];
        if (action === "build") return ["cargo", "build", ...manifest, "--locked", "--release"];
        if (action === "check") return ["cargo", "check", ...manifest, "--locked"];
        if (action === "test") return ["cargo", "test", ...manifest, "--locked"];
        throw new Error("La demo GPUI solo admite dev, build, check y test. No tiene instalador ni publicación.");
    }
    const gradle = (task: string) => ["cmd", "/c", "gradlew.bat", task];
    if (action === "dev") {
        if (variant && !["android", "isolated"].includes(variant)) throw new Error("Usa dev, dev android o dev isolated.");
        return gradle(variant === "android" ? "devAndroid" : variant === "isolated" ? "devIsolated" : "dev");
    }
    if (action === "build") {
        if (variant && variant !== "android") throw new Error("Usa build o build android.");
        return gradle(variant === "android" ? ":composeApp:assembleRelease" : "buildDesktop");
    }
    if (action === "check") {
        if (variant && variant !== "desktop") throw new Error("Usa check o check desktop.");
        return gradle(variant === "desktop" ? ":composeApp:compileKotlinDesktop" : "check");
    }
    if (action === "test") {
        if (variant && variant !== "scripts") throw new Error("Usa test o test scripts.");
        return variant === "scripts" ? ["bun", "test", "scripts/tests"] : gradle(":composeApp:desktopTest");
    }
    if (action === "release") {
        if (variant) throw new Error("release no admite variantes; publica con mise publish.");
        return ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "./scripts/release/build-release.ps1", "-LocalOnly"];
    }
    throw new Error(`Acción desconocida: ${action}. Usa dev, build, check, test, release o publish.`);
}

export async function main(args = Bun.argv.slice(2)): Promise<number> {
    const [action, ...rest] = args;
    if (!action || action === "--help" || rest.includes("--help")) {
        console.log(help);
        return 0;
    }

    if (["release", "publish"].includes(action) && rest.some(arg => arg.toLowerCase() === "gpui")) {
        throw new Error("La demo GPUI no tiene instalador ni publicación. Usa mise build gpui.");
    }

    if (action === "publish") {
        return handlePublish(rest);
    }

    const cmd = taskCommand(action, rest);
    console.log(`${action} ${appName}\nDirectorio: ${root}\nComando: ${cmd.join(" ")}`);
    if (rest.includes("--plan")) return 0;
    const child = Bun.spawn(cmd, { cwd: root, stdin: "inherit", stdout: "inherit", stderr: "inherit" });
    return (await child.exited) ?? 1;

}

if (import.meta.main) {
    const args = process.env.MISE_TASK_NAME
        ? [process.env.MISE_TASK_NAME, ...Bun.argv.slice(2)]
        : Bun.argv.slice(2);
    try {
        process.exit(await main(args));
    } catch (err: any) {
        console.error(err?.message ?? err);
        process.exit(1);
    }
}
