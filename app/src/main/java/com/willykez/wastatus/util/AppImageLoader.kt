package com.willykez.wastatus.util

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

/**
 * A Coil [ImageLoader] with real video-frame decoding registered, so video
 * statuses show an actual extracted frame as their thumbnail instead of a
 * flat placeholder gradient.
 */
object AppImageLoader {
    @Volatile private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components { add(VideoFrameDecoder.Factory()) }
                .crossfade(true)
                .build()
                .also { instance = it }
        }
    }
}
