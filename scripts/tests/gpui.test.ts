import { expect, test } from "bun:test";
import { main, taskCommand } from "../mise-task";

test("LyraFlow commands target the root Cargo package and lockfile directly", () => {
    for (const [task, cargo] of [["dev", "run"], ["build", "build"], ["check", "check"], ["test", "test"]]) {
        expect(taskCommand(task, ["--plan"])).toEqual([
            "cargo", cargo, "--locked",
            ...(task === "build" ? ["--release"] : []),
        ]);
    }
    expect(() => taskCommand("dev", ["--plna"])).toThrow("Opción desconocida");
    expect(() => taskCommand("dev", ["gpui"])).toThrow("sin alias de tecnología");
});

test("release and publish support planning cleanly without side effects", async () => {
    expect(taskCommand("release", ["--plan"])).toEqual([
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", "./scripts/release/build-release.ps1", "-LocalOnly",
    ]);
    expect(await main(["dev", "--plan"])).toBe(0);
    expect(await main(["release", "--plan"])).toBe(0);
    expect(await main(["publish", "--plan"])).toBe(0);
    expect(await main(["publish", "github", "--plan"])).toBe(0);
});
