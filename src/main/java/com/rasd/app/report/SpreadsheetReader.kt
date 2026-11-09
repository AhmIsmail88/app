package com.rasd.app.report

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * قارئ بسيط لملفات XLSX و CSV لاستيراد بيانات الطلاب (بلا تبعيات خارجية).
 * يعيد صفوفاً من الخلايا النصية، مع تجاهل الخلايا الفارغة.
 */
object SpreadsheetReader {

    fun read(context: Context, uri: Uri): List<List<String>> {
        val name = (uri.lastPathSegment ?: "").lowercase()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
        return if (name.endsWith(".csv")) parseCsvText(String(bytes, Charsets.UTF_8)) else parseXlsx(bytes)
    }

    fun parseCsvText(text: String): List<List<String>> =
        text.split('\n')
            .map { it.trim('\r') }
            .filter { it.isNotBlank() }
            .map { splitCsvLine(it) }

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        out.add(sb.toString())
        return out
    }

    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        var shared: List<String> = emptyList()
        var sheetXml: String? = null
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> shared = parseSharedStrings(zis.readBytes())
                    "xl/worksheets/sheet1.xml" -> sheetXml = String(zis.readBytes(), Charsets.UTF_8)
                }
                entry = zis.nextEntry
            }
        }
        val xml = sheetXml ?: return emptyList()
        return parseSheet(xml, shared)
    }

    private fun newDoc() = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }.newDocumentBuilder()

    private fun parseSharedStrings(content: ByteArray): List<String> {
        return try {
            val doc = newDoc().parse(content.inputStream())
            val si = doc.getElementsByTagName("si")
            (0 until si.length).map { i ->
                val el = si.item(i) as Element
                val ts = el.getElementsByTagName("t")
                val sb = StringBuilder()
                for (j in 0 until ts.length) sb.append(ts.item(j).textContent ?: "")
                sb.toString()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSheet(xml: String, shared: List<String>): List<List<String>> {
        return try {
            val doc = newDoc().parse(xml.byteInputStream())
            val rows = doc.getElementsByTagName("row")
            val out = mutableListOf<List<String>>()
            for (i in 0 until rows.length) {
                val row = rows.item(i) as Element
                val cells = row.getElementsByTagName("c")
                val values = sortedMapOf<Int, String>()
                for (j in 0 until cells.length) {
                    val c = cells.item(j) as Element
                    val col = colIndex(c.getAttribute("r") ?: "")
                    val type = c.getAttribute("t")
                    val v = c.getElementsByTagName("v")
                    val value = when {
                        type == "s" -> shared.getOrNull((v.item(0)?.textContent ?: "").trim().toIntOrNull() ?: -1) ?: ""
                        type == "inlineStr" -> {
                            val ts = c.getElementsByTagName("t")
                            if (ts.length > 0) ts.item(0).textContent ?: "" else ""
                        }
                        v.length > 0 -> v.item(0).textContent ?: ""
                        else -> ""
                    }
                    if (value.isNotBlank()) values[col] = value
                }
                if (values.isNotEmpty()) {
                    val max = values.lastKey()
                    out.add((0..max).map { values[it] ?: "" })
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch.isLetter()) n = n * 26 + (ch.uppercaseChar() - 'A' + 1) else break
        }
        return (n - 1).coerceAtLeast(0)
    }
}
