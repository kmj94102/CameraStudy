package com.example.camerastudy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.example.camerastudy.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.util.*

class MainActivity : AppCompatActivity() {

    private val binding : ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var cameraDevice : CameraDevice? = null
    private var mPreviewBuilder : CaptureRequest.Builder? = null
    private var mPreviewSession : CameraCaptureSession? = null
    private lateinit var manager : CameraManager

    // 카메라 설정에 관한 변수
    private var mPreviewSize : Size?= null
    private var map : StreamConfigurationMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 퍼미션 체크
        checkPermission()

    }

    /**
     * 퍼미션 체크
     * */
    private fun checkPermission(){
        TedPermission.with(this)
            .setPermissionListener(object : PermissionListener{
                override fun onPermissionGranted() {
                    Log.d(TAG.plus(" permission"), "Granted")
                    binding.preview.surfaceTextureListener = mSurfaceTextureListener
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Log.d(TAG.plus(" permission"), "Denied")
                    finish()
                }
            })
            .setDeniedMessage("카메라 권한을 허용해 주세요")
            .setPermissions(android.Manifest.permission.CAMERA)
            .check()
    }


    private val mSurfaceTextureListener = object :  TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }
    }


    private fun openCamera(width : Int, height : Int){
        // 카메라 매니저 객체 생성
        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            // default 카메라 선택
            manager.cameraIdList.forEach {
                Log.e("$TAG cameraIdList", it)
            }
            val cameraId = manager.cameraIdList[0]

            // 카메라 특성 조사
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val fps = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            Log.e("$TAG Characteristics", "maximum frame rate is : ${fps?.get(fps.size-1)} / hardware level : $level")

            // StreamConfigurationMap 객체에는 카메라의 각종 지원 정보가 담겨있다.
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // 미리보기용 TextureView 화면 크기용을 설정 <- 제공할 수 있는 최대크기를 가르킨다.
            mPreviewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.get(0)
            val fpsForVideo = map?.highSpeedVideoFpsRanges
            Log.e(TAG, "for video : ${fpsForVideo?.get(fpsForVideo.size-1)} / preview Size : width - ${mPreviewSize?.width}, height - $height")

        }catch (e : CameraAccessException){
            Log.e("$TAG openCamera", "카메라 디바이스에 접근이 불가능 합니다.\n${e.printStackTrace()}")
        }
    }


    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            // cameraDevice 객체 생성
            cameraDevice = camera
            // CaptureRequest.Builder 객체와 CaptureSession 객체 생성하여 미리보기 화면을 실행시킨다.
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {}

        override fun onError(camera: CameraDevice, error: Int) {}

    }

    @SuppressLint("Recycle")
    private fun startPreview(){
        if (cameraDevice == null || binding.preview.isAvailable.not() || mPreviewSize == null){
            Log.e(TAG, "startPreview() fail")
            return
        }

        val texture = binding.preview.surfaceTexture
        val surface = Surface(texture)
        try{
            mPreviewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }catch (e : CameraAccessException){
            Log.e("$TAG startPreview", "CaptureRequest 객체 생성 실패\n${e.printStackTrace()}")
        }
        mPreviewBuilder?.addTarget(surface)

        try {
            // 미리보기용으로 위에서 생성한 surface 객체 사용
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback(){
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                null
            )
        } catch (e : CameraAccessException){
            Log.e("$TAG startPreview", "CaptureSession 객체 생성 실패\n${e.printStackTrace()}")
        }
    }

    private fun updatePreview(){
        if(cameraDevice == null) return

        mPreviewBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        val thread = HandlerThread("CameraPreview")
        thread.start()
        val backgroundHandler = Handler(thread.looper)
        try{
            mPreviewBuilder?.let{
                mPreviewSession?.setRepeatingRequest(it.build(), null, backgroundHandler)
            } ?: Log.e("$TAG updatePreview", "mPreviewBuilder null")
        }catch (e : CameraAccessException){
            Log.e("$TAG updatePreview", "${e.printStackTrace()}")
        }
    }


    companion object {
        private val TAG = this::class.java.javaClass.simpleName
    }
}