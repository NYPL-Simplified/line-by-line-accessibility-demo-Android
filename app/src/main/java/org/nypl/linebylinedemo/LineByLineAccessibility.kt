package org.nypl.linebylinedemo

import org.json.JSONException
import org.json.JSONObject

class LineByLineAccessibility {

    data class Document(val pages: List<Page>)

    data class Page(val lines: List<Line>)

    data class Line(val pageIndex: Int, val pageRelativeRectangle: Rectangle, val text: String)

    data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double)

    companion object {
        fun documentOfJSONObject(documentObject: JSONObject): Document? {
            try {
                val pagesArray = documentObject.getJSONArray("pages")
                val pages = List(pagesArray.length()) { i ->
                    val pageObject = pagesArray.getJSONObject(i)
                    val linesArray = pageObject.getJSONArray("lines")
                    val lines = List(linesArray.length()) { j ->
                        val lineObject = linesArray.getJSONObject(j)
                        val pageIndex = lineObject.getInt("pageIndex")
                        val pageRelativeRectObject = lineObject.getJSONObject("pageRelativeRect")
                        val x = pageRelativeRectObject.getDouble("left")
                        val y = pageRelativeRectObject.getDouble("top")
                        val width = pageRelativeRectObject.getDouble("width")
                        val height = pageRelativeRectObject.getDouble("height")
                        val rectangle = Rectangle(x = x, y = y, width = width, height = height)
                        val text = lineObject.getString("text")
                        Line(pageIndex = pageIndex, pageRelativeRectangle = rectangle, text = text)
                    }
                    Page(lines = lines)
                }
                return Document(pages = pages)
            } catch(_ : JSONException) {
                return null
            }
        }
    }
}