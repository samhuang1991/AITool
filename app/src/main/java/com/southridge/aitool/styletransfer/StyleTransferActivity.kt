package com.southridge.aitool.styletransfer

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.graphics.BitmapFactory
import android.os.Bundle
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
import androidx.databinding.DataBindingUtil.setContentView
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityMainBinding
import com.southridge.aitool.databinding.ActivitySuperResolutionBinding
import com.southridge.aitool.databinding.ActivityTransferStyleBinding
import com.southridge.aitool.superresolution.Result
import com.southridge.aitool.superresolution.SuperResPerformer
import com.southridge.aitool.ui.theme.AIToolTheme
import java.io.InputStream

class StyleTransferActivity : BaseActivity<ActivityTransferStyleBinding>() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private var modelID: Int = R.raw.mosaic_8
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
        return assets.open("gorilla.png")
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
