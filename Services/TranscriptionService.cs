using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using Whisper.net;

namespace LyraFlow.Api.Services
{
    public class TranscriptionService : ITranscriptionService
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly string _modelsPath;

        public TranscriptionService(IHttpClientFactory httpClientFactory, IConfiguration configuration)
        {
            _httpClientFactory = httpClientFactory;
            _modelsPath = configuration["Whisper:ModelsPath"] ?? Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "models");
        }

        public async Task<string> TranscribeAsync(byte[] wavData, string apiKey)
        {
            if (wavData == null || wavData.Length == 0) return string.Empty;

            using var client = _httpClientFactory.CreateClient();
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

        public async Task<string> TranscribeLocalAsync(byte[] wavData, string modelName)
        {
            if (wavData == null || wavData.Length == 0) return string.Empty;

            string fileName = modelName.Contains("Large", StringComparison.OrdinalIgnoreCase) 
                ? "ggml-large-v3-turbo.bin" 
                : $"ggml-{modelName.ToLower()}.bin";

            string modelPath = Path.Combine(_modelsPath, fileName);
            
            if (!File.Exists(modelPath))
            {
                 throw new FileNotFoundException($"Modelo Whisper '{modelName}' no encontrado en {modelPath}.");
            }

            return await Task.Run(async () => {
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
            });
        }
    }
}
