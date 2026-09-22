package com.sniffy.uvpaint

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Self-contained in-app file browser, replacing the system ACTION_OPEN_DOCUMENT
 * picker.
 *
 * Why: assimp reads a model straight off the filesystem and resolves any
 * sibling files it references (loose glTF .bin/textures, OBJ .mtl) relative
 * to that path on its own. A SAF document picker only ever hands back one
 * opaque content:// Uri for the single file the user tapped, with no way to
 * see its siblings — which is exactly the "companion files aren't resolved
 * yet" limitation this replaces. Browsing real java.io.File paths (backed by
 * the MANAGE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE access MainActivity
 * requests) means the path returned here is the model's real location, so
 * its neighbors are still right there next to it when assimp loads it.
 */
class FilePickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "picked_path"
        private val MODEL_EXTENSIONS = setOf("obj", "gltf", "glb", "fbx")
    }

    private lateinit var listView: ListView
    private lateinit var pathLabel: TextView

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var currentChildren: List<File> = emptyList()
    private var hasParentRow: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pathLabel = TextView(this).apply {
            setPadding(32, 24, 32, 24)
            textSize = 14f
        }
        listView = ListView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(pathLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(listView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)

        listView.setOnItemClickListener { _, _, position, _ ->
            if (hasParentRow && position == 0) {
                currentDir.parentFile?.let { openDir(it) }
                return@setOnItemClickListener
            }
            val entry = currentChildren[position - if (hasParentRow) 1 else 0]
            if (entry.isDirectory) openDir(entry) else pickFile(entry)
        }

        openDir(currentDir)
    }

    private fun openDir(dir: File) {
        val children = dir.listFiles()
        if (children == null) {
            Toast.makeText(this, "Can't open ${dir.name} — access blocked by the system", Toast.LENGTH_SHORT).show()
            return
        }

        currentDir = dir
        pathLabel.text = dir.absolutePath
        hasParentRow = dir.parentFile != null

        currentChildren = children
            .filter { it.isDirectory || it.extension.lowercase() in MODEL_EXTENSIONS }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        val labels = mutableListOf<String>()
        if (hasParentRow) labels.add("⬆  ..")
        labels += currentChildren.map { if (it.isDirectory) "📁  ${it.name}" else "📄  ${it.name}" }

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    private fun pickFile(file: File) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PATH, file.absolutePath))
        finish()
    }
}
