package com.southridge.aitool.superresolution

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.databinding.DataBindingUtil.setContentView
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityMainBinding
import com.southridge.aitool.databinding.ActivitySuperResolutionBinding
import com.southridge.aitool.ui.theme.AIToolTheme
import java.io.InputStream

class SuperResolutionActivity : BaseActivity<ActivitySuperResolutionBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    override fun inflateBinding(inflater: LayoutInflater): ActivitySuperResolutionBinding {
        return ActivitySuperResolutionBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_resolution)

        binding.imageView1.setImageBitmap(
            BitmapFactory.decodeStream(readInputImage())
        );

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        ortSession = ortEnv.createSession(readModel(), sessionOptions)

        binding.superResolutionButton?.setOnClickListener {
            try {
                performSuperResolution(ortSession)
                Toast.makeText(baseContext, "Super resolution performed!", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught when perform super resolution", e)
                Toast.makeText(baseContext, "Failed to perform super resolution", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun readModel(): ByteArray {
        val modelID = R.raw.pytorch_superresolution_with_pre_post_processing_op18
        return resources.openRawResource(modelID).readBytes()
    }

    private fun readInputImage(): InputStream {
        return assets.open("test_superresolution.png")
    }

    private fun performSuperResolution(ortSession: OrtSession) {
        var superResPerformer = SuperResPerformer()
        var result = superResPerformer.upscale(readInputImage(), ortEnv, ortSession)
        updateUI(result);
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
}
