package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File
class GalleryActivity : AppCompatActivity(), MediaGridAdapter.OnItemClickListener {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mediaFiles: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs[0].absolutePath)
        val files = directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        mediaFiles = files

        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        val adapter = MediaGridAdapter(mediaFiles, this)
        binding.recyclerView.adapter = adapter

        binding.createNewContentBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onItemClick(position: Int) {
        // При клике на конкретный файл открываем полноэкранный просмотр
        val intent = Intent(this, FullScreenGalleryActivity::class.java)
        intent.putExtra("current_index", position)
        startActivity(intent)
    }
}
