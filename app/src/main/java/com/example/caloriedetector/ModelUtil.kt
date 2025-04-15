package com.example.caloriedetector

import android.graphics.Bitmap
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter


fun loadModelFile(context: android.content.Context): Interpreter {

    val fileDescriptor = context.assets.openFd("food101_model.tflite")
    val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = fileInputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength


    val byteBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)


    return Interpreter(byteBuffer)
}


fun preprocessImage(bitmap: Bitmap): ByteBuffer {
    val IMAGE_SIZE = 224
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)

    // ByteBuffer ile görüntüyü saklamak için bellek ayırıyoruz
    val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
    byteBuffer.order(ByteOrder.nativeOrder())

    val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
    resizedBitmap.getPixels(pixels, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)


    for (pixel in pixels) {
        byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
        byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
        byteBuffer.putFloat(((pixel and 0xFF)) / 255.0f)
    }

    return byteBuffer
}


