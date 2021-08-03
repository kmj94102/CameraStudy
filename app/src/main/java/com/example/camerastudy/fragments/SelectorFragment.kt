package com.example.camerastudy.fragments

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.camerastudy.R
import com.example.camerastudy.utils.GenericListAdapter

class SelectorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as RecyclerView

        view.apply {
            layoutManager = LinearLayoutManager(requireContext())

            val cameraManager =
                requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraList = enumerateCameras(cameraManager)

            val layoutId = android.R.layout.simple_list_item_1
            adapter = GenericListAdapter(cameraList, itemLayoutId = layoutId){ view, item, _ ->
                view.findViewById<TextView>(android.R.id.text1).text = item.title
                view.setOnClickListener {
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                        .navigate(R.id.camera_fragment)//, item.cameraId, item.format)
                    // todo argument 넘기는것 체크 필요
                }
            }
        }

    }

    companion object{
        /**
         * Helper class used as a data holder for each selectable camera format item
         * 선택 가능한 각 카메라 형식 항목에 대한 데이터 홀더로 사용되는 도우미 클래스
         * */
        private data class FormatItem(val title: String, val cameraId: String, val format: Int)

        /**
         * Helper function used to convert a lens orientation enum into a human-readable string
         * 렌즈 방향 열거형을 사람이 읽을 수 있는 문자열로 변환하는 데 사용되는 도우미 함수
         * */
        private fun lensOrientationString(value : Int)
            = when(value) {
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }


        /**
         * Helper function used to list all compatible cameras and supported pixel formats
         * 호환되는 모든 카메라 및 지원되는 픽셀 형식을 나열하는 데 사용되는 도우미 기능
         * */
        private fun enumerateCameras(cameraManager: CameraManager) : List<FormatItem>{
            val availableCameras : MutableList<FormatItem> = mutableListOf()

            // Get list of all compatible cameras : 호환되는 모든 카메라 목록 가져오기
            val cameraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

                capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
            }

            // Iterate over the list of cameras and return all the compatible ones
            // 카메라 목록을 반복하고 호환되는 모든 카메라를 반환합니다.
            cameraIds.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!
                )

                // Query the available capabilities and output formats
                // 사용 가능한 기능 및 출력 형식 쿼리
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )!!
                val outputFormats = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )!!.outputFormats

                //  All cameras *must* support JPEG output so we don't need to check characteristics
                //  모든 카메라는 JPEG 출력을 지원해야하므로 특성을 확인할 필요가 없습니다.
                availableCameras.add(
                    FormatItem("$orientation JPEG ($id)", id, ImageFormat.JPEG)
                )

                // Return cameras that support RAW capability
                // RAW 기능을 지원하는 리턴 카메라
                if(capabilities.contains( CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
                    && outputFormats.contains(ImageFormat.RAW_SENSOR)){

                    availableCameras.add(
                        FormatItem("$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR)
                    )
                }

                // Return cameras that support JPEG DEPTH capability
                // JPEG DEPTH 기능을 지원하는 카메라 반환
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)
                    && outputFormats.contains(ImageFormat.DEPTH_JPEG)) {

                    availableCameras.add(
                        FormatItem("$orientation DEPTH ($id)", id, ImageFormat.DEPTH_JPEG)
                    )
                }

            }

            return availableCameras
        }
    }

}