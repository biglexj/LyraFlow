//! Campo de texto editable ligero para GPUI 0.2.2 (claves API, endpoints, modelos y prompts).

use crate::theme::Palette;
use gpui::{
    App, ClickEvent, ClipboardItem, Context, FocusHandle, FontWeight, KeyDownEvent, Render,
    SharedString, Window, div, prelude::*, px, rgb,
};

struct TextTooltip {
    text: SharedString,
    bg: u32,
    border: u32,
    fg: u32,
}

impl Render for TextTooltip {
    fn render(&mut self, _window: &mut Window, _cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .px(px(10.))
            .py(px(5.))
            .rounded_lg()
            .border_1()
            .border_color(rgb(self.border))
            .bg(rgb(self.bg))
            .shadow_md()
            .text_xs()
            .text_color(rgb(self.fg))
            .child(self.text.clone())
    }
}

pub struct TextFieldState {
    pub value: String,
    pub focus: FocusHandle,
    pub revealed: bool,
}

impl TextFieldState {
    pub fn new<V: 'static>(initial: impl Into<String>, cx: &mut Context<V>) -> Self {
        Self {
            value: initial.into(),
            focus: cx.focus_handle().tab_stop(true),
            revealed: false,
        }
    }

    pub fn handle_key_down(&mut self, event: &KeyDownEvent, multiline: bool, cx: &mut App) -> bool {
        let ks = &event.keystroke;
        let ctrl = ks.modifiers.control || ks.modifiers.platform;
        let key = ks.key.as_str();

        if ctrl && key.eq_ignore_ascii_case("v") {
            if let Some(item) = cx.read_from_clipboard()
                && let Some(text) = item.text()
            {
                let clean = if multiline {
                    text
                } else {
                    text.replace(['\r', '\n'], "")
                };
                self.value.push_str(&clean);
                return true;
            }
            return false;
        }

        if ctrl && key.eq_ignore_ascii_case("a") {
            return false;
        }

        if ctrl && key.eq_ignore_ascii_case("c") {
            if !self.value.is_empty() {
                cx.write_to_clipboard(ClipboardItem::new_string(self.value.clone()));
            }
            return false;
        }

        if key == "backspace" {
            if ctrl {
                self.value.clear();
                return true;
            }
            return self.value.pop().is_some();
        }

        if key == "enter" {
            if multiline {
                self.value.push('\n');
                return true;
            }
            return false;
        }

        if !ctrl
            && !ks.modifiers.alt
            && let Some(ch) = &ks.key_char
            && !ch.chars().any(|c| c.is_control())
        {
            self.value.push_str(ch);
            return true;
        }

        false
    }
}

