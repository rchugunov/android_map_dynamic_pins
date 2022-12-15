package com.rchugunov.dynamicpinsview.map

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import com.rchugunov.dynamicpinsview.R
import com.rchugunov.dynamicpinsview.databinding.ViewMapMarkerBinding

class MapMarkerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewMapMarkerBinding.inflate(LayoutInflater.from(context)) }

    init {
        addView(binding.root)
    }

    fun setContent(
        circle: CircleContent,
        title: String?,
        @ColorInt pinColor: Int,
    ) {

        when (circle) {
            is CircleContent.Cluster -> {
                binding.mapMarkerViewClusterText.isVisible = true
                binding.mapMarkerViewClusterText.text = circle.count.toString()
                binding.mapMarkerViewIcon.setImageResource(R.drawable.white_circle)
            }

            is CircleContent.Marker -> {
                binding.mapMarkerViewClusterText.isVisible = false
                val icon = circle.mapMarkerIcon
                val drawable = getIconDrawable(markerIcon = icon)
                binding.mapMarkerViewIcon.setImageDrawable(drawable)
            }
        }

        binding.mapMarkerViewPin.imageTintList = ColorStateList.valueOf(pinColor)
        binding.mapMarkerViewTitle.text = title
    }

    private fun getIconDrawable(
        markerIcon: MapMarker.Icon,
    ): Drawable? {

        val drawable = when (markerIcon) {
            is MapMarker.Icon.BitmapIcon -> {
                RoundedBitmapDrawableFactory.create(resources, markerIcon.image).apply {
                    isCircular = true
                    cornerRadius = Math.max(markerIcon.image.width, markerIcon.image.height) / 2.0f
                }
            }

            is MapMarker.Icon.Placeholder -> {
                // This view doesn't support images which can be loaded asynchronously
                // UrlIcon should be loaded with MapMarkerIconsLoader and converted to either of two above
                null
            }
        }
        return drawable
    }

    sealed interface CircleContent {

        data class Cluster(
            val count: Int,
        ) : CircleContent

        data class Marker(
            val mapMarkerIcon: MapMarker.Icon,
        ) : CircleContent
    }
}