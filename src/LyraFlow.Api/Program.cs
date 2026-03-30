using LyraFlow.Api.Services;
using Microsoft.AspNetCore.Mvc;

var builder = WebApplication.CreateBuilder(args);

// Configuración de CORS
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll",
        builder =>
        {
            builder.AllowAnyOrigin()
                   .AllowAnyMethod()
                   .AllowAnyHeader();
        });
});

// Registrar servicios
builder.Services.AddHttpClient();
builder.Services.AddScoped<ITranscriptionService, TranscriptionService>();
builder.Services.AddScoped<ILlmService, LlmService>();

var app = builder.Build();

app.UseCors("AllowAll");

app.MapGet("/", () => "LyraFlow API is running!");

// Endpoint para transcripción (soporta Groq y Local Whisper)
app.MapPost("/api/transcribe", async (
    [FromForm] IFormFile file, 
    [FromForm] string? provider, 
    [FromForm] string? apiKey, 
    [FromForm] string? localModel,
    ITranscriptionService transcriptionService) =>
{
    if (file == null || file.Length == 0) return Results.BadRequest("No se proporcionó audio.");

    using var ms = new MemoryStream();
    await file.CopyToAsync(ms);
    var audioData = ms.ToArray();

    try
    {
        if (provider?.ToLower() == "groq" && !string.IsNullOrEmpty(apiKey))
        {
            var text = await transcriptionService.TranscribeAsync(audioData, apiKey);
            return Results.Ok(new { text });
        }
        else
        {
            var model = localModel ?? "Base";
            var text = await transcriptionService.TranscribeLocalAsync(audioData, model);
            return Results.Ok(new { text });
        }
    }
    catch (Exception ex)
    {
        return Results.Problem(ex.Message);
    }
}).DisableAntiforgery(); // Requerido para Minimal API con FromForm en .NET 8+

// Endpoint para refinamiento LLM
app.MapPost("/api/refine", async (
    [FromBody] RefineRequest request, 
    ILlmService llmService) =>
{
    if (string.IsNullOrEmpty(request.Text)) return Results.BadRequest("Texto vacío.");
    
    try
    {
        var refined = await llmService.RefineTextAsync(request.Text, request.ApiKey, request.Model, request.PromptContext);
        return Results.Ok(new { refined });
    }
    catch (Exception ex)
    {
        return Results.Problem(ex.Message);
    }
});

// Endpoint combinado (Procesamiento completo)
app.MapPost("/api/process", async (
    [FromForm] ProcessRequestForm request, 
    ITranscriptionService transcriptionService,
    ILlmService llmService) =>
{
    if (request.File == null || request.File.Length == 0) return Results.BadRequest("No se proporcionó audio.");

    using var ms = new MemoryStream();
    await request.File.CopyToAsync(ms);
    var audioData = ms.ToArray();

    try
    {
        // 1. Transcripción
        string transcribedText;
        if (request.TranscribeProvider?.ToLower() == "groq" && !string.IsNullOrEmpty(request.GroqApiKey))
        {
            transcribedText = await transcriptionService.TranscribeAsync(audioData, request.GroqApiKey);
        }
        else
        {
            transcribedText = await transcriptionService.TranscribeLocalAsync(audioData, request.LocalModel ?? "Base");
        }

        if (string.IsNullOrEmpty(transcribedText)) return Results.Ok(new { text = "", refined = "" });

        // 2. Refinamiento
        var refinedText = await llmService.RefineTextAsync(
            transcribedText, 
            request.GeminiApiKey, 
            request.GeminiModel ?? "gemini-1.5-flash", 
            request.PromptContext);

        return Results.Ok(new { text = transcribedText, refined = refinedText });
    }
    catch (Exception ex)
    {
        return Results.Problem(ex.Message);
    }
}).DisableAntiforgery();

app.Run();

// Definiciones de Modelos para los cuerpos de peticiones
public record RefineRequest(string Text, string ApiKey, string Model, string? PromptContext);

public class ProcessRequestForm
{
    public IFormFile? File { get; set; }
    public string? TranscribeProvider { get; set; } // groq o local
    public string? GroqApiKey { get; set; }
    public string? LocalModel { get; set; }
    public string GeminiApiKey { get; set; } = string.Empty;
    public string? GeminiModel { get; set; }
    public string? PromptContext { get; set; }
}
