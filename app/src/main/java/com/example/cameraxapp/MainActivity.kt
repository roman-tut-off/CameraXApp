package com.example.cameraxapp

import android.content.pm.PackageManager
import android.graphics.Camera
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.cameraxapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDir: File
    private var imageCapture: ImageCapture? = null
    private var cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getPermission()

        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDir()

        startCamera()

        binding.stopVideo.visibility = View.GONE

        binding.startVideo.setOnClickListener {
            binding.startVideo.visibility= View.GONE
            binding.stopVideo.visibility= View.VISIBLE
        }

        binding.stopVideo.setOnClickListener {
            binding.stopVideo.visibility= View.GONE
            binding.startVideo.visibility= View.VISIBLE
        }

        binding.flip.setOnClickListener{
            if(cameraFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraFacing = CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                cameraFacing = CameraSelector.DEFAULT_BACK_CAMERA
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.isShutdown
    }

    private fun getOutputDir(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let { mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun startCamera() {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = cameraFacing

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider
                    .bindToLifecycle(this, cameraSelector, preview, imageCapture)

                binding.capture.setOnClickListener{
                    val photoFile = File(outputDir, SimpleDateFormat(
                        Constants.FILE_NAME_FORMAT, Locale.getDefault())
                        .format(System.currentTimeMillis()) + ".png"
                    )
                    val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture!!.takePicture(outputOption, ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback{
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = Uri.fromFile(photoFile)
                            val msg = "Фото сохранено в $savedUri"
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(Constants.TAG, "Ошибка записи файла: ${exception.message}", exception)
                        }

                    })
                }

                binding.toggleFlash.setOnClickListener {
                    if (camera.cameraInfo.hasFlashUnit()) {
                        if (camera.cameraInfo.torchState.value == 0) {
                            camera.cameraControl.enableTorch(true)
                            binding.toggleFlash.setImageResource(R.drawable.ic_baseline_flashlight_off)

                        } else {
                            camera.cameraControl.enableTorch(false)
                            binding.toggleFlash.setImageResource(R.drawable.ic_baseline_flashlight_on)
                        }
                    }
                    else {
                        runOnUiThread{Toast.makeText(this@MainActivity, "Подцветка не доступна", Toast.LENGTH_SHORT)}
                    }

                }

            } catch (e: Exception) {
                Log.d(Constants.TAG, "Ошибка запуска камеры: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getPermission() {
        val permissionList = mutableListOf<String>()
        Constants.REQUIRED_PERMISSION.forEach {
            if(ContextCompat.checkSelfPermission(this, it)
                !=PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(it)
        }

        }
        if(permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), Constants.REQUEST_CODE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }



    }
}