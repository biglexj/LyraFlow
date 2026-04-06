# Construcción
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copiar el proyecto y restaurar (ahora en la raíz)
COPY ["LyraFlow.Api.csproj", "./"]
RUN dotnet restore "LyraFlow.Api.csproj"

# Copiar el código fuente y publicar
COPY . .
RUN dotnet publish "LyraFlow.Api.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Ejecución
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
EXPOSE 8080

# Dependencias para Whisper
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Descargar modelo base
RUN mkdir -p /app/models
RUN wget -O /app/models/ggml-base.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin

COPY --from=build /app/publish .

ENV ASPNETCORE_URLS=http://+:8080
ENV Whisper__ModelsPath=/app/models

ENTRYPOINT ["dotnet", "LyraFlow.Api.dll"]
