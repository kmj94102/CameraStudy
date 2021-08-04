package com.example.camerastudy.utils

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import kotlin.math.roundToInt

/**
 * A [SurfaceView] that can be adjusted to a specified aspect ratio and
 * performs center-crop transformation of input frames.
 * 지정된 종횡비로 조정할 수 있고 입력 프레임의 중앙 자르기 변환을 수행하는 [SurfaceView]입니다.
 */
class AutoFitSurfaceView @JvmOverloads constructor(
    context : Context,
    attrs : AttributeSet?= null,
    defStyle : Int = 0
) : SurfaceView(context, attrs, defStyle) {

    private var aspectRatio = 0f

    /**
     * Sets the aspect ratio for this view. The size of the view will be
     * measured based on the ratio calculated from the parameters.
     * 이 보기의 종횡비를 설정합니다. 뷰의 크기는 매개변수에서 계산된 비율을 기반으로 측정됩니다.
     *
     * @param width  카메라 해상도 가로 크기  Camera resolution horizontal size
     * @param height 카메라 해상도 세로 크기 Camera resolution vertical size
     */
    fun setAspectRatio(width : Int, height : Int){
        require(width > 0 && height > 0) { "Size cannot be negative" }
        aspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if(aspectRatio == 0f){
            setMeasuredDimension(width, height)
        } else{
            // Performs center-crop transformation of the camera frames
            // 카메라 프레임의 중앙 자르기 변환을 수행합니다.
            val newWidth : Int
            val newHeight : Int
            val actualRatio = if(width > height) aspectRatio else 1f / aspectRatio

            if(width < height * actualRatio){
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }

            Log.d(TAG, "Measured dimensions set : $newWidth x $newHeight")
            setMeasuredDimension(newWidth, newHeight)
        }

    }

    companion object {
        private val TAG = AutoFitSurfaceView::class.java.simpleName
    }

}