using System.Threading.Tasks;

namespace LyraFlow.Api.Services
{
    public interface ILlmService
    {
        Task<string> RefineTextAsync(string text, string apiKey, string model, string? context = null);
    }
}
