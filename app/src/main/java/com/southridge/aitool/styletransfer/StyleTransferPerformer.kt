package com.southridge.aitool.styletransfer

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.Collections

internal data class Result(
    var outputBitmap: Bitmap? = null
) {}

internal class StyleTransferPerformer {

    fun performStyleTransfer(
        inputStream: InputStream,
        ortEnv: OrtEnvironment,
        ortSession: OrtSession
    ): com.southridge.aitool.superresolution.Result {
        var result = com.southridge.aitool.superresolution.Result()

        // Step 1: convert image into Bitmap
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Resize the bitmap to the expected input size (e.g., 224x224)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false)

        // Convert the resized bitmap to a float array
        val floatArray = bitmapToFloatArray(resizedBitmap)

        // Convert float array to FloatBuffer
        val floatBuffer = FloatBuffer.allocate(floatArray.size)
        floatArray.forEach { floatBuffer.put(it) }
        floatBuffer.flip()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(1, 3, 224, 224)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            floatBuffer,
            shape
        )
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap("input1", inputTensor))

            // Step 4: output analysis
            output.use {
//                val rawOutput = (output?.get(0)?.value) as ByteArray
//                val outputImageBitmap =
//                    byteArrayToBitmap(rawOutput)

                val rawOutput = (output?.get(0)?.value) as Array<Array<Array<FloatArray>>>
                val outputImageBitmap = floatArrayToBitmap(rawOutput)

                // Step 5: set output result
                result.outputBitmap = outputImageBitmap
            }
        }
        return result
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val intValues = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val floatValues = FloatArray(intValues.size * 3)
        for (i in intValues.indices) {
            val value = intValues[i]
            floatValues[i * 3] = ((value shr 16) and 0xFF) / 255.0f
            floatValues[i * 3 + 1] = ((value shr 8) and 0xFF) / 255.0f
            floatValues[i * 3 + 2] = (value and 0xFF) / 255.0f
        }
        return floatValues
    }

    private fun floatArrayToBitmap(data: Array<Array<Array<FloatArray>>>): Bitmap {
    val width = data[0][0][0].size
    val height = data[0][0].size
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (i in 0 until height) {
        for (j in 0 until width) {
            val r = (data[0][0][i][j] * 255).toInt()
            val g = (data[0][1][i][j] * 255).toInt()
            val b = (data[0][2][i][j] * 255).toInt()
            bitmap.setPixel(j, i, Color.rgb(r, g, b))
        }
    }
    return bitmap
}
}