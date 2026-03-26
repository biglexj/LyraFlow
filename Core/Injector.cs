using System.Threading.Tasks;
using WindowsInput;

namespace LyraFlow.Core
{
    public static class Injector
    {
        public static void Inject(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            
            var sim = new InputSimulator();
            
            // Reemplazamos \r\n por \n para normalizar
            text = text.Replace("\r\n", "\n");
            var lines = text.Split('\n');

            for (int i = 0; i < lines.Length; i++)
            {
                if (!string.IsNullOrEmpty(lines[i]))
                {
                    sim.Keyboard.TextEntry(lines[i]);
                }

                // Si no es la última línea, inyectamos un ENTER
                if (i < lines.Length - 1)
                {
                    sim.Keyboard.KeyPress(WindowsInput.Native.VirtualKeyCode.RETURN);
                }
            }

            // Eliminamos espacio final opcional para limpieza absoluta
            // sim.Keyboard.TextEntry(" "); 
        }
    }
}
