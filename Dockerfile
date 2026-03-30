# Estapa de construcción
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /app

# Copiar archivos del proyecto y restaurar dependencias
COPY src/LyraFlow.Api/LyraFlow.Api.csproj ./
RUN dotnet restore

# Copiar el resto de los archivos y compilar
COPY src/LyraFlow.Api/ ./
RUN dotnet publish -c Release -o out

# Etapa de ejecución
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime
WORKDIR /app

# Instalar dependencias para Whisper (libdl, etc) si es necesario
# Generalmente el runtime de .NET 8 en Linux ya incluye lo necesario para P/Invoke
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Crear carpeta de modelos y descargar modelo 'base' por defecto
RUN mkdir -p /app/models
RUN wget -O /app/models/ggml-base.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin

# Copiar archivos publicados
COPY --from=build /app/out ./

# Configurar variables de entorno
ENV ASPNETCORE_URLS=http://+:8080
ENV Whisper__ModelsPath=/app/models

EXPOSE 8080

ENTRYPOINT ["dotnet", "LyraFlow.Api.dll"]
