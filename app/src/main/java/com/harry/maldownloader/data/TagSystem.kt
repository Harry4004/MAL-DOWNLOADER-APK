package com.harry.maldownloader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class TagGroup(
    val name: String,
    val tags: List<String>,
    val color: String = "#2196F3"
)

data class TagConfiguration(
    val anime: TagGroup = TagGroup("Anime", listOf("Action", "Comedy", "Drama", "Romance", "Slice of Life")),
    val manga: TagGroup = TagGroup("Manga", listOf("Shounen", "Seinen", "Shoujo", "Josei", "Oneshot")),
    val hentai: TagGroup = TagGroup("Hentai", listOf("Vanilla", "NTR", "Yuri", "Yaoi", "Tentacles"), "#FF5722")
)

class TagSystem(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        private val TAG_CONFIG_KEY = stringPreferencesKey("tag_configuration")
        private val CUSTOM_TAGS_KEY = stringPreferencesKey("custom_tags")
    }
    
    val tagConfiguration: Flow<TagConfiguration> = context.dataStore.data
        .map { preferences ->
            val json = preferences[TAG_CONFIG_KEY]
            if (json != null) {
                try {
                    gson.fromJson(json, TagConfiguration::class.java)
                } catch (e: Exception) {
                    TagConfiguration()
                }
            } else {
                TagConfiguration()
            }
        }
    
    val customTags: Flow<Map<String, List<String>>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CUSTOM_TAGS_KEY] ?: "{}"
            try {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    
    suspend fun updateTagConfiguration(config: TagConfiguration) {
        context.dataStore.edit { preferences ->
            preferences[TAG_CONFIG_KEY] = gson.toJson(config)
        }
    }
    
    suspend fun addCustomTag(entryId: String, tag: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[CUSTOM_TAGS_KEY] ?: "{}"
            val currentMap: MutableMap<String, List<String>> = try {
                val type = object : TypeToken<MutableMap<String, List<String>>>() {}.type
                gson.fromJson(currentJson, type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
            
            currentMap[entryId] = (currentMap[entryId] ?: emptyList()) + tag
            preferences[CUSTOM_TAGS_KEY] = gson.toJson(currentMap)
        }
    }
    
    suspend fun removeCustomTag(entryId: String, tag: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[CUSTOM_TAGS_KEY] ?: "{}"
            val currentMap: MutableMap<String, List<String>> = try {
                val type = object : TypeToken<MutableMap<String, List<String>>>() {}.type
                gson.fromJson(currentJson, type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
            
            currentMap[entryId] = (currentMap[entryId] ?: emptyList()) - tag
            if (currentMap[entryId]?.isEmpty() == true) {
                currentMap.remove(entryId)
            }
            preferences[CUSTOM_TAGS_KEY] = gson.toJson(currentMap)
        }
    }
    
    fun exportTagsToJson(): String {
        // This will be called from UI thread with current tag state
        return gson.toJson(TagConfiguration())
    }
    
    suspend fun importTagsFromJson(json: String): Result<TagConfiguration> {
        return try {
            val config = gson.fromJson(json, TagConfiguration::class.java)
            updateTagConfiguration(config)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun classifyEntry(title: String, type: String): List<String> {
        val tags = mutableListOf<String>()
        val lowerTitle = title.lowercase()
        
        // Auto-classify based on content
        when {
            type.contains("hentai", true) -> tags.add("Hentai")
            type.contains("manga", true) -> tags.add("Manga") 
            else -> tags.add("Anime")
        }
        
        // Genre detection
        when {
            lowerTitle.contains("action") -> tags.add("Action")
            lowerTitle.contains("comedy") -> tags.add("Comedy")
            lowerTitle.contains("romance") -> tags.add("Romance")
            lowerTitle.contains("drama") -> tags.add("Drama")
        }
        
        return tags
    }
}