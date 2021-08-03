package com.example.camerastudy.fragments

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

            // Iterate over the list of cameras and return all the compatible ones : 카메라 목록을 반복하고 호환되는 모든 카메라를 반환합니다.
            // todo 여기하고 있었다

            return availableCameras
        }
    }

}