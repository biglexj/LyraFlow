#!/usr/bin/env bun
import { publishRelease } from "../../Scripts/releases/aurora-publisher.ts";
import { readFileSync } from "node:fs";
import { resolve, join } from "node:path";

const root = resolve(import.meta.dir, "..");

function readCargoManifest(path: string): { version: string; versionCode: number } {
    const content = readFileSync(path, "utf-8");
    const vMatch = content.match(/^\[package\][\s\S]*?^version\s*=\s*"(\d+\.\d+\.\d+)"/m);
    if (!vMatch) throw new Error("No se pudo leer version en " + path);
    const cMatch = content.match(/^version_code\s*=\s*(\d+)/m);
    if (!cMatch) throw new Error("No se pudo leer version_code en " + path);
    return { version: vMatch[1], versionCode: Number(cMatch[1]) };
}

const { version: appVersion, versionCode: appVersionCode } = readCargoManifest(join(root, "Cargo.toml"));
const appName = "LyraFlow";
const repository = "biglexj/LyraFlow";

const help = `LyraFlow (Rust + GPUI v${appVersion}): mise <dev|build|check|test|release|publish> [opciones]
  dev      Ejecutar la aplicación nativa en desarrollo (cargo run --locked).
  build    Compilar binario nativo en release (cargo build --locked --release).
  check    Verificar compilación Rust + GPUI (cargo check --locked).
  test     Pruebas unitarias de LyraFlow; 'scripts' verifica las herramientas de tareas.
  release  Generar instalador oficial Inno Setup en release/ sin publicar.
  publish  Publicar en GitHub Releases y sincronizar ficha en Aurora (--plan y --check sin escritura).
  --plan   Mostrar los comandos o el plan sin ejecutar acciones ni escribir.

Nota de ramas: main aloja la edición nativa Rust + GPUI (v2.0.0); la edición histórica Compose (v1.1.9) se conserva en 'v-kmp'.
`;

export function taskCommand(action: string, args: string[]): string[] {
    for (const arg of args) {
        if (arg.startsWith("-") && arg !== "--plan") throw new Error(`Opción desconocida: ${arg}`);
    }
    const positional = args.filter((arg) => !arg.startsWith("-"));
    if (positional.length > 1) throw new Error("Se admite una sola variante por tarea.");
    const variant = positional[0]?.toLowerCase();
    if (variant === "gpui") {
        throw new Error("En main se ejecuta directamente con 'mise dev' sin alias de tecnología.");
    }
    if (["compose", "android", "isolated", "desktop"].includes(variant ?? "")) {
        throw new Error("La edición Compose Multiplatform (v1.1.9) se conserva en la rama histórica 'v-kmp'.");
    }
    if (action === "test" && variant === "scripts") {
        return ["bun", "test", "scripts/tests"];
    }
    if (variant) {
        throw new Error(`Argumento no admitido en LyraFlow: ${variant}.`);
    }
    if (action === "dev") return ["cargo", "run", "--locked"];
    if (action === "build") return ["cargo", "build", "--locked", "--release"];
    if (action === "check") return ["cargo", "check", "--locked"];
    if (action === "test") return ["cargo", "test", "--locked"];
    if (action === "release") {
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

    const isPlan = rest.includes("--plan");

    if (action === "publish") {
        for (const arg of rest) {
            if (arg.startsWith("-") && !["--plan", "--check"].includes(arg)) throw new Error(`Opción desconocida: ${arg}`);
        }
        return publishRelease({
            slug: "lyraflow",
            appName,
            repository,
            version: appVersion,
            versionCode: appVersionCode,
            projectRoot: root,
            primaryAssetPattern: new RegExp(`^LyraFlow-Windows-${appVersion.replaceAll(".", "\\.")}\\.exe$`, "i"),
            build: async (planMode) => {
                const psArgs = ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "./scripts/release/build-release.ps1", "-LocalOnly"];
                if (planMode) return 0;
                const child = Bun.spawn(psArgs, { cwd: root, stdin: "inherit", stdout: "inherit", stderr: "inherit" });
                return (await child.exited) ?? 0;
            },
        }, rest);
    }

    const cmd = taskCommand(action, rest);
    console.log(`${action} ${appName}\nDirectorio: ${root}\nComando: ${cmd.join(" ")}`);
    if (isPlan) return 0;

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
