package com.southridge.aitool.styletransfer

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.southridge.aitool.utils.BitmapUtils.bitmapToFloatArrayCHW
import com.southridge.aitool.utils.BitmapUtils.bitmapToFloatArrayWHC
import com.southridge.aitool.utils.BitmapUtils.floatArrayCHWToBitmap
import com.southridge.aitool.utils.BitmapUtils.floatArrayHWCToBitmap
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

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
        val shape = longArrayOf(1, 3, 224, 224)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            floatBuffer,
            shape
        )
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap(ortSession.inputNames.first(), inputTensor))

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
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)

        // Convert the resized bitmap to a float array
        val floatArray = bitmapToFloatArrayWHC(resizedBitmap)

        val floatBuffer =
            ByteBuffer.allocateDirect(floatArray.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        floatBuffer.put(floatArray).flip()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(1, bitmap.width.toLong(), bitmap.height.toLong(), 3)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            floatBuffer,
            shape
        )
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap(ortSession.inputNames.first(), inputTensor))

            // Step 4: output analysis
            output.use {

                val rawOutput = (output?.get(0)?.value) as Array<Array<Array<FloatArray>>>

                val outputImageBitmap = floatArrayHWCToBitmap(rawOutput)

                // Step 5: set output result
                result.outputBitmap = outputImageBitmap
            }
        }
        return result
    }




}