using System;
using System.IO;
using NAudio.Wave;

namespace LyraFlow.Services
{
    public class AudioRecorder
    {
        private WaveInEvent _waveIn;
        private WaveFileWriter _writer;
        private MemoryStream _memoryStream;

        public void Start()
        {
            _waveIn = new WaveInEvent();
            // Formato óptimo para Whisper
            _waveIn.WaveFormat = new WaveFormat(16000, 1); 
            _memoryStream = new MemoryStream();
            _writer = new WaveFileWriter(_memoryStream, _waveIn.WaveFormat);

            _waveIn.DataAvailable += (s, a) =>
            {
                if (_writer != null)
                {
                    _writer.Write(a.Buffer, 0, a.BytesRecorded);
                }
            };

            _waveIn.StartRecording();
        }

        public byte[]? Stop()
        {
            if (_waveIn == null) return null;

            _waveIn.StopRecording();
            _waveIn.Dispose();
            _waveIn = null;

            if (_writer != null)
            {
                _writer.Flush();
                _writer.Dispose();
                _writer = null;
            }

            byte[]? bytes = _memoryStream?.ToArray();
            _memoryStream?.Dispose();
            _memoryStream = null;

            return bytes;
        }
    }
}
