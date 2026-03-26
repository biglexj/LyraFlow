using WindowsInput;

namespace LyraFlow.Core
{
    public static class Injector
    {
        public static void Inject(string text)
        {
            if (string.IsNullOrWhiteSpace(text)) return;

            var sim = new InputSimulator();

            // Normalizamos saltos de línea
            text = text.Replace("\r\n", "\n").Replace("\r", "\n").Trim();

            // Dividimos por cada salto de línea individual
            // Si hay líneas vacías (doble \n = párrafo), se preservan como Shift+Enter extra
            var lines = text.Split('\n');

            for (int i = 0; i < lines.Length; i++)
            {
                var line = lines[i].Trim();

                // Si la línea tiene contenido, la escribimos
                if (!string.IsNullOrEmpty(line))
                {
                    sim.Keyboard.TextEntry(line);
                }

                // Para cada salto de línea (incluyendo líneas vacías de párrafo),
                // usamos Shift+Enter para no disparar el envío del chat
                if (i < lines.Length - 1)
                {
                    sim.Keyboard.ModifiedKeyStroke(
                        WindowsInput.Native.VirtualKeyCode.SHIFT,
                        WindowsInput.Native.VirtualKeyCode.RETURN);
                }
            }
        }
    }
}
