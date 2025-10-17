package com.harry.maldownloader

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class AnimeEntry(
    val id: String,
    val title: String
)

object MalParser {

    fun parse(file: File): List<AnimeEntry> {
        val entries = mutableListOf<AnimeEntry>()
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.parse(file)
        val animeNodes = doc.getElementsByTagName("anime")

        for (i in 0 until animeNodes.length) {
            val node = animeNodes.item(i)
            val id = node.getChildText("series_animedb_id")
            val title = node.getChildText("series_title")
            entries.add(AnimeEntry(id, title))
        }

        println("Found ${entries.size} entries in ${file.name}")
        return entries
    }

    private fun org.w3c.dom.Node.getChildText(tag: String): String {
        val nodes = this.childNodes
        for (i in 0 until nodes.length) {
            val child = nodes.item(i)
            if (child.nodeName == tag) return child.textContent.trim()
        }
        return ""
    }
}
