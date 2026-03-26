using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Whisper.net;
using Whisper.net.Ggml;

namespace LyraFlow.Services
{
    public static class Transcriber
    {
        public static async Task<string> TranscribeAsync(byte[] wavData, string apiKey)
        {
            if (wavData == null || wavData.Length == 0) return string.Empty;

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);

            using var content = new MultipartFormDataContent();
            var audioContent = new ByteArrayContent(wavData);
            audioContent.Headers.ContentType = MediaTypeHeaderValue.Parse("audio/wav");
            content.Add(audioContent, "file", "audio.wav");
            content.Add(new StringContent("whisper-large-v3-turbo"), "model");

            var response = await client.PostAsync("https://api.groq.com/openai/v1/audio/transcriptions", content);
            var responseString = await response.Content.ReadAsStringAsync();

            if (!response.IsSuccessStatusCode)
            {
                throw new Exception($"Groq API Error: {responseString}");
            }

            var json = JObject.Parse(responseString);
            return json["text"]?.ToString() ?? string.Empty;
        }

        public static async Task<string> TranscribeLocalAsync(byte[] wavData)
        {
            if (wavData == null || wavData.Length == 0) return string.Empty;

            var config = LyraFlow.Core.ConfigManager.Load();
            string modelName = config.SelectedLocalModel ?? "Base";
            string fileName = modelName.Contains("Large", StringComparison.OrdinalIgnoreCase) 
                ? "ggml-large-v3-turbo.bin" 
                : $"ggml-{modelName.ToLower()}.bin";

            string modelPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "models", fileName);
            if (!File.Exists(modelPath))
            {
                modelPath = Path.Combine(Directory.GetCurrentDirectory(), "models", fileName);
            }
            
            LyraFlow.Core.Logger.Log($"[Whisper] Verificando modelo en: {modelPath}");
            if (!File.Exists(modelPath))
            {
                 throw new FileNotFoundException($"Modelo Whisper '{modelName}' no encontrado.");
            }

            return await Task.Run(async () => {
                var watch = System.Diagnostics.Stopwatch.StartNew();
                try {
                    LyraFlow.Core.Logger.Log($"[Whisper] Inicializando Factory ({modelName})...");
                    using var whisperFactory = WhisperFactory.FromPath(modelPath);
                    
                    LyraFlow.Core.Logger.Log($"[Whisper] Creando Procesador...");
                    using var processor = whisperFactory.CreateBuilder()
                        .WithLanguage("auto")
                        .Build();

                    LyraFlow.Core.Logger.Log($"[Whisper] Iniciando procesamiento de audio ({wavData.Length} bytes)...");
                    using var ms = new MemoryStream(wavData);
                    
                    var result = "";
                    int segments = 0;
                    await foreach (var segment in processor.ProcessAsync(ms))
                    {
                        result += segment.Text;
                        segments++;
                        if (segments % 10 == 0) LyraFlow.Core.Logger.Log($"[Whisper] Procesados {segments} segmentos...");
                    }

                    watch.Stop();
                    LyraFlow.Core.Logger.Log($"[Whisper] Finalizado en {watch.ElapsedMilliseconds}ms. Segmentos: {segments}");
                    return result.Trim();
                } catch (Exception ex) {
                    LyraFlow.Core.Logger.Log($"[Whisper Error] {ex.Message}");
                    throw;
                }
            });
        }
    }
}
