package com.davidogrady.irishexchange.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream


class ImageCompressor {

    @Suppress("DEPRECATION")
     fun getCompressedImageAsByteArray(oldUri: Uri, contentResolver: ContentResolver) : ByteArray {

        val bitmap: Bitmap
        val baos = ByteArrayOutputStream()

        // Android Q support and above for rendering bitmap image
        if (Build.VERSION.SDK_INT > 28){
            val source: ImageDecoder.Source = ImageDecoder.createSource(contentResolver, oldUri)
            bitmap = ImageDecoder.decodeBitmap(source)
        } else{
            // all android versions below
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, oldUri)
        }

        val targetWidth = 640 // arbitrary fixed limit
        val targetHeight =
            (bitmap.height * targetWidth / bitmap.width)

        val newBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        return baos.toByteArray()
    }
}