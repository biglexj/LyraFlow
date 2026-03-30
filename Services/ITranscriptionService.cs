using System.Threading.Tasks;

namespace LyraFlow.Api.Services
{
    public interface ITranscriptionService
    {
        Task<string> TranscribeAsync(byte[] wavData, string apiKey);
        Task<string> TranscribeLocalAsync(byte[] wavData, string modelName);
    }
}
