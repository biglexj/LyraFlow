import { expect, test } from "bun:test";
import { main, taskCommand } from "../mise-task";

test("GPUI commands target the isolated Cargo package and lockfile", () => {
    for (const [task, cargo] of [["dev", "run"], ["build", "build"], ["check", "check"], ["test", "test"]]) {
        expect(taskCommand(task, ["gpui", "--plan"])).toEqual([
            "cargo", cargo, "--manifest-path", "native/Cargo.toml", "--locked",
            ...(task === "build" ? ["--release"] : []),
        ]);
    }
    expect(() => taskCommand("dev", ["gpui", "--plna"])).toThrow("Opción desconocida");
});

test("GPUI release and publication are rejected before dispatch", async () => {
    for (const task of ["release", "publish"]) {
        await expect(main([task, "gpui", "--plan"])).rejects.toThrow("no tiene instalador ni publicación");
    }
});

test("GPUI plan does not invoke Cargo", async () => {
    expect(await main(["dev", "gpui", "--plan"])).toBe(0);
});
