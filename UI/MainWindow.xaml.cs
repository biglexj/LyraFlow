using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using NHotkey;
using NHotkey.Wpf;
using LyraFlow.Core;
using LyraFlow.Services;
using Color = System.Windows.Media.Color;
using ColorConverter = System.Windows.Media.ColorConverter;

namespace LyraFlow.UI;

/// <summary>
/// Interaction logic for MainWindow.xaml
/// </summary>
public partial class MainWindow : Window
{
    private bool _isRecordingHotkey = false;
    public static bool IsManualReviewEnabled { get; private set; } = true;
    public static bool IsSkipRefinementEnabled { get; private set; } = false;

    public MainWindow()
    {
        InitializeComponent();
        
        // Cargar configuración persistente
        var config = ConfigManager.Load();
        GeminiKeyBox.Password = config.GeminiApiKey ?? "";
        GroqKeyBox.Password = config.GroqApiKey ?? "";
        IsSkipRefinementEnabled = config.SkipRefinement;
        SkipRefinementCheck.IsChecked = config.SkipRefinement;
        
        // Vincular cambios
        SkipRefinementCheck.Checked += (s, e) => IsSkipRefinementEnabled = true;
        SkipRefinementCheck.Unchecked += (s, e) => IsSkipRefinementEnabled = false;

        // Seleccionar el modelo por defecto
        GeminiModelCombo.SelectedIndex = 0;
        App.CurrentGeminiModel = "gemini-3-flash-preview";

        // Inicializar Modelos Whisper
        string lastWhisperModel = config.SelectedLocalModel ?? "Base";
        foreach (ComboBoxItem item in WhisperModelCombo.Items)
        {
            if (item.Content.ToString()!.Equals(lastWhisperModel, StringComparison.OrdinalIgnoreCase))
            {
                WhisperModelCombo.SelectedItem = item;
                break;
            }
        }
        if (WhisperModelCombo.SelectedItem == null) WhisperModelCombo.SelectedIndex = 1; // Base
        
        UpdateModelStatus();

        this.KeyDown += MainWindow_KeyDown;
    }

    private void MainWindow_KeyDown(object sender, System.Windows.Input.KeyEventArgs e)
    {
        if (_isRecordingHotkey)
        {
            e.Handled = true;
            Key key = e.Key == Key.System ? e.SystemKey : e.Key;

            // Si el diálogo de salida está visible, Esc lo cierra
            if (key == Key.Escape && ExitDialogPanel.Visibility == Visibility.Visible)
            {
                ExitDialogPanel.Visibility = Visibility.Collapsed;
                e.Handled = true;
                return;
            }

            // Ignorar modificadores solos
            if (key == Key.LeftShift || key == Key.RightShift || key == Key.LeftCtrl || key == Key.RightCtrl || key == Key.LeftAlt || key == Key.RightAlt || key == Key.LWin || key == Key.RWin)
                return;

            ModifierKeys modifiers = Keyboard.Modifiers;
            
            // Actualizar UI
            string hotkeyText = "";
            if (modifiers.HasFlag(ModifierKeys.Control)) hotkeyText += "Ctrl+";
            if (modifiers.HasFlag(ModifierKeys.Shift)) hotkeyText += "Shift+";
            if (modifiers.HasFlag(ModifierKeys.Alt)) hotkeyText += "Alt+";
            hotkeyText += key.ToString();

            HotkeyPickerButton.Content = hotkeyText;
            HotkeyPickerButton.Background = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#38424b"));

            // Registrar el nuevo atajo globalmente
            try
            {
                if (System.Windows.Application.Current is App myApp)
                {
                    HotkeyManager.Current.AddOrReplace("ToggleRecord", key, modifiers, myApp.OnToggleRecordInternal);
                    Logger.Log($"Atajo remapeado a: {hotkeyText}");
                }
            }
            catch (Exception ex)
            {
                Logger.Log($"Error al remapear atajo: {ex.Message}");
            }

            _isRecordingHotkey = false;
        }
    }

