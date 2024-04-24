package com.southridge.aitool.styletransfer

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.abs
import kotlin.math.log

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
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // Convert the resized bitmap to a float array
        val floatArray = bitmapToFloatArray(resizedBitmap)

        // Convert float array to FloatBuffer
//        val floatBuffer = FloatBuffer.allocate(floatArray.size)
//        floatArray.forEach { floatBuffer.put(it) }
//        floatBuffer.flip()
        val floatBuffer =
            ByteBuffer.allocateDirect(floatArray.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        floatBuffer.put(floatArray).flip()

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


    /**
     * 转换卡通的效果
     */
    fun performStyleTransfer2(
        inputStream: InputStream,
        ortEnv: OrtEnvironment,
        ortSession: OrtSession
    ): com.southridge.aitool.superresolution.Result {
        var result = com.southridge.aitool.superresolution.Result()

        // Step 1: convert image into Bitmap
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Resize the bitmap to the expected input size (e.g., 224x224)
        //宽度和高度从bitmap图片获取
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

        // Convert the resized bitmap to a float array
        val floatArray = bitmapToFloatArrayHWC(resizedBitmap)

        val floatBuffer =
            ByteBuffer.allocateDirect(floatArray.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        floatBuffer.put(floatArray).flip()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(1, 640, 640, 3)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            floatBuffer,
            shape
        )
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap("generator_input:0", inputTensor))

            // Step 4: output analysis
            output.use {

                val rawOutput = (output?.get(0)?.value) as Array<Array<Array<FloatArray>>>

                val outputImageBitmap = floatArrayToBitmap2(rawOutput)

                // Step 5: set output result
                result.outputBitmap = outputImageBitmap
            }
        }
        return result
    }

    private fun bitmapToFloatArrayHWC(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val floatArray = FloatArray(1 * 3 * width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val rFloat = r.toFloat() / 255.0f
                val gFloat = g.toFloat() / 255.0f
                val bFloat = b.toFloat() / 255.0f

                val index = (y * width + x) * 3
                floatArray[index] = rFloat
                floatArray[index + 1] = gFloat
                floatArray[index + 2] = bFloat
            }
        }
        return floatArray
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val floatArray = FloatArray(1 * 3 * width * height)

        // 遍历Bitmap的每个像素
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // 将0-255的整数转换为0.0-1.0的浮点数
                val rFloat = r.toFloat() / 255.0f
                val gFloat = g.toFloat() / 255.0f
                val bFloat = b.toFloat() / 255.0f

                // 将浮点数存储到floatArray中，按照CHW格式
                val index = y * width + x
                floatArray[index] = rFloat
                floatArray[index + width * height] = gFloat
                floatArray[index + 2 * width * height] = bFloat
            }
        }

        return floatArray
    }

    private fun floatArrayToBitmap(data: Array<Array<Array<FloatArray>>>): Bitmap {
        val width = data[0][0][0].size
        val height = data[0][0].size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (i in 0 until height) {
            for (j in 0 until width) {
                var r = abs(data[0][0][i][j] * 1).toInt()
                var g = abs(data[0][1][i][j] * 1).toInt()
                var b = abs(data[0][2][i][j] * 1).toInt()
                if (r>255 ){
                    r = 255
                }
                if (g>255){
                    g = 255
                }
                if (b>255){
                    b = 255
                }
                bitmap.setPixel(j, i, Color.rgb(r, g, b))
//                Log.i("output: ", "r = $r , g = $g , b = $b")
            }
        }
        return bitmap
    }

    private fun floatArrayToBitmap2(data: Array<Array<Array<FloatArray>>>): Bitmap {
        val width = data[0].size
        val height = data[0][0].size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {

                val r = (data[0][y][x][0] * 255).coerceIn(0f, 255f).toInt()
                val g = (data[0][y][x][1] * 255).coerceIn(0f, 255f).toInt()
                val b = (data[0][y][x][2] * 255).coerceIn(0f, 255f).toInt()
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bitmap
    }

}