package com.southridge.aitool.styletransfer

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class StyleTransferActivity : BaseActivity<ActivityTransferStyleBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private var modelID: Int = R.raw.mosaic_8
    private var stylePosition: Int = 0
    companion object {
        const val IMAGE_PICK_CODE = 1000
        const val PERMISSION_CODE = 1001
        const val REQUEST_WRITE_STORAGE = 112
        const val IMAGE_NAME = "road.jpg"
    }

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
            BitmapFactory.decodeStream(assets.open(IMAGE_NAME))
        )

        //获取宽高
        binding.sizeTextView.text = "${binding.imageView1.drawable.intrinsicWidth} x ${binding.imageView1.drawable.intrinsicHeight}"

        binding.imageView1.setOnClickListener {
            //TODO 选择图片
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissions(permissions, PERMISSION_CODE)
            } else {
                pickImageFromGallery()
            }
        }

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

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
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

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            binding.imageView1.setImageURI(data?.data)
            //获取宽高
            binding.sizeTextView.text = "${binding.imageView1.drawable.intrinsicWidth} x ${binding.imageView1.drawable.intrinsicHeight}"
        }
    }

    // 权限请求的回调
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if (hasWriteExternalStoragePermission()) {
                    saveImageToGallery()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_CODE -> {
                if (hasReadExternalStoragePermission()) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hasWriteExternalStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadExternalStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun performStyleTransfer() {
        var styleTransferPerformer = StyleTransferPerformer()

        // 显示ProgressBar
        binding.progressBar.visibility = View.VISIBLE

        // 在后台线程中执行风格转换
        GlobalScope.launch(Dispatchers.IO) {
            // 获取开始时间
            val startTime = System.currentTimeMillis()

            var result : Result = if (stylePosition>= 5){
                styleTransferPerformer.performStyleTransfer2(readInputImage(), ortEnv, ortSession)
            }else{
                styleTransferPerformer.performStyleTransfer(readInputImage(), ortEnv, ortSession)
            }

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
        val bitmap = (binding.imageView1.drawable as BitmapDrawable).bitmap
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return ByteArrayInputStream(byteArray)
    }

    private fun updateUI(result: Result) {
        binding.imageView2.setImageBitmap(result.outputBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        ortEnv.close()
        ortSession.close()
    }

    private val styleFiles = intArrayOf(
        R.raw.mosaic_8,
        R.raw.udnie_8,
        R.raw.candy_8,
        R.raw.rain_princess_8,
        R.raw.pointilism_8,
        R.raw.animeganv3_hayao_36,
        R.raw.shinkai_53
    )
}
