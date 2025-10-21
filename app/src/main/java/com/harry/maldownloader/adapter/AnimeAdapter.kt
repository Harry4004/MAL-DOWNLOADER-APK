package com.harry.maldownloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
        holder.episodesText.text = "${entry.episodesWatched ?: 0}/${entry.totalEpisodes ?: "?"} eps"
        
        holder.imageView.load(entry.imagePath ?: entry.imageUrl)
        
        holder.tagGroup.removeAllViews()
        val tagList: List<String> = entry.tags ?: emptyList()
        tagList.forEach { tag ->
            val trimmedTag = tag.trim()
            if (trimmedTag.isNotEmpty()) {
                val chip = Chip(holder.tagGroup.context).apply {
                    text = trimmedTag
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { 
                        val updatedTags = getUpdatedTags(position, trimmedTag)
                        onTagEdit(position, updatedTags)
                    }
                }
                holder.tagGroup.addView(chip)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            showTagEditDialog(holder.itemView.context, position, tagList)
            true
        }
    }

    override fun getItemCount() = entries.size

    private fun showTagEditDialog(context: android.content.Context, position: Int, currentTags: List<String>) {
        val initial = currentTags.joinToString(", ")
        val input = EditText(context).apply { 
            setText(initial)
            hint = "Enter tags separated by commas"
        }
        
        AlertDialog.Builder(context)
            .setTitle("Edit Tags (comma-separated)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTagsString = input.text.toString()
                val newTags = newTagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                entries[position] = entries[position].copy(tags = newTags)
                notifyItemChanged(position)
                onTagEdit(position, newTags)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getUpdatedTags(position: Int, removedTag: String): List<String> {
        val tags = entries[position].tags ?: emptyList()
        return tags.map { it.trim() }.filter { it != removedTag.trim() && it.isNotEmpty() }
    }
}
