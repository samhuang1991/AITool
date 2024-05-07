package com.southridge.aitool.cartoon

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
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityPhotoToCartoonBinding
import com.southridge.aitool.styletransfer.StyleTransferActivity
import com.southridge.aitool.styletransfer.StyleTransferActivity.Companion.PERMISSION_CODE
import com.southridge.aitool.superresolution.Result
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

class PhotoToCartoonActivity : BaseActivity<ActivityPhotoToCartoonBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private var modelID: Int = R.raw.photo2cartoon_weights
    private var stylePosition: Int = 0
    companion object {
        const val IMAGE_PICK_CODE = 1000
        const val PERMISSION_CODE = 1001
        const val REQUEST_WRITE_STORAGE = 112
        const val IMAGE_NAME = "road.jpg"
    }
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

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, StyleTransferActivity.IMAGE_PICK_CODE)
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
        if (resultCode == RESULT_OK && requestCode == StyleTransferActivity.IMAGE_PICK_CODE) {
            binding.imageView1.setImageURI(data?.data)
            //获取宽高
            binding.sizeTextView.text = "${binding.imageView1.drawable.intrinsicWidth} x ${binding.imageView1.drawable.intrinsicHeight}"
        }
    }

    // 权限请求的回调
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            StyleTransferActivity.REQUEST_WRITE_STORAGE -> {
                if (hasWriteExternalStoragePermission()) {
                    saveImageToGallery()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            StyleTransferActivity.PERMISSION_CODE -> {
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
        //从imageView1中获取InputStream图片返回
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
//        R.raw.photo2cartoon_weights
//        R.raw.face_paint_512_v2
//        R.raw.generator_celeba_distill
//        R.raw.face_paint_1024_v2
//        R.raw.generator_celeba_distill_1024
        R.raw.generator_paprika_1024
    )
}
