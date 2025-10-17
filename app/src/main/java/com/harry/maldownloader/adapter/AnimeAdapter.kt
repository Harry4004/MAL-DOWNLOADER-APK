package com.harry.maldownloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.harry.maldownloader.R
import com.harry.maldownloader.data.AnimeEntry

class AnimeAdapter(
    private val entries: MutableList<AnimeEntry>,
    private val onTagEdit: (Int, List<String>) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val statusText: TextView = view.findViewById(R.id.statusText)
        val episodesText: TextView = view.findViewById(R.id.episodesText)
        val imageView: ImageView = view.findViewById(R.id.animeImage)
        val tagGroup: ChipGroup = view.findViewById(R.id.tagGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anime, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.titleText.text = entry.title
        holder.statusText.text = entry.status ?: "Unknown"
        holder.episodesText.text = "${entry.episodesWatched ?: 0}/${entry.episodesTotal ?: 0} eps"
        
        // Load image with Coil (fallback to placeholder if path invalid)
        holder.imageView.load(entry.imagePath) {
            placeholder(R.drawable.ic_placeholder)  // Add a simple drawable if needed
            error(R.drawable.ic_error)  // Or generated placeholder
        }
        
        // Populate tags as chips
        holder.tagGroup.removeAllViews()
        entry.tags?.split(",")?.forEach { tag ->
            val chip = Chip(holder.tagGroup.context).apply {
                text = tag.trim()
                isCloseIconVisible = true
                setOnCloseIconClickListener { 
                    // Remove tag logic here, then notify
                    onTagEdit(position, getUpdatedTags(position, tag))
                }
            }
            holder.tagGroup.addView(chip)
        }
        
        holder.itemView.setOnLongClickListener {
            showTagEditDialog(position, entry.tags ?: "")
            true
        }
    }

    override fun getItemCount() = entries.size

    private fun showTagEditDialog(position: Int, currentTags: String) {
        val context = entries[0].let { /* dummy */ }  // Use adapter context
        val input = android.widget.EditText(context).apply { setText(currentTags) }
        AlertDialog.Builder(context)
            .setTitle("Edit Tags (comma-separated)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTags = input.text.toString()
                entries[position] = entries[position].copy(tags = newTags)
                notifyItemChanged(position)
                onTagEdit(position, newTags.split(","))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getUpdatedTags(position: Int, removedTag: String): List<String> {
        // Implement removal logic
        val tags = entries[position].tags?.split(",") ?: emptyList()
        return tags.filter { it.trim() != removedTag.trim() }.joinToString(",")
    }
}
