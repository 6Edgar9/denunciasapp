package com.regalado.denunciasapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente Retrofit singleton.
 * 10.0.2.2 es el alias del localhost de la PC desde el emulador de Android.
 * TODO: Cambiar BASE_URL por la URL del servidor desplegado.
 */
object RetrofitClient {

    private const val BASE_URL = "https://denunciaservice-38if.onrender.com/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val denunciaApi: DenunciaApi by lazy {
        retrofit.create(DenunciaApi::class.java)
    }
}
