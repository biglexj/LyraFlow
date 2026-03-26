using System;
using System.Runtime.InteropServices;
using NAudio.CoreAudioApi;

namespace LyraFlow.Services
{
    public static class AudioManager
    {
        private static bool _isMutedByApp = false;
        private static float _originalVolume = 1.0f;

        public static void MuteSystem()
        {
            try
            {
                var enumerator = new MMDeviceEnumerator();
                var device = enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
                
                if (device.AudioEndpointVolume.Mute) return;

                _originalVolume = device.AudioEndpointVolume.MasterVolumeLevelScalar;
                device.AudioEndpointVolume.Mute = true;
                _isMutedByApp = true;
                Core.Logger.Log("Sistema silenciado para grabación.");
            }
            catch (Exception ex)
            {
                Core.Logger.Log($"Error al silenciar sistema: {ex.Message}");
            }
        }

        public static void UnmuteSystem()
        {
            if (!_isMutedByApp) return;

            try
            {
                var enumerator = new MMDeviceEnumerator();
                var device = enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
                
                device.AudioEndpointVolume.Mute = false;
                _isMutedByApp = false;
                Core.Logger.Log("Sistema reactivado.");
            }
            catch (Exception ex)
            {
                Core.Logger.Log($"Error al reactivar sistema: {ex.Message}");
            }
        }
    }
}
