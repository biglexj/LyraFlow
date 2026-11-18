use anyhow::{Context, Result, bail};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use std::{
    fs::{self, File, OpenOptions},
    io::BufWriter,
    path::PathBuf,
    sync::{Arc, Mutex},
    time::{SystemTime, UNIX_EPOCH},
};

pub const MAX_SECONDS: u32 = 300;
type Writer = hound::WavWriter<BufWriter<File>>;

#[derive(Default)]
pub struct Progress {
    pub samples: u64,
    pub peak: f32,
    pub error: Option<String>,
}

pub struct Recording {
    pub path: PathBuf,
    pub seconds: f64,
    pub sample_rate: u32,
    pub channels: u16,
}

pub struct Capture {
    stream: Option<cpal::Stream>,
    writer: Arc<Mutex<Option<Writer>>>,
    pub progress: Arc<Mutex<Progress>>,
    pub sample_rate: u32,
    pub channels: u16,
    pub device: String,
    path: PathBuf,
    keep: bool,
}

impl Capture {
    pub fn start() -> Result<Self> {
        let device = cpal::default_host()
            .default_input_device()
            .context("No hay un micrófono predeterminado disponible.")?;
        let supported = device
            .default_input_config()
            .context("No se pudo consultar el micrófono. Revisa los permisos de Windows.")?;
        let config: cpal::StreamConfig = supported.clone().into();
        let sample_rate = config.sample_rate.0;
        let channels = config.channels;
        let directory = std::env::temp_dir().join("LyraFlow-gpui-demo");
        fs::create_dir_all(&directory)?;
        let timestamp = SystemTime::now().duration_since(UNIX_EPOCH)?.as_nanos();
        let path = directory.join(format!("dictado-{}-{timestamp}.wav", std::process::id()));
        let file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&path)?;
        let writer = match hound::WavWriter::new(
            BufWriter::new(file),
            hound::WavSpec {
                channels,
                sample_rate,
                bits_per_sample: 16,
                sample_format: hound::SampleFormat::Int,
            },
        ) {
            Ok(writer) => writer,
            Err(error) => {
                let _ = fs::remove_file(&path);
                return Err(error.into());
            }
        };
        // Construct the guard before opening the stream: every failure removes the partial WAV.
        let mut capture = Self {
            stream: None,
            writer: Arc::new(Mutex::new(Some(writer))),
            progress: Arc::new(Mutex::new(Progress::default())),
            sample_rate,
            channels,
            device: device
                .name()
                .unwrap_or_else(|_| "Micrófono predeterminado".into()),
            path,
            keep: false,
        };
        let stream = match supported.sample_format() {
            cpal::SampleFormat::F32 => capture.build_stream::<f32>(&device, &config),
            cpal::SampleFormat::I16 => capture.build_stream::<i16>(&device, &config),
            cpal::SampleFormat::U16 => capture.build_stream::<u16>(&device, &config),
            format => bail!("El formato del micrófono ({format:?}) todavía no está soportado."),
        }?;
        stream.play().context("No se pudo iniciar el micrófono.")?;
        capture.stream = Some(stream);
        Ok(capture)
    }

    fn build_stream<T>(
        &self,
        device: &cpal::Device,
        config: &cpal::StreamConfig,
    ) -> Result<cpal::Stream>
    where
        T: cpal::SizedSample,
        f32: cpal::FromSample<T>,
    {
        let writer = self.writer.clone();
        let progress = self.progress.clone();
        let errors = self.progress.clone();
        let limit = u64::from(self.sample_rate) * u64::from(self.channels) * u64::from(MAX_SECONDS);
        Ok(device.build_input_stream(
            config,
            move |data: &[T], _| {
                let Ok(mut progress) = progress.lock() else {
                    return;
                };
                if progress.error.is_some() {
                    return;
                }
                let Ok(mut guard) = writer.lock() else { return };
                let Some(writer) = guard.as_mut() else { return };
                let remaining = limit.saturating_sub(progress.samples) as usize;
                let mut peak = 0.0_f32;
                for &sample in data.iter().take(remaining) {
                    let value: f32 = cpal::Sample::from_sample(sample);
                    peak = peak.max(value.abs());
                    if let Err(error) = writer.write_sample(pcm16(value)) {
                        progress.error = Some(format!("No se pudo guardar el audio: {error}"));
                        return;
                    }
                    progress.samples += 1;
                }
                progress.peak = peak.min(1.0);
            },
            move |error| {
                if let Ok(mut progress) = errors.lock() {
                    progress.error = Some(format!("Se interrumpió el micrófono: {error}"));
                }
            },
            None,
        )?)
    }

    pub fn cancel(mut self) -> Result<()> {
        self.stream.take();
        self.writer
            .lock()
            .map_err(|_| anyhow::anyhow!("Error al cerrar el audio cancelado."))?
            .take();
        fs::remove_file(&self.path).context("No se pudo eliminar el audio cancelado.")?;
        self.keep = true;
        Ok(())
    }

    pub fn finish(mut self) -> Result<Recording> {
        self.stream.take();
        let progress = self
            .progress
            .lock()
            .map_err(|_| anyhow::anyhow!("Error al leer el audio."))?;
        if let Some(error) = &progress.error {
            bail!("{error}");
        }
        if progress.samples == 0 {
            bail!("El micrófono no entregó audio. Revisa el dispositivo y sus permisos.");
        }
        let seconds =
            progress.samples as f64 / f64::from(self.sample_rate) / f64::from(self.channels);
        drop(progress);
        self.writer
            .lock()
            .map_err(|_| anyhow::anyhow!("Error al cerrar el WAV."))?
            .take()
            .context("La grabación ya está cerrada.")?
            .finalize()?;
        self.keep = true;
        Ok(Recording {
            path: self.path.clone(),
            seconds,
            sample_rate: self.sample_rate,
            channels: self.channels,
        })
    }
}

