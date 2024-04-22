package com.southridge.aitool

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    lateinit var binding: T

    override fun setContentView(layoutResID: Int) {
        // 在setContentView中初始化ViewBinding
        binding = inflateBinding(layoutInflater)
        // 将生成的View设置给Activity
        super.setContentView(binding.root)
    }

    abstract fun inflateBinding(inflater: LayoutInflater): T
    fun showToast(s: String){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

}