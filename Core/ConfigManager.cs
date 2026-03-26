using System;
using System.IO;
using System.Text.Json;
using LyraFlow.Core;

namespace LyraFlow.Core
{
    public class AppConfig
    {
        public string? GeminiApiKey { get; set; }
        public string? GroqApiKey { get; set; }
        public string? SelectedGeminiModel { get; set; }
        public bool UseGroq { get; set; }
        public bool SkipRefinement { get; set; }
        public string? GlobalHotkey { get; set; }
    }

    public static class ConfigManager
    {
        private static readonly string ConfigPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "LyraFlow",
            "config.json"
        );

        public static AppConfig Load()
        {
            try
            {
                if (File.Exists(ConfigPath))
                {
                    string json = File.ReadAllText(ConfigPath);
                    return JsonSerializer.Deserialize<AppConfig>(json) ?? new AppConfig();
                }
            }
            catch (Exception ex)
            {
                Logger.Log($"Error cargando configuración: {ex.Message}");
            }
            return new AppConfig();
        }

        public static void Save(AppConfig config)
        {
            try
            {
                string dir = Path.GetDirectoryName(ConfigPath)!;
                if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);

                string json = JsonSerializer.Serialize(config, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(ConfigPath, json);
            }
            catch (Exception ex)
            {
                Logger.Log($"Error guardando configuración: {ex.Message}");
            }
        }
    }
}
