package com.biglexj.lyraflow.core.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
