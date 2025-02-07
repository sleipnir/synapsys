package io.eigr.synapsys.extensions.android.sensors.events

sealed class SensorData{
    abstract val timestamp: Long

    data class AccelerometerData(
        val x: Float,
        val y: Float,
        val z: Float,
        override val timestamp: Long
    ) : SensorData()

    data class GyroscopeData(
        val x: Float,
        val y: Float,
        val z: Float,
        override val timestamp: Long
    ) : SensorData()

    data class HeartRateData(
        val bpm: Float,
        override val timestamp: Long
    ) : SensorData()

    data class StepCounterData(
        val steps: Int,
        override val timestamp: Long
    ) : SensorData()

    data class GpsData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val accuracy: Float? = null,
        val speed: Float? = null,
        val bearing: Float? = null,
        override val timestamp: Long
    ) : SensorData() {

        /*override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Gps

            return latitude == other.latitude &&
                    longitude == other.longitude &&
                    altitude == other.altitude &&
                    accuracy == other.accuracy &&
                    speed == other.speed &&
                    bearing == other.bearing &&
                    timestamp == other.timestamp
        }*/

        override fun hashCode(): Int {
            var result = latitude.hashCode()
            result = 31 * result + longitude.hashCode()
            result = 31 * result + (altitude?.hashCode() ?: 0)
            result = 31 * result + (accuracy?.hashCode() ?: 0)
            result = 31 * result + (speed?.hashCode() ?: 0)
            result = 31 * result + (bearing?.hashCode() ?: 0)
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    data class RawSensorData(
        val sensorType: Int,
        val values: FloatArray,
        override val timestamp: Long
    ) : SensorData() {
        override fun hashCode(): Int {
            var result = sensorType
            result = 31 * result + values.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }
}