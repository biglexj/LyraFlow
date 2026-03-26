using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Animation;

namespace LyraFlow.UI
{
    public enum OverlayState { Idle, Recording, Processing }

    public partial class StatusOverlay : Window
    {
        public StatusOverlay()
        {
            InitializeComponent();
            this.Top = 15;
            this.Left = (SystemParameters.PrimaryScreenWidth - this.Width) / 2;
        }

        public void SetState(OverlayState state)
        {
            var sbWave = (Storyboard)this.Resources["WaveAnimation"];
            var sbProc = (Storyboard)this.Resources["ProcessingAnimation"];

            // Reset colors
            var brush = state == OverlayState.Processing 
                ? new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#f59e0b")) // Amber
                : new SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#14b8a6")); // Turquoise

            Bar1.Fill = Bar2.Fill = Bar3.Fill = Bar4.Fill = Bar5.Fill = brush;

            if (state == OverlayState.Idle)
            {
                IndicatorGroup.Visibility = Visibility.Visible;
                IdleBar.Visibility = Visibility.Visible;
                RecordingBars.Visibility = Visibility.Hidden;
                sbWave.Stop(this);
                sbProc.Stop(this);
                this.Hide();
            }
            else if (state == OverlayState.Recording)
            {
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
                IndicatorGroup.Visibility = Visibility.Visible;
                IdleBar.Visibility = Visibility.Hidden;
                RecordingBars.Visibility = Visibility.Visible;
                sbWave.Stop(this);
                sbProc.Begin(this, true);
                this.Show();
            }
        }
    }
}
