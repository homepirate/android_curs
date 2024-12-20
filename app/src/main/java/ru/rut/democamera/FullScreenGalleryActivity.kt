package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.rut.democamera.databinding.ActivityFullscreenGalleryBinding
import java.io.File

class FullScreenGalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullscreenGalleryBinding
    private lateinit var files: MutableList<File>
    private var currentIndex = 0
    private lateinit var adapter: FullScreenPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs[0].absolutePath)
        files = (directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() }
            ?: emptyList()).toMutableList()

        currentIndex = intent.getIntExtra("current_index", 0)

        adapter = FullScreenPagerAdapter(files)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentIndex, false)

        // Кнопка удаления файла
        binding.deleteBtn.setOnClickListener {
            if (files.isNotEmpty()) {
                val position = binding.viewPager.currentItem
                val fileToDelete = files[position]
                if (fileToDelete.delete()) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
                    // Удаляем файл из списка и обновляем адаптер
                    files.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    if (files.isEmpty()) {
                        startActivity(Intent(this, GalleryActivity::class.java))
                    }
                } else {
                    Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Кнопка назад
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
    }
}
