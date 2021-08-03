package com.example.camerastudy.fragments

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.camerastudy.MainActivity
import com.example.camerastudy.R
import com.example.camerastudy.databinding.FragmentCameraBinding
import com.example.camerastudy.utils.OrientationLiveData

class CameraFragment : Fragment() {

    /** Android ViewBinding */
    private var _fragmentCameraBinding : FragmentCameraBinding?= null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    /** AndroidX navigation arguments */
//    private val args : CameraFragmentArgs by navArgs()

    /** Host's navigation controller
     * 호스트의 탐색 컨트롤러 */
    private val navController : NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations)
     * CameraDevice 감지, 특성화 및 연결(모든 카메라 작업에 사용됨)*/
    private val cameraManager : CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID
     * 제공된 카메라 ID에 해당하는 [CameraCharacteristics] */
    private val characteristics : CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    /** Readers used as buffers for camera still shots
     * 카메라 스틸 샷의 버퍼로 사용되는 리더 */
    private lateinit var imageReader : ImageReader

    /** [HandlerThread] where all camera operations run
     * 모든 카메라 작업이 실행되는 [HandlerThread] */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread]
     * [cameraThread]에 해당하는 [Handler] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** Performs recording animation of flashing screen
     * 깜박이는 화면의 녹화 애니메이션 수행 */
    private val animationTask : Runnable by lazy {
        Runnable{
            // 플래시 화이트 애니메이션 Flash white animation
            fragmentCameraBinding.overlay.background = Color.argb(150, 255, 255, 255).toDrawable()
            // ANIMATION_FAST_MILLIS 를 기다립니다. Wait for ANIMATION_FAST_MILLIS
            fragmentCameraBinding.overlay.postDelayed({

            }, MainActivity.ANIMATION_FAST_MILLIS)
        }
    }

    /** [HandlerThread] where all buffer reading operations run
     * 모든 버퍼 읽기 작업이 실행되는 [HandlerThread] */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread]
     * [imageReaderThread]에 해당하는 [Handler] */
    private val imageHandler = Handler(imageReaderThread.looper)

    /** The [CameraDevice] that will be opened in this fragment
     * 이 조각에서 열릴 [CameraDevice] */
    private lateinit var camera : CameraDevice

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters
     * 매개변수로 구성된 진행 중인 [CameraCaptureSession]에 대한 내부 참조 */
    private lateinit var session : CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera
     * 카메라를 기준으로 한 장치 방향 변경에 대한 라이브 데이터 리스너 */
    private lateinit var relativeOrientation : OrientationLiveData

    // todo OrientationLiveData 제작해야함

}