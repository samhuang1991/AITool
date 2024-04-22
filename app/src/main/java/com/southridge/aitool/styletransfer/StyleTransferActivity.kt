package com.southridge.aitool.styletransfer

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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil.setContentView
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityMainBinding
import com.southridge.aitool.databinding.ActivitySuperResolutionBinding
import com.southridge.aitool.databinding.ActivityTransferStyleBinding
import com.southridge.aitool.superresolution.Result
import com.southridge.aitool.superresolution.SuperResPerformer
import com.southridge.aitool.ui.theme.AIToolTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class StyleTransferActivity : BaseActivity<ActivityTransferStyleBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private var modelID: Int = R.raw.mosaic_8
    private val REQUEST_WRITE_STORAGE = 112
    override fun inflateBinding(inflater: LayoutInflater): ActivityTransferStyleBinding {
        return ActivityTransferStyleBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_style)

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        ortSession = ortEnv.createSession(readModel(), sessionOptions)

        binding.imageView1.setImageBitmap(
            BitmapFactory.decodeStream(readInputImage())
        );

        binding.styleTransferButton.setOnClickListener {
            try {
                performStyleTransfer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                modelID = styleFiles[position]
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
                    // 权限被用户拒绝，你可以在这里给出一些提示
                }
            }
        }
    }


    private fun performStyleTransfer() {
        var styleTransferPerformer = StyleTransferPerformer()
        var result = styleTransferPerformer.performStyleTransfer(readInputImage(), ortEnv, ortSession)
        updateUI(result);
    }

    private fun readModel(): ByteArray {
//        val modelID = R.raw.rain_princess_8
        return resources.openRawResource(modelID).readBytes()
    }


    private fun readInputImage(): InputStream {
//        return assets.open("test_superresolution.png")
//        return assets.open("gorilla.png")
        return assets.open("baozi.jpg")
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
        const val TAG = "ORTSuperResolution"
    }

    private val styleFiles = intArrayOf(
        R.raw.mosaic_8,
        R.raw.mosaic_9,
        R.raw.udnie_8,
        R.raw.udnie_9,
        R.raw.candy_8,
        R.raw.candy_9,
        R.raw.rain_princess_8,
        R.raw.rain_princess_9,
        R.raw.pointilism_8,
        R.raw.pointilism_9,
        R.raw.animeganv3_hayao_36
    )
}
