package com.southridge.aitool.cartoon

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityPhotoToCartoonBinding
import com.southridge.aitool.superresolution.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class PhotoToCartoonActivity : BaseActivity<ActivityPhotoToCartoonBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private var modelID: Int = R.raw.photo2cartoon_weights
    private val REQUEST_WRITE_STORAGE = 112
    private var stylePosition: Int = 0
    override fun inflateBinding(inflater: LayoutInflater): ActivityPhotoToCartoonBinding {
        return ActivityPhotoToCartoonBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_to_cartoon)

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        ortSession = ortEnv.createSession(readModel(), sessionOptions)

        binding.imageView1.setImageBitmap(
            BitmapFactory.decodeStream(readInputImage())
        )

        binding.styleTransferButton.setOnClickListener {
            try {
                performCartoonTransfer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                modelID = styleFiles[position]
                stylePosition = position
                ortSession = ortEnv.createSession(readModel(), sessionOptions)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                modelID = styleFiles[0]
            }
        }

        //生成的图片长按保存
        binding.imageView2.setOnLongClickListener {

            // 检查是否已经授权存储权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 请求存储权限
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
            } else {
                // 已经有权限，保存图片
                saveImageToGallery()
            }
            true
        }

    }

    /**
     * 保存图片到手机相册
     */
    private fun saveImageToGallery() {
        val bitmap = (binding.imageView2.drawable as BitmapDrawable).bitmap
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        val contentResolver = contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                // 通知图库更新
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = uri
                }
                sendBroadcast(intent)
                showToast("图片保存成功")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    // 权限请求的回调
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被用户授予，保存图片
                    saveImageToGallery()
                } else {
                    // 权限被用户拒绝，可以在这里给出一些提示
                }
            }
        }
    }


    private fun performCartoonTransfer() {
        var styleTransferPerformer = CartoonTransferPerformer()

        // 显示ProgressBar
        binding.progressBar.visibility = View.VISIBLE

        // 在后台线程中执行风格转换
        GlobalScope.launch(Dispatchers.IO) {
            // 获取开始时间
            val startTime = System.currentTimeMillis()

            val result : Result = styleTransferPerformer.performCartoonTransfer(readInputImage(), ortEnv, ortSession)

            // 在主线程中更新UI
            withContext(Dispatchers.Main) {
                // 隐藏ProgressBar
                binding.progressBar.visibility = View.GONE
                updateUI(result)

                // 获取结束时间并计算运行时间
                val endTime = System.currentTimeMillis()
                val runTime = endTime - startTime
                // 显示运行时间
                showToast("运行时间：$runTime 毫秒")
            }
        }
    }

    private fun readModel(): ByteArray {
//        val modelID = R.raw.rain_princess_8
        return resources.openRawResource(modelID).readBytes()
    }


    private fun readInputImage(): InputStream {
//        return assets.open("test_superresolution.png")
//        return assets.open("gorilla.png")
        return assets.open("cartoon_test.jpg")
//        return assets.open("man.png")
//        return assets.open("boat.jpg")
//        return assets.open("boat.jpg")
//        return assets.open("wood_house.png")
//        return assets.open("road.jpg")
    }

    private fun updateUI(result: Result) {
        binding.imageView2.setImageBitmap(result.outputBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        ortEnv.close()
        ortSession.close()
    }
    companion object {
        const val TAG = "PhotoToCartoonActivity"
    }

    private val styleFiles = intArrayOf(
//        R.raw.photo2cartoon_weights
//        R.raw.face_paint_512_v2
//        R.raw.generator_celeba_distill
//        R.raw.face_paint_1024_v2
        R.raw.generator_celeba_distill_1024
    )
}
