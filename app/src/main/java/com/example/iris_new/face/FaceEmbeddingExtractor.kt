package com.example.iris_new.face

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FaceEmbeddingExtractor(context: Context) {

    private val interpreter: Interpreter//brain behind the neural network

    init {
        interpreter = Interpreter(loadModelFile(context))/*This runs when object created.

        Steps:

        load model file

        create TensorFlow interpreter

        ready to run predictions*/
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val fileDescriptor = context.assets.openFd("mobilefacenet.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    fun extract(bitmap: Bitmap): FloatArray {
        val input = ByteBuffer.allocateDirect(112 * 112 * 3 * 4)
        input.order(ByteOrder.nativeOrder())

        val resized = Bitmap.createScaledBitmap(bitmap, 112, 112, true)

        for (y in 0 until 112) {
            for (x in 0 until 112) {
                val px = resized.getPixel(x, y)
                input.putFloat(((px shr 16 and 0xFF) - 128) / 128f)
                input.putFloat(((px shr 8 and 0xFF) - 128) / 128f)
                input.putFloat(((px and 0xFF) - 128) / 128f)
            }
        }

        val output = Array(1) { FloatArray(128) }
        interpreter.run(input, output)
        return output[0]
    }
}
