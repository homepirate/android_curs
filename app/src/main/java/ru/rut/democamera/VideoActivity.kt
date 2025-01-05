package ru.rut.democamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityVideoBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var recording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraExecutor: ExecutorService

    private val TAG = "VideoActivity"

    // Разрешения, которые мы будем запрашивать
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // Ланчер для запроса разрешений
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraPermission = permissions[Manifest.permission.CAMERA] ?: false
            val audioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraPermission) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
            }

            if (!audioPermission) {
                Toast.makeText(this, "Audio permission not granted. Video will have no sound.", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Запрашиваем необходимые разрешения
        requestPermissions()

        binding.recordVideoBtn.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        binding.switchCamBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.galleryBtn.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.switchModeBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissions() {
        // Проверяем, предоставлены ли все разрешения
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            // Запрашиваем отсутствующие разрешения
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // Если все разрешения уже предоставлены, запускаем камеру
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording() {
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val file = File(externalMediaDirs[0], name)
        val outputOptions = FileOutputOptions.Builder(file).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                } else {
                    // Разрешение на запись аудио не предоставлено
                    Toast.makeText(this@VideoActivity, "Audio permission not granted. Recording without sound.", Toast.LENGTH_SHORT).show()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            Toast.makeText(this, "Error: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Video saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                        }
                        recording = null
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
