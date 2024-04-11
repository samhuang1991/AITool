package com.southridge.aitool.superresolution

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
import com.southridge.aitool.BaseActivity
import com.southridge.aitool.R
import com.southridge.aitool.databinding.ActivityMainBinding
import com.southridge.aitool.databinding.ActivitySuperResolutionBinding
import com.southridge.aitool.ui.theme.AIToolTheme

class SuperResolutionActivity : BaseActivity<ActivitySuperResolutionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivitySuperResolutionBinding {
        return ActivitySuperResolutionBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_resolution)
    }


}
