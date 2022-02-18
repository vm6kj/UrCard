package com.kc_hsu.urcard

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

fun Bitmap.toByteArray(): ByteArray {
    // val buf = ByteBuffer.allocate(byteCount)
    //     copyPixelsToBuffer(buf)
    //     return buf.array()

    ByteArrayOutputStream().apply {
        compress(Bitmap.CompressFormat.JPEG, 100, this)
        return toByteArray()
    }
}