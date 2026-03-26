using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Animation;

namespace LyraFlow.UI
{
    public enum OverlayState { Idle, Recording, Processing, Reviewing }

    public partial class StatusOverlay : Window
    {
        private string _pendingText = "";

        public StatusOverlay()
        {
            InitializeComponent();
            this.Top = 15;
            this.Left = (SystemParameters.PrimaryScreenWidth - 400) / 2; // Ancho base aproximado
            this.Width = 400;
        }

        public void SetText(string text)
        {
            _pendingText = text;
            PreviewText.Text = text;
        }

        public void SetState(OverlayState state)
        {
            var sbWave = (Storyboard)this.Resources["WaveAnimation"];
            var sbProc = (Storyboard)this.Resources["ProcessingAnimation"];

            // Reset colors
            var brush = state == OverlayState.Processing 
                ? new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#f59e0b")) // Amber
                : new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#14b8a6")); // Turquoise

            if (Bar1 != null) Bar1.Fill = Bar2.Fill = Bar3.Fill = Bar4.Fill = Bar5.Fill = brush;

            if (state == OverlayState.Idle)
            {
                ReviewPanel.Visibility = Visibility.Collapsed;
                IndicatorGroup.Visibility = Visibility.Visible;
                IdleBar.Visibility = Visibility.Visible;
                RecordingBars.Visibility = Visibility.Hidden;
                sbWave.Stop(this);
                sbProc.Stop(this);
                this.Hide();
            }
            else if (state == OverlayState.Recording)
            {
                ReviewPanel.Visibility = Visibility.Collapsed;
                IndicatorGroup.Visibility = Visibility.Visible;
                IdleBar.Visibility = Visibility.Hidden;
                RecordingBars.Visibility = Visibility.Visible;
                RecordingBars.Opacity = 1.0;
                sbWave.Begin(this, true);
                sbProc.Stop(this);
                this.Show();
            }
            else if (state == OverlayState.Processing)
            {
                ReviewPanel.Visibility = Visibility.Collapsed;
                IndicatorGroup.Visibility = Visibility.Visible;
                IdleBar.Visibility = Visibility.Hidden;
                RecordingBars.Visibility = Visibility.Visible;
                sbWave.Stop(this);
                sbProc.Begin(this, true);
                this.Show();
            }
            else if (state == OverlayState.Reviewing)
            {
                IndicatorGroup.Visibility = Visibility.Collapsed;
                ReviewPanel.Visibility = Visibility.Visible;
                sbWave.Stop(this);
                sbProc.Stop(this);
                this.Show();
                this.Activate(); // Asegurar que reciba foco si es posible (aunque es WS_EX_NOACTIVATE usualmente)
            }
        }
    }
}
