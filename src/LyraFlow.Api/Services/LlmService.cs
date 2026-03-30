using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace LyraFlow.Api.Services
{
    public class LlmService : ILlmService
    {
        private readonly IHttpClientFactory _httpClientFactory;

        public LlmService(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
        }

        public async Task<string> RefineTextAsync(string text, string apiKey, string model, string? context = null)
        {
            if (string.IsNullOrWhiteSpace(text)) return string.Empty;

            using var client = _httpClientFactory.CreateClient();
            string url = $"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";

            string prompt = string.IsNullOrWhiteSpace(context) 
                ? $"Refina el siguiente texto. Responde ÚNICAMENTE con el resultado final:\n\n{text}"
                : $"{context}\n\nRefina el siguiente texto. Responde ÚNICAMENTE con el resultado final:\n\n{text}";

            var requestBody = new
            {
                contents = new[]
                {
                    new {
                        parts = new[] { new { text = prompt } }
                    }
                }
            };

            var content = new StringContent(JsonConvert.SerializeObject(requestBody), Encoding.UTF8, "application/json");

            var response = await client.PostAsync(url, content);
            var responseString = await response.Content.ReadAsStringAsync();

            if (!response.IsSuccessStatusCode)
            {
                throw new Exception($"Gemini API Error ({response.StatusCode}): {responseString}");
            }

            var json = JObject.Parse(responseString);
            var refined = json["candidates"]?[0]?["content"]?["parts"]?[0]?["text"]?.ToString()?.Trim() ?? string.Empty;
            
            refined = refined.Replace("\r\n", "\n").Trim();
            
            return refined;
        }
    }
}
