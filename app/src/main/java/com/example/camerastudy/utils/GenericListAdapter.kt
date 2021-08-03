package com.example.camerastudy.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalStateException

/**
 * Type helper used for the callback triggered once our view has been bound
 * 뷰가 바인딩되면 트리거되는 콜백에 사용되는 유형 도우미
 * */
typealias BindCallback<T> = (view : View, data : T, position : Int) -> Unit

class GenericListAdapter<T>(
    private val dataset : List<T>,
    private val itemLayoutId : Int? = null,
    private val itemViewFactory : (() -> View)? = null,
    private val onBind : BindCallback<T>
) : RecyclerView.Adapter<GenericListAdapter.GenericListViewHolder>() {

    class GenericListViewHolder(val view : View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        GenericListViewHolder(
            when {
                itemViewFactory != null -> itemViewFactory.invoke()
                itemLayoutId != null -> {
                    LayoutInflater.from(parent.context).inflate(itemLayoutId, parent, false)
                }
                else -> {
                    throw IllegalStateException("레이아웃 ID 또는 뷰 팩토리는 null 이 아니어야 합니다.")
                }
            }
        )

    override fun onBindViewHolder(holder: GenericListViewHolder, position: Int) {
        if(position < 0 || position > dataset.size) return
        onBind(holder.view, dataset[position], position)
    }

    override fun getItemCount() = dataset.size
}