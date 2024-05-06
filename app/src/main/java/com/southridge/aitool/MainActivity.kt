package com.southridge.aitool

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import com.southridge.aitool.cartoon.PhotoToCartoonActivity
import com.southridge.aitool.databinding.ActivityMainBinding
import com.southridge.aitool.styletransfer.StyleTransferActivity
import com.southridge.aitool.superresolution.SuperResolutionActivity
import com.southridge.aitool.ui.theme.AIToolTheme
import java.io.InputStream

class MainActivity : BaseActivity<ActivityMainBinding>() {


    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleListener()
    }


    private fun handleListener() {
        binding.btnDiffusion.setOnClickListener {
            // startActivity(Intent(this, DiffusionActivity::class.java))
        }

        binding.btnPlugins.setOnClickListener {
            // startActivity(Intent(this, PluginActivity::class.java))
        }

        binding.btnLoRA.setOnClickListener {
            // startActivity(Intent(this, LoRAWeightActivity::class.java))
        }

        binding.btnSuperResolution.setOnClickListener {
            startActivity(Intent(this,SuperResolutionActivity::class.java))
        }

        binding.styleTransfer.setOnClickListener {
             startActivity(Intent(this,StyleTransferActivity::class.java))
        }

        binding.cartoonTransfer.setOnClickListener {
             startActivity(Intent(this, PhotoToCartoonActivity::class.java))
        }
    }



}
