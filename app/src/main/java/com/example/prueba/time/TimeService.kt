package com.example.prueba.time

import retrofit2.http.GET

interface TimeService {
    @GET("now?tz=Europe/Madrid&format=dd/MM/yyyy")
    suspend fun getTime(): TimeModel
}