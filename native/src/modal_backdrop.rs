//! Instantánea exclusiva del cliente de esta ventana, desenfocada en RAM.
//! No captura el escritorio, no escribe archivos y se descarta al cerrar el modal.
use std::sync::Arc;

pub fn capture(window: &gpui::Window) -> anyhow::Result<Arc<gpui::Image>> {
    let image = capture_client(window)?;
    // Un cuarto de resolución reduce el trabajo; sigma 7.5 (~30 px físicos) impide leer el texto de fondo.
    let small = image::imageops::resize(&image, (image.width()/4).max(1), (image.height()/4).max(1), image::imageops::FilterType::Triangle);
    let blurred = image::imageops::blur(&small, 7.5);
    let mut bytes = std::io::Cursor::new(Vec::new());
    image::DynamicImage::ImageRgba8(blurred).write_to(&mut bytes, image::ImageFormat::Png)?;
    Ok(Arc::new(gpui::Image::from_bytes(gpui::ImageFormat::Png, bytes.into_inner())))
}

#[cfg(target_os = "windows")]
fn capture_client(window: &gpui::Window) -> anyhow::Result<image::RgbaImage> {
    use raw_window_handle::{HasWindowHandle, RawWindowHandle};
    use windows::Win32::{Foundation::{HWND, RECT}, Graphics::Gdi::*, Storage::Xps::{PrintWindow, PRINT_WINDOW_FLAGS}, UI::WindowsAndMessaging::GetClientRect};
    let RawWindowHandle::Win32(handle) = HasWindowHandle::window_handle(window).map_err(|error|anyhow::anyhow!("Ventana no disponible: {error:?}"))?.as_raw() else { anyhow::bail!("Ventana no compatible"); };
    let hwnd = HWND(handle.hwnd.get() as *mut _);
    unsafe {
        let mut rect = RECT::default();
        GetClientRect(hwnd, &mut rect)?;
        let (w,h) = (rect.right, rect.bottom);
        anyhow::ensure!(w>0 && h>0 && w<=16384 && h<=16384, "Tamaño no válido");
        let dc = GetDC(Some(hwnd));
        anyhow::ensure!(!dc.0.is_null(), "No se pudo obtener el cliente");
        let memory = CreateCompatibleDC(Some(dc));
        if memory.0.is_null() { ReleaseDC(Some(hwnd),dc); anyhow::bail!("No se pudo crear la captura"); }
        let bitmap = CreateCompatibleBitmap(dc,w,h);
        if bitmap.0.is_null() { let _=DeleteDC(memory); ReleaseDC(Some(hwnd),dc); anyhow::bail!("No se pudo crear el bitmap"); }
        let old = SelectObject(memory,bitmap.into());
        let printed = PrintWindow(hwnd,memory,PRINT_WINDOW_FLAGS(3)).as_bool();
        SelectObject(memory,old);
        let result = (|| {
            anyhow::ensure!(printed,"No se pudo capturar la ventana");
            let mut info = BITMAPINFO::default();
            info.bmiHeader.biSize = std::mem::size_of::<BITMAPINFOHEADER>() as u32;
            info.bmiHeader.biWidth=w; info.bmiHeader.biHeight=-h;
            info.bmiHeader.biPlanes=1; info.bmiHeader.biBitCount=32;
            let mut pixels=vec![0u8;w as usize*h as usize*4];
            anyhow::ensure!(GetDIBits(memory,bitmap,0,h as u32,Some(pixels.as_mut_ptr().cast()),&mut info,DIB_RGB_COLORS)==h,"Captura incompleta");
            anyhow::ensure!(pixels.chunks_exact(4).any(|p| p[0]!=0 || p[1]!=0 || p[2]!=0),"Captura vacía");
            for pixel in pixels.chunks_exact_mut(4) { pixel.swap(0,2); pixel[3]=255; }
            image::RgbaImage::from_raw(w as u32,h as u32,pixels).ok_or_else(||anyhow::anyhow!("Imagen no válida"))
        })();
        let _=DeleteObject(bitmap.into()); let _=DeleteDC(memory); ReleaseDC(Some(hwnd),dc);
        result
    }
}

#[cfg(not(target_os = "windows"))]
fn capture_client(_: &gpui::Window) -> anyhow::Result<image::RgbaImage> {
    anyhow::bail!("Desenfoque de captura no disponible en esta plataforma")
}
