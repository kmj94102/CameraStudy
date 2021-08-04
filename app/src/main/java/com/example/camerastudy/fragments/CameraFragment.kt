package com.example.camerastudy.fragments

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.camerastudy.MainActivity
import com.example.camerastudy.R
import com.example.camerastudy.databinding.FragmentCameraBinding
import com.example.camerastudy.utils.BUNDLE_CAMERA_ID
import com.example.camerastudy.utils.OrientationLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {

    /** Android ViewBinding */
    private var _fragmentCameraBinding : FragmentCameraBinding?= null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

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
        cameraManager.getCameraCharacteristics(arguments?.getString(BUNDLE_CAMERA_ID)!!)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(layoutInflater)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentCameraBinding.captureButton.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            insets.consumeSystemWindowInsets()
        }

        fragmentCameraBinding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) = Unit
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Selects appropriate preview size and configures view finder
                // 적절한 미리보기 크기를 선택하고 뷰파인더를 구성합니다.
                val previewSize = getPreviewOutputSize(
                    fragmentCameraBinding.viewFinder.display,
                    characteristics,
                    SurfaceHolder::class.java
                )
                Log.d(TAG, "View finder size : ${fragmentCameraBinding.viewFinder.width} x ${fragmentCameraBinding.viewFinder.height}")
                Log.d(TAG, "Selected preview size : $previewSize")
                fragmentCameraBinding.viewFinder.setAspectRatio(
                    previewSize.width,
                    previewSize.height
                )

                // To ensure that size is set, initialize camera in the view's thread
                // 크기가 설정되었는지 확인하려면 뷰의 스레드에서 카메라를 초기화하십시오.
                view.post { initializeCamera() }

            }

            // Used to rotate the output media to match device orientation
//            relativeOrientation  todo 여기부터 진행

        })

    }

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {


    }

    companion object {
        private val TAG = CameraFragment::class.java.javaClass.simpleName

        /** Maximum number of images that will be held in the reader's buffer
         * 판독기의 버퍼에 보관될 최대 이미지 수 */
        private const val IMAGE_BUFFER_SIZE : Int = 3

        /** Maximum time allowed to wait for the result of an image capture
         * 이미지 캡처 결과를 기다리는 데 허용되는 최대 시간 */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS : Long = 5000

        /** Helper data class used to hold capture metadata with their associated image
         * 연결된 이미지와 함께 캡처 메타데이터를 보관하는 데 사용되는 도우미 데이터 클래스 */
        data class CombinedCaptureResult(
            val image : Image,
            val metadata : CaptureRequest,
            val orientation : Int,
            val format : Int
        ) : Closeable {
            override fun close() { image.close() }
        }

        /**
         * Create a [File] named a using formatted timestamp with the current date and time.
         * 현재 날짜와 시간을 사용하여 형식이 지정된 타임스탬프라는 이름의 [File]을 만듭니다.
         * @return [File] created.
         * @return [File]이 생성되었습니다.
         */
        private fun createFile(context: Context, extension : String) : File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.KOREA)
            return File(context.filesDir, "IMG_${sdf.format(Date())}.$extension")
        }
    }

}