impl Drop for Capture {
    fn drop(&mut self) {
        self.stream.take();
        if let Ok(mut writer) = self.writer.lock() {
            writer.take();
        }
        if !self.keep {
            let _ = fs::remove_file(&self.path);
        }
    }
}

fn pcm16(value: f32) -> i16 {
    if !value.is_finite() {
        return 0;
    }
    (value.clamp(-1.0, 1.0) * f32::from(i16::MAX)).round() as i16
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn clips_and_sanitizes_microphone_samples() {
        assert_eq!(pcm16(2.0), i16::MAX);
        assert_eq!(pcm16(-2.0), -i16::MAX);
        assert_eq!(pcm16(f32::NAN), 0);
        assert_eq!(pcm16(f32::INFINITY), 0);
        assert_eq!(pcm16(0.0), 0);
    }

    fn capture_fixture(samples: &[i16]) -> Capture {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        let path = std::env::temp_dir().join(format!(
            "lyra-gpui-test-{}-{timestamp}.wav",
            std::process::id()
        ));
        let spec = hound::WavSpec {
            channels: 1,
            sample_rate: 16000,
            bits_per_sample: 16,
            sample_format: hound::SampleFormat::Int,
        };
        let file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&path)
            .unwrap();
        let mut writer = hound::WavWriter::new(BufWriter::new(file), spec).unwrap();
        for sample in samples {
            writer.write_sample(*sample).unwrap();
        }
        Capture {
            stream: None,
            writer: Arc::new(Mutex::new(Some(writer))),
            progress: Arc::new(Mutex::new(Progress {
                samples: samples.len() as u64,
                ..Default::default()
            })),
            sample_rate: 16000,
            channels: 1,
            device: "test fixture".into(),
            path,
            keep: false,
        }
    }

    #[test]
    fn stop_finalizes_and_preserves_the_actual_wav() {
        let recording = capture_fixture(&[0, 16384, -16384, 32767])
            .finish()
            .unwrap();
        assert_eq!(recording.seconds, 4.0 / 16000.0);
        let mut reader = hound::WavReader::open(&recording.path).unwrap();
        assert_eq!(reader.spec().sample_rate, 16000);
        assert_eq!(
            reader
                .samples::<i16>()
                .map(Result::unwrap)
                .collect::<Vec<_>>(),
            vec![0, 16384, -16384, 32767]
        );
        drop(reader);
        fs::remove_file(recording.path).unwrap();
    }

    #[test]
    fn cancel_and_window_close_remove_partial_audio() {
        let capture = capture_fixture(&[123]);
        let path = capture.path.clone();
        capture.cancel().unwrap();
        assert!(!path.exists());
        let capture = capture_fixture(&[123]);
        let path = capture.path.clone();
        drop(capture);
        assert!(!path.exists());
    }

    #[test]
    fn missing_audio_and_device_errors_never_report_success() {
        let capture = capture_fixture(&[]);
        let path = capture.path.clone();
        assert!(capture.finish().is_err());
        assert!(!path.exists());
        let capture = capture_fixture(&[123]);
        let path = capture.path.clone();
        capture.progress.lock().unwrap().error = Some("Micrófono desconectado".into());
        assert!(capture.finish().is_err());
        assert!(!path.exists());
    }
}
