using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using LyraFlow.Core;

namespace LyraFlow.Services
{
    public static class Llm
    {
        public static async Task<string> RefineTextAsync(string text, string apiKey, string model)
        {
            if (string.IsNullOrWhiteSpace(text)) return string.Empty;

            using var client = new HttpClient();
            string url = $"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";

            string context = "";
            try {
                if (System.IO.File.Exists("context.md"))
                    context = System.IO.File.ReadAllText("context.md");
            } catch (Exception ex) {
                Logger.Log($"Warning: No se pudo leer context.md -> {ex.Message}");
            }

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
                Logger.Log($"Gemini API Error ({response.StatusCode}): {responseString}");
                return string.Empty; 
            }

            var json = JObject.Parse(responseString);
            var refined = json["candidates"]?[0]?["content"]?["parts"]?[0]?["text"]?.ToString()?.Trim() ?? string.Empty;
            
            // Limpieza extra: normalizar saltos de línea y quitar los finales
            refined = refined.Replace("\r\n", "\n").Trim();
            
            if (string.IsNullOrEmpty(refined))
            {
                Logger.Log("Gemini devolvió una respuesta vacía.");
            }
            
            return refined;
        }
    }
}
