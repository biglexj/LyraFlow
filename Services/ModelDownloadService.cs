using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;

namespace LyraFlow.Services
{
    public static class ModelDownloadService
    {
        private static readonly HttpClient client = new HttpClient();

        public static string GetModelUrl(string modelName)
        {
            // URLs de HuggingFace para modelos Whisper Ggml
            string normalized = modelName.ToLower().Trim();
            return normalized switch
            {
                "tiny" => "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                "base" => "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                "small" => "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                "medium" => "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
                var m when m.Contains("large") => "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin",
                _ => throw new ArgumentException($"Modelo '{modelName}' no soportado")
            };
        }

        public static async Task DownloadModelAsync(string modelName, Action<double> progressCallback)
        {
            string url = GetModelUrl(modelName);
            string fileName = $"ggml-{modelName.ToLower()}.bin";
            string modelsDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "models");
            
            if (!Directory.Exists(modelsDir)) Directory.CreateDirectory(modelsDir);
            string destinationPath = Path.Combine(modelsDir, fileName);

            using var response = await client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
            response.EnsureSuccessStatusCode();

            var totalBytes = response.Content.Headers.ContentLength ?? -1L;
            var canReportProgress = totalBytes != -1;

            using var contentStream = await response.Content.ReadAsStreamAsync();
            using var fileStream = new FileStream(destinationPath, FileMode.Create, FileAccess.Write, FileShare.None, 8192, true);

            var totalReadBytes = 0L;
            var buffer = new byte[8192];
            var isMoreToRead = true;

            do
            {
                var readBytes = await contentStream.ReadAsync(buffer, 0, buffer.Length);
                if (readBytes == 0)
                {
                    isMoreToRead = false;
                    continue;
                }

                await fileStream.WriteAsync(buffer, 0, readBytes);

                totalReadBytes += readBytes;
                if (canReportProgress)
                {
                    progressCallback((double)totalReadBytes / totalBytes * 100);
                }
            }
            while (isMoreToRead);
        }

        public static bool IsModelDownloaded(string modelName)
        {
            string fileName = $"ggml-{modelName.ToLower()}.bin";
            string path = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "models", fileName);
            return File.Exists(path);
        }
    }
}