    private void Header_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton == MouseButton.Left)
            this.DragMove();
    }

    private bool _isDarkMode = true;

    private void ThemeToggle_Click(object sender, RoutedEventArgs e)
    {
        _isDarkMode = !_isDarkMode;
        var brushBg = _isDarkMode ? new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#16161d")) : new SolidColorBrush(Colors.White);
        var brushHeader = _isDarkMode ? new SolidColorBrush(Colors.Transparent) : new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#f8f8ff")); // Pale Ghost White
        var brushText = _isDarkMode ? new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#f4f4f5")) : new SolidColorBrush(Colors.Black);
        var brushDesc = _isDarkMode ? new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#a1a1aa")) : new SolidColorBrush(Colors.DarkGray);
        
        MainBorder.Background = brushBg;
        HeaderBorder.Background = brushHeader;
        HeaderTitle.Foreground = brushText;
        ThemeToggleButton.Content = _isDarkMode ? "🌙" : "☀️";
        ThemeToggleButton.Foreground = brushText;
        CloseWindowButton.Foreground = brushText;

        // Invertir colores para el cuerpo y pie de la ventana
        TranscriptionModeLabel_Title.Foreground = brushDesc;
        WhisperModeRadio.Foreground = brushText;
        GroqRadio.Foreground = brushText;
        GlobalHotkeyLabel.Foreground = brushText;
        HotkeyPickerButton.Foreground = brushText;
        GeminiModelLabel_Title.Foreground = brushDesc;
        GeminiModelLabel.Foreground = brushText;
    }

    private void TranscriptionMode_Checked(object sender, RoutedEventArgs e)
    {
        if (IsLoaded)
        {
            App.UseGroq = GroqRadio.IsChecked == true;
        }
    }

    private void HotkeyPickerButton_Click(object sender, RoutedEventArgs e)
    {
        _isRecordingHotkey = true;
        HotkeyPickerButton.Content = "... Presiona una combinación ...";
        HotkeyPickerButton.Background = new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#4DE34234")); // Reddish
    }

    private void GeminiModelCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (GeminiModelCombo.SelectedItem is ComboBoxItem item)
        {
            App.CurrentGeminiModel = item.Content.ToString() ?? "gemini-3-flash-preview";
        }
    }

    private void WhisperModelCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (!IsLoaded) return;
        UpdateModelStatus();
    }

    private void UpdateModelStatus()
    {
        if (WhisperModelCombo.SelectedItem is ComboBoxItem item)
        {
            string modelName = item.Content.ToString()!;
            bool downloaded = ModelDownloadService.IsModelDownloaded(modelName);
            
            DownloadModelButton.Visibility = downloaded ? Visibility.Collapsed : Visibility.Visible;
            ModelStatusText.Visibility = downloaded ? Visibility.Collapsed : Visibility.Visible;
            ModelStatusText.Text = downloaded ? "" : "Modelo no descargado";
            ModelStatusText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#EF4444")); // Red
        }
    }

    private async void DownloadModelButton_Click(object sender, RoutedEventArgs e)
    {
        if (WhisperModelCombo.SelectedItem is ComboBoxItem item)
        {
            string modelName = item.Content.ToString()!;
            DownloadModelButton.IsEnabled = false;
            DownloadProgress.Visibility = Visibility.Visible;
            ModelStatusText.Visibility = Visibility.Visible;
            ModelStatusText.Text = "Descargando...";
            ModelStatusText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#a1a1aa"));

            try
            {
                await ModelDownloadService.DownloadModelAsync(modelName, (progress) =>
                {
                    Dispatcher.Invoke(() => DownloadProgress.Value = progress);
                });

                ModelStatusText.Text = "¡Descargado!";
                ModelStatusText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#14b8a6")); // Turquoise
                DownloadProgress.Visibility = Visibility.Collapsed;
                DownloadModelButton.Visibility = Visibility.Collapsed;
            }
            catch (Exception ex)
            {
                ModelStatusText.Text = "Error en descarga";
                ModelStatusText.Foreground = new SolidColorBrush(Colors.Red);
                DownloadModelButton.IsEnabled = true;
                Logger.Log($"Error descargando modelo: {ex.Message}");
            }
        }
    }

    private void SaveConfig()
    {
        var config = ConfigManager.Load();
        config.GeminiApiKey = GeminiKeyBox.Password;
        config.GroqApiKey = GroqKeyBox.Password;
        config.SkipRefinement = IsSkipRefinementEnabled;
        config.UseGroq = GroqRadio.IsChecked == true;
        config.SelectedGeminiModel = App.CurrentGeminiModel;
        if (WhisperModelCombo.SelectedItem is ComboBoxItem whisperItem)
        {
            config.SelectedLocalModel = whisperItem.Content.ToString();
        }
        ConfigManager.Save(config);
    }

    private void CloseWindowButton_Click(object sender, RoutedEventArgs e)
    {
        ExitDialogPanel.Visibility = Visibility.Visible;
    }

    private void ExitApp_Confirm_Click(object sender, RoutedEventArgs e)
    {
        System.Windows.Application.Current.Shutdown();
    }

    private void ExitDialogPanel_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        // Al hacer clic en el fondo oscuro, se cierra el diálogo
        ExitDialogPanel.Visibility = Visibility.Collapsed;
    }

    private void ExitDialogBorder_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        // Evitar que el clic dentro del cuadro blanco cierre el diálogo
        e.Handled = true;
    }

    private void HideWindow_Click(object sender, RoutedEventArgs e)
    {
        SaveConfig();
        ExitDialogPanel.Visibility = Visibility.Collapsed;
        this.Hide();
    }
}