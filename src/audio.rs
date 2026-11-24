use anyhow::{Context, Result, bail};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use std::{
    fs::{self, File, OpenOptions},
    io::{BufWriter, Cursor},
    path::PathBuf,
    sync::{Arc, Mutex},
    time::{SystemTime, UNIX_EPOCH},
};

pub const MAX_SECONDS: u32 = 300;
const TARGET_SAMPLE_RATE: u32 = 16_000;
type Writer = hound::WavWriter<BufWriter<File>>;

#[derive(Default)]
pub struct Progress {
    pub samples: u64,
    pub peak: f32,
    pub error: Option<String>,
    pub mono_samples: Vec<f32>,
}

pub struct Recording {
    pub path: PathBuf,
    pub seconds: f64,
    #[allow(dead_code)]
    pub sample_rate: u32,
    #[allow(dead_code)]
    pub channels: u16,
    pub wav_16khz_mono: Vec<u8>,
}

pub struct Capture {
    stream: Option<cpal::Stream>,
    writer: Arc<Mutex<Option<Writer>>>,
    pub progress: Arc<Mutex<Progress>>,
    pub sample_rate: u32,
    pub channels: u16,
    #[allow(dead_code)]
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
        let directory = std::env::temp_dir().join("LyraFlow");
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
        let channels = usize::from(self.channels.max(1));
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
                let mut frame_acc = 0.0_f32;
                let mut frame_count = 0usize;
                for &sample in data.iter().take(remaining) {
                    let value: f32 = cpal::Sample::from_sample(sample);
                    let clean = if value.is_finite() { value } else { 0.0 };
                    peak = peak.max(clean.abs());
                    if let Err(error) = writer.write_sample(pcm16(clean)) {
                        progress.error = Some(format!("No se pudo guardar el audio: {error}"));
                        return;
                    }
                    progress.samples += 1;
                    frame_acc += clean;
                    frame_count += 1;
                    if frame_count == channels {
                        progress.mono_samples.push(frame_acc / channels as f32);
                        frame_acc = 0.0;
                        frame_count = 0;
                    }
                }
                progress.peak = (peak * 4.5).min(1.0);
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
        let mut progress = self
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
        let mono_samples = std::mem::take(&mut progress.mono_samples);
        drop(progress);
        self.writer
            .lock()
            .map_err(|_| anyhow::anyhow!("Error al cerrar el WAV."))?
            .take()
            .context("La grabación ya está cerrada.")?
            .finalize()?;
        self.keep = true;
        let wav_16khz_mono = encode_16khz_mono_wav(&mono_samples, self.sample_rate)?;
        Ok(Recording {
            path: self.path.clone(),
            seconds,
            sample_rate: self.sample_rate,
            channels: self.channels,
            wav_16khz_mono,
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

pub fn encode_16khz_mono_wav(mono_samples: &[f32], source_rate: u32) -> Result<Vec<u8>> {
    let source_rate = source_rate.max(1);
    let resampled: Vec<i16> = if source_rate == TARGET_SAMPLE_RATE || mono_samples.is_empty() {
        mono_samples.iter().map(|&s| pcm16(s)).collect()
    } else {
        let ratio = source_rate as f64 / TARGET_SAMPLE_RATE as f64;
        let out_len = ((mono_samples.len() as f64) / ratio).round().max(1.0) as usize;
        let mut out = Vec::with_capacity(out_len);
        for i in 0..out_len {
            let pos = i as f64 * ratio;
            let idx = (pos.floor() as usize).min(mono_samples.len().saturating_sub(1));
            let next_idx = (idx + 1).min(mono_samples.len().saturating_sub(1));
            let frac = (pos - idx as f64) as f32;
            let s = mono_samples[idx] + (mono_samples[next_idx] - mono_samples[idx]) * frac;
            out.push(pcm16(s));
        }
        out
    };

    let mut buffer = Cursor::new(Vec::new());
    {
        let mut writer = hound::WavWriter::new(
            &mut buffer,
            hound::WavSpec {
                channels: 1,
                sample_rate: TARGET_SAMPLE_RATE,
                bits_per_sample: 16,
                sample_format: hound::SampleFormat::Int,
            },
        )?;
        for sample in resampled {
            writer.write_sample(sample)?;
        }
        writer.finalize()?;
    }
    Ok(buffer.into_inner())
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

    #[test]
    fn resamples_to_16khz_mono_wav() {
        let samples = vec![0.0_f32; 48_000];
        let wav = encode_16khz_mono_wav(&samples, 48_000).unwrap();
        let reader = hound::WavReader::new(Cursor::new(wav)).unwrap();
        assert_eq!(reader.spec().sample_rate, 16_000);
        assert_eq!(reader.spec().channels, 1);
        assert_eq!(reader.duration(), 16_000);
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
        let mono_samples: Vec<f32> = samples
            .iter()
            .map(|&s| f32::from(s) / f32::from(i16::MAX))
            .collect();
        Capture {
            stream: None,
            writer: Arc::new(Mutex::new(Some(writer))),
            progress: Arc::new(Mutex::new(Progress {
                samples: samples.len() as u64,
                mono_samples,
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
        assert!(!recording.wav_16khz_mono.is_empty());
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
