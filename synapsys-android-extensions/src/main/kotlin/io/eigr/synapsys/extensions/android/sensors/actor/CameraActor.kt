package io.eigr.synapsys.extensions.android.sensors.actor

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorPointer
import io.eigr.synapsys.extensions.android.sensors.events.SensorData
import io.eigr.synapsys.extensions.android.sensors.internals.ActorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import android.content.Context as AndroidContext
import io.eigr.synapsys.core.actor.Context as ActorContext

/**
 * CameraActor is an actor that manages the capture of images from the Android device's camera
 * and sends the captured frames to another actor.
 *
 * @param S Type of the actor's initial state.
 * @param M Type of the message received by the actor.
 * @param id Unique identifier of the actor.
 * @param initialState Initial state of the actor (optional).
 * @param androidContext Android context required to access the camera.
 * @param cameraId ID of the camera to be used (default: "0").
 * @param targetFps Desired frame rate per second (default: 30fps).
 * @param targetResolution Target resolution of the capture (default: 1920x1080).
 */
open class CameraActor<S : Any, M : SensorData>(
    id: String,
    initialState: S?,
    private val androidContext: AndroidContext,
    private val cameraId: String = "0",
    private val targetFps: Int = 30,
    private val targetResolution: Size = Size(1920, 1080),
) : Actor<S, M, Unit>(
    id = "camera-actor-${id}",
    initialState = initialState
) {

    private val log = LoggerFactory.getLogger(CameraActor::class.java)

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val cameraManager by lazy {
        androidContext.getSystemService(AndroidContext.CAMERA_SERVICE) as CameraManager
    }

    private val characteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }

    private val fpsRange: Range<Int> = characteristics.get(
        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
    )?.maxBy { it.upper } ?: Range(30, 30)

    private var targetActor: ActorPointer<*>? = null

    /**
     * Starts the camera and initializes image processing.
     *
     * @param ctx The actor context.
     * @return Updated actor context.
     */
    @SuppressLint("MissingPermission", "NewApi")
    override fun onStart(ctx: ActorContext<S>): ActorContext<S> {
        targetActor = ctx.system.actorOf(
            id = "processor-$id",
            initialState = initialState!!
        ) { id, state ->
            ActorHandler<S, M>(id, state).apply {
                parentActor = this@CameraActor
            }
        }
        val handlerThread = HandlerThread("CameraBackground").apply { start() }
        backgroundHandler = Handler(handlerThread.looper)

        imageReader = ImageReader.newInstance(
            targetResolution.width,
            targetResolution.height,
            ImageFormat.YUV_420_888,
            2
        ).apply {
            setOnImageAvailableListener({ reader ->
                reader?.acquireLatestImage()?.use { image ->
                    val frame = convertImageToFrame(image)
                    scope.launch {
                        targetActor?.send(frame)
                    }
                }
            }, backgroundHandler)
        }

        cameraManager.openCamera(cameraId, cameraExecutor, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCaptureSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                closeCamera()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                log.error("Camera error: {}", error)
                closeCamera()
            }
        })

        return ctx
    }

    /**
     * Stops the camera and releases resources.
     */
    override fun onStop() {
        closeCamera()
    }

    /**
     * Handles received messages.
     *
     * @param message The received message.
     * @param ctx The actor context.
     * @return A pair containing the updated actor context and unit result.
     */
    override fun onReceive(message: M, ctx: ActorContext<S>): Pair<ActorContext<S>, Unit> {
        return this.onReceive(message, ctx)
    }

    /**
     * Creates a camera capture session.
     */
    private fun createCaptureSession() {
        val surfaces = listOf(imageReader.surface)

        cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startRepeatingPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                log.error("Failed to configure capture session")
            }
        }, backgroundHandler)
    }

    /**
     * Starts previewing frames continuously.
     */
    private fun startRepeatingPreview() {
        val requestBuilder = cameraDevice.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            addTarget(imageReader.surface)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        }

        captureSession.setRepeatingRequest(
            requestBuilder.build(),
            null,
            backgroundHandler
        )
    }

    /**
     * Converts an image to a CameraSensorFrameData object.
     *
     * @param image The image to convert.
     * @return Converted sensor frame data.
     */
    private fun convertImageToFrame(image: Image): SensorData.CameraSensorFrameData {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return SensorData.CameraSensorFrameData(
            data = bytes,
            timestamp = image.timestamp,
            format = image.format,
            resolution = Pair(image.width, image.height),
            fps = targetFps.toDouble(),
            characteristics = characteristics
        )
    }

    /**
     * Closes the camera and releases resources.
     */
    private fun closeCamera() {
        captureSession.close()
        imageReader.close()
        cameraDevice.close()
    }
}