#[allow(clippy::too_many_arguments)]
pub fn render_text_field(
    id: impl Into<SharedString>,
    label: &str,
    placeholder: &str,
    state: &TextFieldState,
    masked: bool,
    enabled: bool,
    multiline: bool,
    paste_tooltip: Option<&str>,
    palette: &Palette,
    window: &Window,
    on_key: impl Fn(&KeyDownEvent, &mut Window, &mut App) + 'static,
    on_toggle_reveal: Option<Box<dyn Fn(&ClickEvent, &mut Window, &mut App) + 'static>>,
    on_paste: Option<Box<dyn Fn(&ClickEvent, &mut Window, &mut App) + 'static>>,
) -> impl IntoElement {
    let focused = state.focus.is_focused(window);
    let display = if state.value.is_empty() {
        placeholder.to_string()
    } else if masked && !state.revealed {
        "•".repeat(state.value.chars().count().clamp(6, 28))
    } else {
        state.value.clone()
    };

    let border_color = if focused {
        palette.focus_ring
    } else {
        palette.control_border
    };
    let text_color = if !enabled {
        palette.disabled_foreground
    } else if state.value.is_empty() {
        palette.outline
    } else {
        palette.foreground
    };

    let field_id: SharedString = id.into();
    let paste_id: SharedString = format!("{field_id}-paste").into();
    let reveal_id: SharedString = format!("{field_id}-reveal").into();

    let mut box_div = div()
        .id(field_id)
        .w_full()
        .rounded_xl()
        .border_1()
        .border_color(rgb(border_color))
        .bg(rgb(if enabled {
            palette.surface
        } else {
            palette.disabled_surface
        }))
        .px_3()
        .py_2()
        .flex()
        .flex_row()
        .items_center()
        .justify_between()
        .gap_2()
        .track_focus(&state.focus);

    if enabled {
        box_div = box_div
            .cursor_text()
            .tab_stop(true)
            .on_click({
                let handle = state.focus.clone();
                move |_, window, cx| {
                    handle.focus(window);
                    cx.stop_propagation();
                }
            })
            .on_key_down(on_key);
    }

    let mut left = div()
        .flex_1()
        .min_w_0()
        .flex()
        .flex_col()
        .gap(px(2.))
        .child(
            div()
                .text_xs()
                .text_color(rgb(palette.secondary_foreground))
                .child(label.to_string()),
        );

    let mut value_div =
        div()
            .text_sm()
            .text_color(rgb(text_color))
            .child(if focused && state.value.is_empty() {
                "|".to_string()
            } else if focused {
                format!("{display}|")
            } else {
                display
            });
    if multiline {
        value_div = value_div.min_h(px(72.));
    }
    left = left.child(value_div);

    let mut actions = div()
        .flex_shrink_0()
        .flex()
        .flex_row()
        .items_center()
        .gap_1();

    if enabled && let Some(paste_cb) = on_paste {
        let tip_text: SharedString = paste_tooltip
            .unwrap_or("Pegar desde el portapapeles")
            .to_string()
            .into();
        let tip_bg = palette.surface_raised;
        let tip_border = palette.control_border;
        let tip_fg = palette.foreground;
        actions = actions.child(
            div()
                .id(paste_id)
                .cursor_pointer()
                .px_2()
                .py_1()
                .rounded_lg()
                .text_xs()
                .font_weight(FontWeight::SEMIBOLD)
                .text_color(rgb(palette.accent_foreground))
                .hover(|s| s.bg(rgb(palette.surface_hover)))
                .tooltip(move |_, cx| {
                    cx.new(|_| TextTooltip {
                        text: tip_text.clone(),
                        bg: tip_bg,
                        border: tip_border,
                        fg: tip_fg,
                    })
                    .into()
                })
                .on_click(move |ev, win, cx| {
                    paste_cb(ev, win, cx);
                    cx.stop_propagation();
                })
                .child("Pegar"),
        );
    }

    if enabled
        && masked
        && let Some(reveal_cb) = on_toggle_reveal
    {
        let tip_text: SharedString = if state.revealed {
            "Ocultar clave"
        } else {
            "Mostrar clave"
        }
        .into();
        let tip_bg = palette.surface_raised;
        let tip_border = palette.control_border;
        let tip_fg = palette.foreground;
        actions = actions.child(
            div()
                .id(reveal_id)
                .cursor_pointer()
                .px_2()
                .py_1()
                .rounded_lg()
                .text_xs()
                .font_weight(FontWeight::SEMIBOLD)
                .text_color(rgb(palette.accent_foreground))
                .hover(|s| s.bg(rgb(palette.surface_hover)))
                .tooltip(move |_, cx| {
                    cx.new(|_| TextTooltip {
                        text: tip_text.clone(),
                        bg: tip_bg,
                        border: tip_border,
                        fg: tip_fg,
                    })
                    .into()
                })
                .on_click(move |ev, win, cx| {
                    reveal_cb(ev, win, cx);
                    cx.stop_propagation();
                })
                .child(if state.revealed { "Ocultar" } else { "Mostrar" }),
        );
    }

    box_div.child(left).child(actions)
}
