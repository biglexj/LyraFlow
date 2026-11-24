//! Integración nativa de plataforma (atajos globales, inyección de texto y arranque en Windows).

use std::collections::BTreeSet;

#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Debug)]
pub enum ShortcutModifier {
    Windows,
    Control,
    Alt,
    Shift,
}

impl ShortcutModifier {
    pub fn label(self) -> &'static str {
        match self {
            ShortcutModifier::Windows => "Windows",
            ShortcutModifier::Control => "Ctrl",
            ShortcutModifier::Alt => "Alt",
            ShortcutModifier::Shift => "Shift",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            ShortcutModifier::Windows => "Windows",
            ShortcutModifier::Control => "Control",
            ShortcutModifier::Alt => "Alt",
            ShortcutModifier::Shift => "Shift",
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value.trim() {
            "Windows" => Some(ShortcutModifier::Windows),
            "Control" | "Ctrl" => Some(ShortcutModifier::Control),
            "Alt" => Some(ShortcutModifier::Alt),
            "Shift" => Some(ShortcutModifier::Shift),
            _ => None,
        }
    }

    #[cfg(target_os = "windows")]
    pub fn win_flag(self) -> u32 {
        match self {
            ShortcutModifier::Control => 0x0002,
            ShortcutModifier::Alt => 0x0001,
            ShortcutModifier::Shift => 0x0004,
            ShortcutModifier::Windows => 0x0008,
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ShortcutKey {
    Space,
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
}

impl ShortcutKey {
    pub fn label(self) -> &'static str {
        match self {
            ShortcutKey::Space => "Espacio",
            ShortcutKey::A => "A",
            ShortcutKey::B => "B",
            ShortcutKey::C => "C",
            ShortcutKey::D => "D",
            ShortcutKey::E => "E",
            ShortcutKey::F => "F",
            ShortcutKey::G => "G",
            ShortcutKey::H => "H",
            ShortcutKey::I => "I",
            ShortcutKey::J => "J",
            ShortcutKey::K => "K",
            ShortcutKey::L => "L",
            ShortcutKey::M => "M",
            ShortcutKey::N => "N",
            ShortcutKey::O => "O",
            ShortcutKey::P => "P",
            ShortcutKey::Q => "Q",
            ShortcutKey::R => "R",
            ShortcutKey::S => "S",
            ShortcutKey::T => "T",
            ShortcutKey::U => "U",
            ShortcutKey::V => "V",
            ShortcutKey::W => "W",
            ShortcutKey::X => "X",
            ShortcutKey::Y => "Y",
            ShortcutKey::Z => "Z",
            ShortcutKey::F1 => "F1",
            ShortcutKey::F2 => "F2",
            ShortcutKey::F3 => "F3",
            ShortcutKey::F4 => "F4",
            ShortcutKey::F5 => "F5",
            ShortcutKey::F6 => "F6",
            ShortcutKey::F7 => "F7",
            ShortcutKey::F8 => "F8",
            ShortcutKey::F9 => "F9",
            ShortcutKey::F10 => "F10",
            ShortcutKey::F11 => "F11",
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            ShortcutKey::Space => "Space",
            other => other.label(),
        }
    }

    pub fn from_id(value: &str) -> Option<Self> {
        match value.trim() {
            "Space" | "space" | " " => Some(ShortcutKey::Space),
            "A" | "a" => Some(ShortcutKey::A),
            "B" | "b" => Some(ShortcutKey::B),
            "C" | "c" => Some(ShortcutKey::C),
            "D" | "d" => Some(ShortcutKey::D),
            "E" | "e" => Some(ShortcutKey::E),
            "F" | "f" => Some(ShortcutKey::F),
            "G" | "g" => Some(ShortcutKey::G),
            "H" | "h" => Some(ShortcutKey::H),
            "I" | "i" => Some(ShortcutKey::I),
            "J" | "j" => Some(ShortcutKey::J),
            "K" | "k" => Some(ShortcutKey::K),
            "L" | "l" => Some(ShortcutKey::L),
            "M" | "m" => Some(ShortcutKey::M),
            "N" | "n" => Some(ShortcutKey::N),
            "O" | "o" => Some(ShortcutKey::O),
            "P" | "p" => Some(ShortcutKey::P),
            "Q" | "q" => Some(ShortcutKey::Q),
            "R" | "r" => Some(ShortcutKey::R),
            "S" | "s" => Some(ShortcutKey::S),
            "T" | "t" => Some(ShortcutKey::T),
            "U" | "u" => Some(ShortcutKey::U),
            "V" | "v" => Some(ShortcutKey::V),
            "W" | "w" => Some(ShortcutKey::W),
            "X" | "x" => Some(ShortcutKey::X),
            "Y" | "y" => Some(ShortcutKey::Y),
            "Z" | "z" => Some(ShortcutKey::Z),
            "F1" | "f1" => Some(ShortcutKey::F1),
            "F2" | "f2" => Some(ShortcutKey::F2),
            "F3" | "f3" => Some(ShortcutKey::F3),
            "F4" | "f4" => Some(ShortcutKey::F4),
            "F5" | "f5" => Some(ShortcutKey::F5),
            "F6" | "f6" => Some(ShortcutKey::F6),
            "F7" | "f7" => Some(ShortcutKey::F7),
            "F8" | "f8" => Some(ShortcutKey::F8),
            "F9" | "f9" => Some(ShortcutKey::F9),
            "F10" | "f10" => Some(ShortcutKey::F10),
            "F11" | "f11" => Some(ShortcutKey::F11),
            _ => None,
        }
    }

    #[cfg(target_os = "windows")]
    pub fn win_vk(self) -> u32 {
        match self {
            ShortcutKey::Space => 0x20,
            ShortcutKey::A => 0x41,
            ShortcutKey::B => 0x42,
            ShortcutKey::C => 0x43,
            ShortcutKey::D => 0x44,
            ShortcutKey::E => 0x45,
            ShortcutKey::F => 0x46,
            ShortcutKey::G => 0x47,
            ShortcutKey::H => 0x48,
            ShortcutKey::I => 0x49,
            ShortcutKey::J => 0x4A,
            ShortcutKey::K => 0x4B,
            ShortcutKey::L => 0x4C,
            ShortcutKey::M => 0x4D,
            ShortcutKey::N => 0x4E,
            ShortcutKey::O => 0x4F,
            ShortcutKey::P => 0x50,
            ShortcutKey::Q => 0x51,
            ShortcutKey::R => 0x52,
            ShortcutKey::S => 0x53,
            ShortcutKey::T => 0x54,
            ShortcutKey::U => 0x55,
            ShortcutKey::V => 0x56,
            ShortcutKey::W => 0x57,
            ShortcutKey::X => 0x58,
            ShortcutKey::Y => 0x59,
            ShortcutKey::Z => 0x5A,
            ShortcutKey::F1 => 0x70,
            ShortcutKey::F2 => 0x71,
            ShortcutKey::F3 => 0x72,
            ShortcutKey::F4 => 0x73,
            ShortcutKey::F5 => 0x74,
            ShortcutKey::F6 => 0x75,
            ShortcutKey::F7 => 0x76,
            ShortcutKey::F8 => 0x77,
            ShortcutKey::F9 => 0x78,
            ShortcutKey::F10 => 0x79,
            ShortcutKey::F11 => 0x7A,
        }
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct KeyboardShortcut {
    pub modifiers: BTreeSet<ShortcutModifier>,
    pub key: ShortcutKey,
}

impl KeyboardShortcut {
    pub fn default_primary() -> Self {
        Self {
            modifiers: BTreeSet::from([ShortcutModifier::Control]),
            key: ShortcutKey::Space,
        }
    }

    pub fn default_cancel() -> Self {
        Self {
            modifiers: BTreeSet::from([ShortcutModifier::Control, ShortcutModifier::Alt]),
            key: ShortcutKey::Space,
        }
    }

    pub fn label(&self) -> String {
        let mut parts: Vec<&str> = self.modifiers.iter().map(|m| m.label()).collect();
        parts.push(self.key.label());
        parts.join(" + ")
    }

    pub fn validation_error(&self) -> Option<&'static str> {
        if !(1..=2).contains(&self.modifiers.len()) {
            return Some("El atajo debe contener 2 o 3 teclas.");
        }
        if self.modifiers.len() == 1
            && self.modifiers.contains(&ShortcutModifier::Windows)
            && self.key == ShortcutKey::Space
        {
            return Some("Windows + Espacio está reservado para cambiar el idioma del teclado.");
        }
        None
    }

    pub fn conflicts_with(&self, other: &Self) -> bool {
        self.modifiers == other.modifiers && self.key == other.key
    }

    pub fn serialize(&self) -> String {
        let mods: Vec<&str> = self.modifiers.iter().map(|m| m.id()).collect();
        format!("{}+{}", mods.join(","), self.key.id())
    }

    pub fn parse(raw: &str) -> Option<Self> {
        let (mods_str, key_str) = raw.split_once('+')?;
        let key = ShortcutKey::from_id(key_str)?;
        let modifiers: BTreeSet<ShortcutModifier> = mods_str
            .split(',')
            .filter_map(ShortcutModifier::from_id)
            .collect();
        let candidate = Self { modifiers, key };
        if candidate.validation_error().is_some() {
            None
        } else {
            Some(candidate)
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum HotkeySignal {
    ToggleRecord,
    CancelRecord,
}

#[cfg(target_os = "windows")]
pub struct WindowsGlobalShortcut {
    worker: Option<std::thread::JoinHandle<()>>,
    thread_id: u32,
    pub status: String,
}

#[cfg(target_os = "windows")]
impl WindowsGlobalShortcut {
    pub fn start(
        primary: KeyboardShortcut,
        cancel: KeyboardShortcut,
        sender: std::sync::mpsc::Sender<HotkeySignal>,
    ) -> Self {
        use std::sync::mpsc;
        use windows::Win32::System::Threading::GetCurrentThreadId;
        use windows::Win32::UI::Input::KeyboardAndMouse::{
            HOT_KEY_MODIFIERS, MOD_NOREPEAT, RegisterHotKey, UnregisterHotKey,
        };
        use windows::Win32::UI::WindowsAndMessaging::{GetMessageW, MSG, WM_HOTKEY};

        const PRIMARY_ID: i32 = 0x4C59;
        const CANCEL_ID: i32 = 0x4C5A;

        let (ready_tx, ready_rx) = mpsc::channel::<(u32, String)>();
        let worker = std::thread::Builder::new()
            .name("lyraflow-hotkey".into())
            .spawn(move || {
                let tid = unsafe { GetCurrentThreadId() };
                let mod_flags = |sc: &KeyboardShortcut| -> HOT_KEY_MODIFIERS {
                    let mut flags = MOD_NOREPEAT.0;
                    for m in &sc.modifiers {
                        flags |= m.win_flag();
                    }
                    HOT_KEY_MODIFIERS(flags)
                };

                let ok_primary = unsafe {
                    RegisterHotKey(None, PRIMARY_ID, mod_flags(&primary), primary.key.win_vk())
                        .is_ok()
                };
                let ok_cancel = unsafe {
                    RegisterHotKey(None, CANCEL_ID, mod_flags(&cancel), cancel.key.win_vk()).is_ok()
                };

                let mut active = Vec::new();
                let mut failed = Vec::new();
                if ok_primary {
                    active.push(primary.label());
                } else {
                    failed.push(primary.label());
                }
                if ok_cancel {
                    active.push(cancel.label());
                } else {
                    failed.push(cancel.label());
                }

                let mut status = if active.is_empty() {
                    "Sin atajos".to_string()
                } else {
                    active.join(" / ")
                };
                if !failed.is_empty() {
                    status.push_str(" · no disponible: ");
                    status.push_str(&failed.join(", "));
                } else {
                    status.push_str(" activo");
                }

                let _ = ready_tx.send((tid, status));

                if !ok_primary && !ok_cancel {
                    return;
                }

                let mut msg = MSG::default();
                unsafe {
                    while GetMessageW(&mut msg, None, 0, 0).as_bool() {
                        if msg.message == WM_HOTKEY {
                            match msg.wParam.0 as i32 {
                                PRIMARY_ID => {
                                    let _ = sender.send(HotkeySignal::ToggleRecord);
                                }
                                CANCEL_ID => {
                                    let _ = sender.send(HotkeySignal::CancelRecord);
                                }
                                _ => {}
                            }
                        }
                    }
                    if ok_primary {
                        let _ = UnregisterHotKey(None, PRIMARY_ID);
                    }
                    if ok_cancel {
                        let _ = UnregisterHotKey(None, CANCEL_ID);
                    }
                }
            })
            .ok();

        let (thread_id, status) = ready_rx
            .recv_timeout(std::time::Duration::from_secs(2))
            .unwrap_or((0, "Pendiente".into()));

        Self {
            worker,
            thread_id,
            status,
        }
    }
}

#[cfg(target_os = "windows")]
impl Drop for WindowsGlobalShortcut {
    fn drop(&mut self) {
        use windows::Win32::Foundation::{LPARAM, WPARAM};
        use windows::Win32::UI::WindowsAndMessaging::{PostThreadMessageW, WM_QUIT};

        if self.thread_id != 0 {
            unsafe {
                let _ = PostThreadMessageW(self.thread_id, WM_QUIT, WPARAM(0), LPARAM(0));
            }
        }
        if let Some(handle) = self.worker.take() {
            let _ = handle.join();
        }
    }
}

#[cfg(not(target_os = "windows"))]
pub struct WindowsGlobalShortcut {
    pub status: String,
}

#[cfg(not(target_os = "windows"))]
impl WindowsGlobalShortcut {
    pub fn start(
        primary: KeyboardShortcut,
        _cancel: KeyboardShortcut,
        _sender: std::sync::mpsc::Sender<HotkeySignal>,
    ) -> Self {
        Self {
            status: format!("{} (solo ventana)", primary.label()),
        }
    }
}

#[derive(Default)]
pub struct DesktopTextInjector {
    #[cfg(target_os = "windows")]
    target_hwnd: isize,
}

impl DesktopTextInjector {
    #[cfg(target_os = "windows")]
    pub fn remember_foreground_target(&mut self) {
        use windows::Win32::UI::WindowsAndMessaging::{GetForegroundWindow, GetWindowTextW};
        unsafe {
            let hwnd = GetForegroundWindow();
            if hwnd.is_invalid() {
                return;
            }
            let mut buf = [0u16; 256];
            let len = GetWindowTextW(hwnd, &mut buf);
            let title = String::from_utf16_lossy(&buf[..len as usize]);
            if title != "LyraFlow" {
                self.target_hwnd = hwnd.0 as isize;
            }
        }
    }

    #[cfg(not(target_os = "windows"))]
    pub fn remember_foreground_target(&mut self) {}

    #[cfg(target_os = "windows")]
    pub fn inject(&self, text: &str) {
        if text.trim().is_empty() {
            return;
        }
        if set_windows_clipboard_text(text).is_err() {
            return;
        }
        use windows::Win32::Foundation::HWND;
        use windows::Win32::UI::Input::KeyboardAndMouse::{
            INPUT, INPUT_0, INPUT_KEYBOARD, KEYBDINPUT, KEYEVENTF_KEYUP, SendInput, VK_CONTROL,
            VK_V,
        };
        use windows::Win32::UI::WindowsAndMessaging::SetForegroundWindow;

        if self.target_hwnd != 0 {
            unsafe {
                let _ = SetForegroundWindow(HWND(self.target_hwnd as _));
            }
            std::thread::sleep(std::time::Duration::from_millis(120));
        }

        let make_key = |vk: u16, up: bool| INPUT {
            r#type: INPUT_KEYBOARD,
            Anonymous: INPUT_0 {
                ki: KEYBDINPUT {
                    wVk: windows::Win32::UI::Input::KeyboardAndMouse::VIRTUAL_KEY(vk),
                    dwFlags: if up {
                        KEYEVENTF_KEYUP
                    } else {
                        Default::default()
                    },
                    ..Default::default()
                },
            },
        };
        let inputs = [
            make_key(VK_CONTROL.0, false),
            make_key(VK_V.0, false),
            make_key(VK_V.0, true),
            make_key(VK_CONTROL.0, true),
        ];
        unsafe {
            SendInput(&inputs, std::mem::size_of::<INPUT>() as i32);
        }
    }

    #[cfg(not(target_os = "windows"))]
    pub fn inject(&self, _text: &str) {}
}

#[cfg(target_os = "windows")]
fn set_windows_clipboard_text(text: &str) -> Result<(), String> {
    use windows::Win32::Foundation::{HANDLE, HGLOBAL};
    use windows::Win32::System::DataExchange::{
        CloseClipboard, EmptyClipboard, OpenClipboard, SetClipboardData,
    };
    use windows::Win32::System::Memory::{GMEM_MOVEABLE, GlobalAlloc, GlobalLock, GlobalUnlock};

    const CF_UNICODETEXT: u32 = 13;
    let utf16: Vec<u16> = text.encode_utf16().chain(std::iter::once(0)).collect();
    let byte_len = utf16.len() * std::mem::size_of::<u16>();

    unsafe {
        OpenClipboard(None).map_err(|e| format!("No se pudo abrir portapapeles: {e}"))?;
        let _ = EmptyClipboard();
        let hmem: HGLOBAL = GlobalAlloc(GMEM_MOVEABLE, byte_len).map_err(|e| {
            let _ = CloseClipboard();
            format!("No se pudo asignar memoria de portapapeles: {e}")
        })?;
        let ptr = GlobalLock(hmem) as *mut u16;
        if ptr.is_null() {
            let _ = CloseClipboard();
            return Err("No se pudo bloquear memoria de portapapeles.".into());
        }
        std::ptr::copy_nonoverlapping(utf16.as_ptr(), ptr, utf16.len());
        let _ = GlobalUnlock(hmem);
        let result = SetClipboardData(CF_UNICODETEXT, Some(HANDLE(hmem.0)));
        let _ = CloseClipboard();
        result
            .map(|_| ())
            .map_err(|e| format!("SetClipboardData falló: {e}"))
    }
}

#[cfg(target_os = "windows")]
pub fn set_auto_start_enabled(enabled: bool) {
    use windows::Win32::System::Registry::{
        HKEY_CURRENT_USER, KEY_SET_VALUE, REG_OPTION_NON_VOLATILE, REG_SZ, RegCloseKey,
        RegCreateKeyExW, RegDeleteValueW, RegOpenKeyExW, RegSetValueExW,
    };
    use windows::core::PCWSTR;

    let subkey: Vec<u16> = "Software\\Microsoft\\Windows\\CurrentVersion\\Run\0"
        .encode_utf16()
        .collect();
    let value_name: Vec<u16> = "LyraFlow\0".encode_utf16().collect();

    unsafe {
        if enabled {
            let Ok(exe) = std::env::current_exe() else {
                return;
            };
            let exe_str = exe.to_string_lossy();
            if !exe_str.to_ascii_lowercase().ends_with(".exe") {
                return;
            }
            let cmd = format!("\"{exe_str}\" --minimized\0");
            let cmd_u16: Vec<u16> = cmd.encode_utf16().collect();
            let mut hkey = Default::default();
            if RegCreateKeyExW(
                HKEY_CURRENT_USER,
                PCWSTR(subkey.as_ptr()),
                Some(0),
                None,
                REG_OPTION_NON_VOLATILE,
                KEY_SET_VALUE,
                None,
                &mut hkey,
                None,
            )
            .is_ok()
            {
                let bytes =
                    std::slice::from_raw_parts(cmd_u16.as_ptr() as *const u8, cmd_u16.len() * 2);
                let _ = RegSetValueExW(
                    hkey,
                    PCWSTR(value_name.as_ptr()),
                    Some(0),
                    REG_SZ,
                    Some(bytes),
                );
                let _ = RegCloseKey(hkey);
            }
        } else {
            let mut hkey = Default::default();
            if RegOpenKeyExW(
                HKEY_CURRENT_USER,
                PCWSTR(subkey.as_ptr()),
                Some(0),
                KEY_SET_VALUE,
                &mut hkey,
            )
            .is_ok()
            {
                let _ = RegDeleteValueW(hkey, PCWSTR(value_name.as_ptr()));
                let _ = RegCloseKey(hkey);
            }
        }
    }
}

#[cfg(not(target_os = "windows"))]
pub fn set_auto_start_enabled(_enabled: bool) {}

#[cfg(target_os = "windows")]
pub fn apply_titlebar_icon() {
    use std::sync::OnceLock;
    use windows::Win32::Foundation::{HWND, LPARAM, WPARAM};
    use windows::Win32::UI::WindowsAndMessaging::{
        CreateIconFromResourceEx, EnumWindows, GetSystemMetrics, GetWindowTextW,
        GetWindowThreadProcessId, HICON, ICON_SMALL, LR_DEFAULTCOLOR, SM_CXSMICON, SM_CYSMICON,
        SendMessageW, WM_SETICON,
    };
    use windows::core::BOOL;

    static TITLEBAR_HICON: OnceLock<isize> = OnceLock::new();
    let hicon_raw = *TITLEBAR_HICON.get_or_init(|| {
        const BADGE_PNG: &[u8] = include_bytes!("../assets/branding/titlebar-icon-badge.png");
        unsafe {
            let cx = GetSystemMetrics(SM_CXSMICON).max(16);
            let cy = GetSystemMetrics(SM_CYSMICON).max(16);
            CreateIconFromResourceEx(BADGE_PNG, true, 0x0003_0000, cx, cy, LR_DEFAULTCOLOR)
                .map(|icon: HICON| icon.0 as isize)
                .unwrap_or(0)
        }
    });

    if hicon_raw == 0 {
        return;
    }

    struct EnumState {
        pid: u32,
        hicon: isize,
    }

    unsafe extern "system" fn enum_proc(hwnd: HWND, lparam: LPARAM) -> BOOL {
        unsafe {
            let state = &*(lparam.0 as *const EnumState);
            let mut win_pid = 0u32;
            GetWindowThreadProcessId(hwnd, Some(&mut win_pid));
            if win_pid == state.pid {
                let mut buf = [0u16; 64];
                let len = GetWindowTextW(hwnd, &mut buf);
                if len > 0 {
                    let title = String::from_utf16_lossy(&buf[..len as usize]);
                    if title.contains("LyraFlow") {
                        let _ = SendMessageW(
                            hwnd,
                            WM_SETICON,
                            Some(WPARAM(ICON_SMALL as usize)),
                            Some(LPARAM(state.hicon)),
                        );
                    }
                }
            }
            BOOL(1)
        }
    }

    let state = EnumState {
        pid: std::process::id(),
        hicon: hicon_raw,
    };
    unsafe {
        let _ = EnumWindows(Some(enum_proc), LPARAM(&state as *const EnumState as isize));
    }
}

#[cfg(not(target_os = "windows"))]
pub fn apply_titlebar_icon() {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shortcuts_format_validate_and_detect_conflicts() {
        let primary = KeyboardShortcut::default_primary();
        let cancel = KeyboardShortcut::default_cancel();
        assert_eq!(primary.label(), "Ctrl + Espacio");
        assert_eq!(cancel.label(), "Ctrl + Alt + Espacio");
        assert!(primary.validation_error().is_none());
        assert!(!primary.conflicts_with(&cancel));
        assert!(primary.conflicts_with(&primary));

        let win_space = KeyboardShortcut {
            modifiers: BTreeSet::from([ShortcutModifier::Windows]),
            key: ShortcutKey::Space,
        };
        assert!(win_space.validation_error().is_some());

        let parsed = KeyboardShortcut::parse(&cancel.serialize()).unwrap();
        assert_eq!(parsed, cancel);
    }
}
