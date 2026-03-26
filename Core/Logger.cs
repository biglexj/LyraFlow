using System;
using System.IO;

namespace LyraFlow.Core
{
    public static class Logger
    {
        private static string logFilePath;

        static Logger()
        {
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            string logFolder = Path.Combine(baseDir, "log");
            Directory.CreateDirectory(logFolder);
            logFilePath = Path.Combine(logFolder, "LyraFlow.log");
        }

        public static void Log(string message)
        {
            try
            {
                string logEntry = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {message}{Environment.NewLine}";
                File.AppendAllText(logFilePath, logEntry);
            }
            catch
            {
                // Ignorar si no puede escribir
            }
        }
    }
}
