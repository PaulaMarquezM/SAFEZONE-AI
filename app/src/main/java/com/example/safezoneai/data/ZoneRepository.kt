package com.example.safezoneai.data

import android.location.Location

/**
 * Repositorio que gestiona las zonas inseguras de la ciudad.
 * En una versión con IA, esto se conectaría a un modelo de ML o API.
 * Por ahora usa datos locales con coordenadas de zonas peligrosas.
 */
class ZoneRepository {

    /**
     * Representa una zona insegura con coordenadas y radio.
     */
    data class DangerousZone(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val dangerLevel: DangerLevel
    )

    enum class DangerLevel {
        LOW,      // Precaución
        MEDIUM,   // Alerta
        HIGH,     // Peligro
        CRITICAL  // Peligro extremo
    }

    /**
     * Lista de zonas peligrosas (simuladas para el proyecto académico).
     * En producción, esto vendría de una API o modelo de IA.
     *
     * Coordenadas de ejemplo para Ciudad de México:
     */
    private val dangerousZones = listOf(
        DangerousZone(
            name = "Zona Tepito",
            latitude = 19.4486,
            longitude = -99.1238,
            radiusMeters = 500f,
            dangerLevel = DangerLevel.HIGH
        ),
        DangerousZone(
            name = "Doctores Norte",
            latitude = 19.4236,
            longitude = -99.1444,
            radiusMeters = 300f,
            dangerLevel = DangerLevel.MEDIUM
        ),
        DangerousZone(
            name = "Iztapalapa Centro",
            latitude = 19.3579,
            longitude = -99.0553,
            radiusMeters = 400f,
            dangerLevel = DangerLevel.HIGH
        ),
        DangerousZone(
            name = "Ecatepec Industrial",
            latitude = 19.6018,
            longitude = -99.0609,
            radiusMeters = 600f,
            dangerLevel = DangerLevel.CRITICAL
        )
    )

    /**
     * Verifica si una ubicación está dentro de una zona peligrosa.
     * @return La zona peligrosa más cercana si está dentro, null si es zona segura.
     */
    fun checkDangerousZone(latitude: Double, longitude: Double): DangerousZone? {
        val userLocation = Location("user").apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        return dangerousZones.firstOrNull { zone ->
            val zoneLocation = Location("zone").apply {
                this.latitude = zone.latitude
                this.longitude = zone.longitude
            }

            val distance = userLocation.distanceTo(zoneLocation)
            distance <= zone.radiusMeters
        }
    }

    /**
     * Obtiene todas las zonas peligrosas (para mostrar en el mapa).
     */
    fun getAllDangerousZones(): List<DangerousZone> = dangerousZones

    /**
     * Calcula la distancia a la zona peligrosa más cercana.
     */
    fun getDistanceToNearestDanger(latitude: Double, longitude: Double): Pair<DangerousZone, Float>? {
        val userLocation = Location("user").apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        return dangerousZones.map { zone ->
            val zoneLocation = Location("zone").apply {
                this.latitude = zone.latitude
                this.longitude = zone.longitude
            }
            zone to userLocation.distanceTo(zoneLocation)
        }.minByOrNull { it.second }
    }
}