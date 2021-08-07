package com.example.camerastudy.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LiveData


/**
 * Calculates closest 90-degree orientation to compensate for the device
 * rotation relative to sensor orientation, i.e., allows user to see camera
 * frames with the expected orientation.
 * 센서 방향을 기준으로 장치 회전을 보상하기 위해 가장 가까운 90도 방향을 계산합니다.
 * 즉, 사용자가 예상 방향으로 카메라 프레임을 볼 수 있습니다.
 */
class OrientationLiveData(
    context: Context,
    characteristics: CameraCharacteristics
): LiveData<Int>() {

    private val listener = object : OrientationEventListener(context.applicationContext){
        override fun onOrientationChanged(orientation: Int) {
            val rotation = when {
                orientation <= 45 -> Surface.ROTATION_0
                orientation <= 135 -> Surface.ROTATION_90
                orientation <= 225 -> Surface.ROTATION_180
                orientation <= 315 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }
            val relative = computeRelativeRotation(characteristics, rotation)
            if(relative != value) postValue(relative)
        }

    }

    override fun onActive() {
        super.onActive()
        listener.enable()
    }

    override fun onInactive() {
        super.onInactive()
        listener.disable()
    }

    companion object{
        /**
         * Computes rotation required to transform from the camera sensor orientation to the
         * device's current orientation in degrees.
         * 카메라 센서 방향에서 장치의 현재 방향(도)으로 변환하는 데 필요한 회전을 계산합니다.
         *
         * @param characteristics the [CameraCharacteristics] to query for the sensor orientation.
         * @param은 센서 방향을 쿼리할 [CameraCharacteristics] 특성입니다.
         * @param surfaceRotation the current device orientation as a Surface constant
         * @param surfaceRotation 현재 장치 방향을 Surface 상수로 사용
         * @return the relative rotation from the camera sensor to the current device orientation.
         * @return 카메라 센서에서 현재 장치 방향으로의 상대 회전.
         */
        private fun computeRelativeRotation(characteristics : CameraCharacteristics, surfaceRotation : Int) : Int {
            val sensorOrientationDegrees =
                characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            val deviceOrientationDegrees = when(surfaceRotation){
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            // Reverse device orientation for front-facing cameras
            // 전면 카메라의 장치 방향 반전
            val sign = if(characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) 1 else -1

            // Calculate desired JPEG orientation relative to camera orientation to make
            // the image upright relative to the device orientation
            // 카메라 방향을 기준으로 원하는 JPEG 방향을 계산하여 장치 방향을 기준으로 이미지를 수직으로 만듭니다.
            return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360
        }
    }
}