package com.southridge.aitool.cartoon

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.southridge.aitool.utils.BitmapUtils.bitmapToFloatArrayCHW
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.math.abs


internal data class Result(
    var outputBitmap: Bitmap? = null
) {}

internal class CartoonTransferPerformer{

    fun performCartoonTransfer(
        inputStream: InputStream,
        ortEnv: OrtEnvironment,
        ortSession: OrtSession
    ): com.southridge.aitool.superresolution.Result {
        var result = com.southridge.aitool.superresolution.Result()

        // Step 1: convert image into Bitmap
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Resize the bitmap to the expected input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true)

        // Convert the resized bitmap to a float array
        val floatArray = bitmapToFloatArrayCHW(resizedBitmap)

        // Convert float array to FloatBuffer
//        val floatBuffer = FloatBuffer.allocate(floatArray.size)
//        floatArray.forEach { floatBuffer.put(it) }
//        floatBuffer.flip()
        val floatBuffer =
            ByteBuffer.allocateDirect(floatArray.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        floatBuffer.put(floatArray).flip()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(1, 3, 1024, 1024)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            floatBuffer,
            shape
        )
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output =
                ortSession.run(Collections.singletonMap(ortSession.inputNames.first(), inputTensor))

            // Step 4: output analysis
            output.use {
//                val rawOutput = (output?.get(0)?.value) as ByteArray
//                val outputImageBitmap =
//                    byteArrayToBitmap(rawOutput)

                val rawOutput = (output?.get(0)?.value) as Array<Array<Array<FloatArray>>>

                val outputImageBitmap = floatArrayCHWToBitmap(rawOutput)

                // Step 5: set output result
                result.outputBitmap = outputImageBitmap
            }
        }
        return result
    }
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        var h = bitmap.height
        var w = bitmap.width

        val x = h % 32
        h -= x

        val y = w % 32
        w -= y

        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }


    private fun floatArrayCHWToBitmap(data: Array<Array<Array<FloatArray>>>): Bitmap {
        val width = data[0][0][0].size
        val height = data[0][0].size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (i in 0 until height) {
            for (j in 0 until width) {
                var r = abs(data[0][0][i][j].coerceIn(0.0f,1.0f)  * 255).toInt()
                var g = abs(data[0][1][i][j].coerceIn(0.0f,1.0f)  * 255).toInt()
                var b = abs(data[0][2][i][j].coerceIn(0.0f,1.0f)  * 255).toInt()
                bitmap.setPixel(j, i, Color.rgb(r, g, b))
//                Log.i("output: ", "r = $r , g = $g , b = $b")
            }
        }
        return bitmap
    }

}