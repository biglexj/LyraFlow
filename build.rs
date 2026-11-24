use std::{
    env, fs,
    path::{Path, PathBuf},
    process::Command,
};

fn main() {
    if env::var("CARGO_CFG_TARGET_OS").as_deref() != Ok("windows") {
        return;
    }

    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").expect("CARGO_MANIFEST_DIR"));
    let icon_path = manifest_dir
        .join("assets")
        .join("branding")
        .join("icons")
        .join("app_icon.ico");
    println!("cargo:rerun-if-changed={}", icon_path.display());

    if !icon_path.is_file() {
        panic!(
            "No se encontró el icono de aplicación en {}",
            icon_path.display()
        );
    }

    let out_dir = PathBuf::from(env::var("OUT_DIR").expect("OUT_DIR"));
    let rc_path = out_dir.join("lyraflow.rc");
    let res_path = out_dir.join("lyraflow.res");

    let canonical_icon = fs::canonicalize(&icon_path).unwrap_or(icon_path);
    let icon_str = canonical_icon
        .to_string_lossy()
        .strip_prefix(r"\\?\")
        .unwrap_or(&canonical_icon.to_string_lossy())
        .replace('\\', "/");

    // GPUI 0.2.2 carga el recurso de icono con identificador 1 (`PCWSTR(1 as _)`).
    let rc_content = format!("1 ICON \"{icon_str}\"\n");
    fs::write(&rc_path, rc_content).expect("No se pudo escribir el archivo .rc");

    let rc_exe = find_rc_exe().expect(
        "No se encontró rc.exe del SDK de Windows para incrustar el icono de la aplicación.",
    );

    let status = Command::new(&rc_exe)
        .arg("/nologo")
        .arg("/fo")
        .arg(&res_path)
        .arg(&rc_path)
        .status()
        .expect("No se pudo ejecutar rc.exe");
    if !status.success() {
        panic!("rc.exe falló con estado {status}");
    }

    println!("cargo:rustc-link-arg-bins={}", res_path.display());
}

fn find_rc_exe() -> Option<PathBuf> {
    if let Ok(output) = Command::new("where").arg("rc.exe").output()
        && output.status.success()
        && let Some(first) = String::from_utf8_lossy(&output.stdout).lines().next()
    {
        let candidate = PathBuf::from(first.trim());
        if candidate.is_file() {
            return Some(candidate);
        }
    }

    let mut roots = Vec::new();
    if let Ok(sdk_dir) = env::var("WindowsSdkDir") {
        roots.push(PathBuf::from(sdk_dir).join("bin"));
    }
    if let Ok(pf86) = env::var("ProgramFiles(x86)") {
        roots.push(
            PathBuf::from(pf86)
                .join("Windows Kits")
                .join("10")
                .join("bin"),
        );
    }
    roots.push(PathBuf::from(r"C:\Program Files (x86)\Windows Kits\10\bin"));

    let arch = match env::var("CARGO_CFG_TARGET_ARCH").as_deref() {
        Ok("x86") => "x86",
        Ok("aarch64") => "arm64",
        _ => "x64",
    };

    for root in roots {
        if let Some(found) = newest_rc_in(&root, arch) {
            return Some(found);
        }
    }
    None
}

fn newest_rc_in(bin_root: &Path, arch: &str) -> Option<PathBuf> {
    let entries = fs::read_dir(bin_root).ok()?;
    let mut candidates: Vec<PathBuf> = entries
        .filter_map(|entry| entry.ok())
        .map(|entry| entry.path().join(arch).join("rc.exe"))
        .filter(|path| path.is_file())
        .collect();
    candidates.sort();
    candidates.pop()
}
