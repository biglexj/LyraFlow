using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace LyraFlow.Services
{
    public static class Llm
    {
        public static async Task<string> RefineTextAsync(string text, string apiKey, string model)
        {
            using var client = new HttpClient();
            string url = $"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";

            string context = "";
            try {
                context = System.IO.File.ReadAllText("context.md");
            } catch { }

            string prompt = $"{context}\n\nRefina el siguiente texto. Responde ÚNICAMENTE con el resultado final:\n\n{text}";

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
                throw new Exception($"Gemini API Error: {responseString}");
            }

            var json = JObject.Parse(responseString);
            return json["candidates"]?[0]?["content"]?["parts"]?[0]?["text"]?.ToString()?.Trim() ?? string.Empty;
        }
    }
}
