package com.regalado.denunciasapp.api

/**
 * Modelo que refleja la entidad Denuncia del backend.
 * `fecha` llega como String ISO-8601 (LocalDateTime serializado por Spring).
 */
data class Denuncia(
    val id: Int? = null,
    val dni: String,
    val fecha: String? = null,
    val tipo: String,
    val direccion: String? = null,
    val descripcion: String? = null,
    val estado: Int? = null
)

/**
 * Respuesta paginada de Spring Data (Page<Denuncia>).
 * Soporta tanto el formato directo (totalElements, totalPages, number, size)
 * como el formato DTO (objeto "page").
 */
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long? = null,
    val totalPages: Int? = null,
    val number: Int? = null,
    val size: Int? = null,
    val last: Boolean? = null,
    val first: Boolean? = null,
    val page: PageMetadata? = null
)

data class PageMetadata(
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int
)
