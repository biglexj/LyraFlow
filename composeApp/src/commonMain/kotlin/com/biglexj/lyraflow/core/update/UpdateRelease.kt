package com.biglexj.lyraflow.core.update

data class UpdateRelease(
    val version: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val body: String = "",
)
