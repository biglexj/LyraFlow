package com.biglexj.lyraflow.core.hotkey

enum class ShortcutModifier(val label: String, val windowsFlag: Int) {
    Control("Ctrl", 0x0002),
    Alt("Alt", 0x0001),
    Shift("Shift", 0x0004),
    Windows("Windows", 0x0008),
}

enum class ShortcutKey(val label: String, val windowsCode: Int) {
    Space("Espacio", 0x20),
    A("A", 0x41), B("B", 0x42), C("C", 0x43), D("D", 0x44),
    E("E", 0x45), F("F", 0x46), G("G", 0x47), H("H", 0x48),
    I("I", 0x49), J("J", 0x4A), K("K", 0x4B), L("L", 0x4C),
    M("M", 0x4D), N("N", 0x4E), O("O", 0x4F), P("P", 0x50),
    Q("Q", 0x51), R("R", 0x52), S("S", 0x53), T("T", 0x54),
    U("U", 0x55), V("V", 0x56), W("W", 0x57), X("X", 0x58),
    Y("Y", 0x59), Z("Z", 0x5A),
    F1("F1", 0x70), F2("F2", 0x71), F3("F3", 0x72), F4("F4", 0x73),
    F5("F5", 0x74), F6("F6", 0x75), F7("F7", 0x76), F8("F8", 0x77),
    F9("F9", 0x78), F10("F10", 0x79), F11("F11", 0x7A),
}

data class KeyboardShortcut(
    val modifiers: Set<ShortcutModifier>,
    val key: ShortcutKey,
) {
    val label: String
        get() = (DISPLAY_ORDER.filter(modifiers::contains).map { it.label } + key.label)
            .joinToString(" + ")

    fun validationError(): String? = when {
        modifiers.size !in 1..2 -> "El atajo debe contener 2 o 3 teclas."
        modifiers == setOf(ShortcutModifier.Windows) && key == ShortcutKey.Space ->
            "Windows + Espacio está reservado para cambiar el idioma del teclado."
        else -> null
    }

    companion object {
        private val DISPLAY_ORDER = listOf(
            ShortcutModifier.Windows,
            ShortcutModifier.Control,
            ShortcutModifier.Alt,
            ShortcutModifier.Shift,
        )

        val Default = KeyboardShortcut(
            setOf(ShortcutModifier.Control),
            ShortcutKey.Space,
        )
    }
}
