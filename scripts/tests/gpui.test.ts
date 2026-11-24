import { expect, test } from "bun:test";
import { main, taskCommand } from "../mise-task";

test("GPUI commands target the isolated Cargo package and lockfile directly", () => {
    for (const [task, cargo] of [["dev", "run"], ["build", "build"], ["check", "check"], ["test", "test"]]) {
        expect(taskCommand(task, ["--plan"])).toEqual([
            "cargo", cargo, "--manifest-path", "native/Cargo.toml", "--locked",
            ...(task === "build" ? ["--release"] : []),
        ]);
    }
    expect(() => taskCommand("dev", ["--plna"])).toThrow("Opción desconocida");
    expect(() => taskCommand("dev", ["gpui"])).toThrow("sin alias de tecnología");
});

test("GPUI release and publication are rejected before dispatch", async () => {
    for (const task of ["release", "publish"]) {
        await expect(main([task, "--plan"])).rejects.toThrow("no tiene instalador ni publicación");
    }
});

test("GPUI plan does not invoke Cargo", async () => {
    expect(await main(["dev", "--plan"])).toBe(0);
});
