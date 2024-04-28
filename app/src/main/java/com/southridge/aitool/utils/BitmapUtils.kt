package com.southridge.aitool.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object BitmapUtils {
    fun bitmapToFloatArrayHWC(bitmap: Bitmap): FloatArray {
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

    fun bitmapToFloatArrayWHC(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val floatArray = FloatArray(1 * 3 * width * height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val rFloat = r.toFloat() / 255.0f
                val gFloat = g.toFloat() / 255.0f
                val bFloat = b.toFloat() / 255.0f

                val index = (x * height + y) * 3
                floatArray[index] = rFloat
                floatArray[index + 1] = gFloat
                floatArray[index + 2] = bFloat
            }
        }
        return floatArray
    }

    fun bitmapToFloatArrayCHW(bitmap: Bitmap): FloatArray {
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

    fun floatArrayCHWToBitmap(data: Array<Array<Array<FloatArray>>>): Bitmap {
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

    fun floatArrayHWCToBitmap(data: Array<Array<Array<FloatArray>>>): Bitmap {
        val width = data[0].size
        val height = data[0][0].size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (data[0][x][y][0] * 255).coerceIn(0f, 255f).toInt()
                val g = (data[0][x][y][1] * 255).coerceIn(0f, 255f).toInt()
                val b = (data[0][x][y][2] * 255).coerceIn(0f, 255f).toInt()
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bitmap
    }

}