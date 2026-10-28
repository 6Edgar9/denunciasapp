package com.regalado.denunciasapp.api

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

/** Resultado tipado de una llamada a la API. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

/**
 * Ejecuta una llamada Retrofit y convierte éxito / error HTTP / error de red
 * en un [ApiResult], para que las pantallas solo manejen dos casos.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) ApiResult.Success(body)
            else ApiResult.Error("El servidor respondió sin datos", response.code())
        } else {
            ApiResult.Error(httpErrorMessage(response.code()), response.code())
        }
    } catch (e: CancellationException) {
        throw e // No interferir con la cancelación de corrutinas
    } catch (e: IOException) {
        ApiResult.Error("No se pudo conectar con el servidor. Verifica que el backend esté activo.")
    } catch (e: Exception) {
        ApiResult.Error("Error inesperado: ${e.localizedMessage ?: e.javaClass.simpleName}")
    }
}

private fun httpErrorMessage(code: Int): String = when (code) {
    400 -> "Datos inválidos (400)"
    404 -> "Denuncia no encontrada (404)"
    in 500..599 -> "Error interno del servidor ($code)"
    else -> "Error HTTP $code"
}
