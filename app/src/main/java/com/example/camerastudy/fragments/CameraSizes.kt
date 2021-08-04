package com.example.camerastudy.fragments

import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Display
import kotlin.math.max
import kotlin.math.min

/**
 * Helper class used to pre-compute shortest and longest sides of a [Size]
 * [Size]의 가장 짧은 변과 가장 긴 변을 미리 계산하는 데 사용되는 도우미 클래스
 * */
class SmartSize(width : Int, height : Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

/**
 * Standard High Definition size for pictures and video
 * 사진 및 비디오용 표준 고화질 크기
 * */
val SIZE_1080P : SmartSize = SmartSize(1920, 1080)

/**
 * Returns a [SmartSize] object for the given [Display]
 * 주어진 [Display]에 대한 [SmartSize] 개체를 반환합니다.
 * */
fun getDisplaySmartSize(display : Display) : SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * Returns the largest available PREVIEW size. For more information, see:
 * 사용 가능한 가장 큰 PREVIEW 크기를 반환합니다. 자세한 내용은
 * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
 * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
 */
fun <T>getPreviewOutputSize(
    display: Display,
    characteristics : CameraCharacteristics,
    targetClass: Class<T>,
    format : Int? = null
) : Size {
    // Find which is smaller: screen or 1080p
    // 화면 또는 1080p 중 더 작은 것 찾기
    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if(hdScreen) SIZE_1080P else screenSize

    // If image format is provided, use it to determine supported sizes; else use target class
    // 이미지 형식이 제공되면 이를 사용하여 지원되는 크기를 결정합니다. 그렇지 않으면 대상 클래스를 사용하십시오.
    val config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

    if (format == null)
        assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
    else
        assert(config.isOutputSupportedFor(format))

    val allSizes = if(format == null) config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // Get available sizes and sort them by area from largest to smallest
    // 사용 가능한 크기를 가져오고 영역별로 가장 큰 것부터 작은 것까지 정렬
    val validSizes = allSizes
        .sortedWith(compareBy { it.height * it.width })
        .map { SmartSize(it.width, it.height) }
        .reversed()

    // Then, get the largest output size that is smaller or equal than our max size
    // 그런 다음 최대 크기보다 작거나 같은 가장 큰 출력 크기를 가져옵니다.
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}