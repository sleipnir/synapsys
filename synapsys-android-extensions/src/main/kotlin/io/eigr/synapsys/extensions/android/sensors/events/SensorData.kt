package io.eigr.synapsys.extensions.android.sensors.events

import android.hardware.camera2.CameraCharacteristics

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

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val accuracy: Float? = null,
        val speed: Float? = null,
        val bearing: Float? = null,
        override val timestamp: Long
    ) : SensorData() {
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LocationData

            if (latitude != other.latitude) return false
            if (longitude != other.longitude) return false
            if (altitude != other.altitude) return false
            if (accuracy != other.accuracy) return false
            if (speed != other.speed) return false
            if (bearing != other.bearing) return false
            if (timestamp != other.timestamp) return false

            return true
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RawSensorData

            if (sensorType != other.sensorType) return false
            if (!values.contentEquals(other.values)) return false
            if (timestamp != other.timestamp) return false

            return true
        }
    }

    data class CameraSensorFrameData(
        val data: ByteArray,
        val timestamp: Long,
        val format: Int,
        val resolution: Pair<Int, Int>,
        val fps: Double,
        val characteristics: CameraCharacteristics?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CameraSensorFrameData

            if (!data.contentEquals(other.data)) return false
            if (timestamp != other.timestamp) return false
            if (format != other.format) return false
            if (resolution != other.resolution) return false
            if (fps != other.fps) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + format
            result = 31 * result + resolution.hashCode()
            result = 31 * result + fps.hashCode()
            return result
        }
    }
}