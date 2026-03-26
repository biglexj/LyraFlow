using System;
using System.Windows;
using System.Windows.Input;
using System.Threading.Tasks;
using DotNetEnv;
using NHotkey;
using NHotkey.Wpf;
using LyraFlow.Services;
using LyraFlow.UI;
using LyraFlow.Core;

namespace LyraFlow
{
    public partial class App : System.Windows.Application
    {
        private AudioRecorder? _recorder;
        private bool _isRecording = false;
        private StatusOverlay? _overlay;
        private static System.Threading.Mutex? _mutex = null;
        private System.Windows.Forms.NotifyIcon? _notifyIcon;
        private MainWindow? _configWindow;

        public static string CurrentGeminiModel { get; set; } = "gemini-3-flash-preview";
        public static bool UseGroq { get; set; } = false;

        protected override void OnStartup(StartupEventArgs e)
        {
            const string appName = "LyraFlowAppMutex";
            bool createdNew;

            _mutex = new System.Threading.Mutex(true, appName, out createdNew);

            if (!createdNew)
            {
                // App is already running. Exit silently.
                System.Windows.Application.Current.Shutdown();
                return;
            }

            base.OnStartup(e);
            
            ShutdownMode = ShutdownMode.OnExplicitShutdown;
            Logger.Log("LyraFlow iniciándose...");

            // Cargar archivo .env del directorio actual (la raíz del proyecto)
            Env.Load(); 
            
            _overlay = new StatusOverlay();
            _recorder = new AudioRecorder();

            // Configurar icono en la bandeja del sistema (System Tray)
            _notifyIcon = new System.Windows.Forms.NotifyIcon();
            try
            {
                var iconStream = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/Icon/app_icon.ico")).Stream;
                _notifyIcon.Icon = new System.Drawing.Icon(iconStream);
            }
            catch (Exception ex)
            {
                Logger.Log($"Error al cargar el icono de la bandeja: {ex.Message}");
                _notifyIcon.Icon = System.Drawing.SystemIcons.Information;
            }
            _notifyIcon.Visible = true;
            _notifyIcon.Text = "LyraFlow";
            _notifyIcon.ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip();
            _notifyIcon.ContextMenuStrip.Items.Add("Configuración", null, (s, args) => ShowConfigWindow());
            _notifyIcon.ContextMenuStrip.Items.Add("Salir", null, (s, args) => OnExitApp(null, null));
            _notifyIcon.DoubleClick += (s, args) => ShowConfigWindow();

            // Mostrar Ventana de Configuración la primera vez
            ShowConfigWindow();

            // Registrar atajo de teclado Global: Control + Shift + Espacio
            try
            {
                HotkeyManager.Current.AddOrReplace("ToggleRecord", Key.Space, ModifierKeys.Control | ModifierKeys.Shift, OnToggleRecordInternal);
                HotkeyManager.Current.AddOrReplace("ExitApp", Key.Q, ModifierKeys.Control | ModifierKeys.Shift, OnExitApp);
                Logger.Log("Atajos registrados correctamente.");
            }
            catch (Exception ex)
            {
                Logger.Log($"Error crítico: Fallo al registrar atajos -> {ex.Message}");
                System.Windows.MessageBox.Show($"Fallo al registrar Atajos: {ex.Message}");
                System.Windows.Application.Current.Shutdown();
            }
        }

        private void OnExitApp(object sender, HotkeyEventArgs e)
        {
            Logger.Log("Cerrando LyraFlow por atajo Esc...");
            if (_notifyIcon != null)
            {
                _notifyIcon.Visible = false;
                _notifyIcon.Dispose();
            }
            System.Windows.Application.Current.Shutdown();
        }

        private void ShowConfigWindow()
        {
            if (_configWindow == null)
            {
                _configWindow = new MainWindow();
                _configWindow.Closed += (s, ev) => _configWindow = null;
            }
            _configWindow.Show();
            _configWindow.Activate();
        }

        public void OnToggleRecordInternal(object? sender, HotkeyEventArgs e)
        {
            OnToggleRecord(sender, e);
        }

        private async void OnToggleRecord(object sender, HotkeyEventArgs e)
        {
            if (!_isRecording)
            {
                // Iniciar Grabación
                _isRecording = true;
                _overlay.SetState(OverlayState.Recording);
                AudioManager.MuteSystem(); // Silenciar sistema
                _recorder.Start();
            }
            else
            {
                // Detener Grabación
                byte[]? audioData = _recorder?.Stop();
                _isRecording = false;
                AudioManager.UnmuteSystem(); // Restaurar sonido

                // Cambiar a estado de procesamiento
                _overlay?.SetState(OverlayState.Processing);

                // Procesar Audio en segundo plano libremente
                await ProcessAudioAsync(audioData);
            }
        }

        private async Task ProcessAudioAsync(byte[] audioData)
        {
            if (audioData == null || audioData.Length == 0)
            {
                Logger.Log("Audio procesado: No hay datos.");
                return;
            }

            var config = ConfigManager.Load();
            string? geminiKey = config.GeminiApiKey;
            string? groqKey = config.GroqApiKey;

            if (string.IsNullOrEmpty(geminiKey) && !UseGroq)
            {
                Logger.Log("Error: No se ha configurado la Gemini API Key.");
                _overlay?.SetState(OverlayState.Idle);
                return;
            }
            if (string.IsNullOrEmpty(groqKey) && UseGroq)
            {
                Logger.Log("Error: No se ha configurado la Groq API Key.");
                _overlay?.SetState(OverlayState.Idle);
                return;
            }

            try
            {
                string transcribed = "";
                if (UseGroq)
                {
                    Logger.Log("Iniciando transcripción con Groq...");
                    transcribed = await Transcriber.TranscribeAsync(audioData, groqKey ?? "");
                    Logger.Log($"Transcripción completada: \"{transcribed}\"");
                }
                else
                {
                    Logger.Log("Iniciando transcripción local (Whisper)...");
                    transcribed = await Transcriber.TranscribeLocalAsync(audioData);
                    Logger.Log($"Transcripción local completada: \"{transcribed}\"");
                }
                
                if (string.IsNullOrWhiteSpace(transcribed))
                {
                    Logger.Log("Transcripción vacía. Cancelando refinamiento.");
                    return;
                }

                string refined = "";
                if (LyraFlow.UI.MainWindow.IsSkipRefinementEnabled)
                {
                    Logger.Log("Modo directo activado. Saltando refinamiento de IA.");
                    refined = transcribed;
                }
                else
                {
                    Logger.Log($"Refinando texto con Gemini ({CurrentGeminiModel})...");
                    refined = await Llm.RefineTextAsync(transcribed, geminiKey ?? "", CurrentGeminiModel);
                    Logger.Log("Refinamiento completado.");
                }
                
                if (!string.IsNullOrEmpty(refined))
                {
                    Logger.Log("Inyectando texto directamente...");
                    Injector.Inject(refined);
                    Logger.Log("Texto inyectado con éxito.");
                }
            }
            catch (Exception ex)
            {
                Logger.Log($"Error en ProcessAudioAsync: {ex.Message}");
            }
            finally
            {
                // Volver a estado Idle al finalizar todo el proceso
                _overlay?.SetState(OverlayState.Idle);
            }
        }
    }
}
