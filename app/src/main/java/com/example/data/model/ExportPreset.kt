package com.example.data.model

data class ExportPreset(
    val id: String,
    val title: String,
    val description: String,
    val format: ExportFormat,
    val resolution: ResolutionPreset,
    val quality: Int,
    val metadataPolicy: MetadataPolicy,
    val count: Int = 0
)

data class ApolloAlbum(
    val id: String,
    val name: String,
    val photoCount: Int,
    val coverResId: Int? = null,
    val isSystem: Boolean = true
)
