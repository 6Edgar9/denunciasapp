package com.regalado.denunciasapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rutas del backend denunciaservice.
 * Nota: "denuncias/" lleva barra final porque el backend mapea POST/GET en "/denuncias/".
 */
interface DenunciaApi {

    /** POST /denuncias/ - Registrar denuncia */
    @POST("denuncias/")
    suspend fun registrar(@Body denuncia: Denuncia): Response<Denuncia>

    /** GET /denuncias/?offset=&limit= - Listar paginado */
    @GET("denuncias/")
    suspend fun listar(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 10
    ): Response<PageResponse<Denuncia>>

    /** GET /denuncias/{id} - Obtener por ID */
    @GET("denuncias/{id}")
    suspend fun obtenerPorId(@Path("id") id: Int): Response<Denuncia>

    /** GET /denuncias/ciudadano/{dni} - Buscar por DNI */
    @GET("denuncias/ciudadano/{dni}")
    suspend fun buscarPorDni(@Path("dni") dni: String): Response<List<Denuncia>>

    /** PUT /denuncias/{id} - Atender denuncia (estado = 0) */
    @PUT("denuncias/{id}")
    suspend fun atender(@Path("id") id: Int): Response<Denuncia>
}
