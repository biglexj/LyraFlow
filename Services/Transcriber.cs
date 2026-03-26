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
            string fileName = $"ggml-{modelName.ToLower()}.bin";

            string modelPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "models", fileName);
            if (!File.Exists(modelPath))
            {
                modelPath = Path.Combine(Directory.GetCurrentDirectory(), "models", fileName);
                if (!File.Exists(modelPath))
                    throw new FileNotFoundException($"Modelo Whisper '{modelName}' no encontrado en {modelPath}");
            }

            using var whisperFactory = WhisperFactory.FromPath(modelPath);
            using var processor = whisperFactory.CreateBuilder()
                .WithLanguage("auto")
                .Build();

            using var ms = new MemoryStream(wavData);
            
            var result = "";
            await foreach (var segment in processor.ProcessAsync(ms))
            {
                result += segment.Text;
            }

            return result.Trim();
        }
    }
}
