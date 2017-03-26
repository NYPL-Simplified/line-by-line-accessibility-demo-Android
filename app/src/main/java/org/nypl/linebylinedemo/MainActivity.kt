package org.nypl.linebylinedemo

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.view.GestureDetectorCompat
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : ToolbarActivity() {

    lateinit var webView: WebView
    lateinit var previousMenuItem: MenuItem
    lateinit var nextMenuItem: MenuItem
    lateinit var readAloudMenuItem: MenuItem
    lateinit var textToSpeech: TextToSpeech
    lateinit var gestureDetector: GestureDetectorCompat
    var currentPageIndex = 0
    var document: LineByLineAccessibility.Document? = null
    var isTextToSpeechReady = false

    init {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.webView = WebView(this)
        // Setting the height to `MATCH_PARENT` is necessary for `overflow: paged-x`
        // to have an effect.
        this.webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        // Disable scrolling for demo purposes because we do not yet have snapping.
        this.webView.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
        this.webView.setWebViewClient(object: WebViewClient() {
            override fun onPageFinished(webView: WebView, url: String) {
                webView.evaluateJavascript("processedDocument") { string ->
                    if(string != null) {
                        // We run this on the UI thread to avoid races on `document`.
                        runOnUiThread {
                            val documentObject = JSONObject(string)
                            if(documentObject != null) {
                                this@MainActivity.document =
                                        LineByLineAccessibility.documentOfJSONObject(documentObject)
                                this@MainActivity.supportInvalidateOptionsMenu()
                            } else {
                                Log.e(null, "Failed to parse document")
                            }
                        }
                    }
                }
                return
            }
        })
        this.webView.settings.javaScriptEnabled = true
        this.webView.loadUrl("file:///android_asset/example.html")
        this.layout.addView(this.webView)

        this.gestureDetector = GestureDetectorCompat(this, GestureListener(this))

        this.webView.setOnTouchListener { _, event ->
            this.gestureDetector.onTouchEvent(event)
            true
        }

        this.textToSpeech = TextToSpeech(this) { status: Int ->
            // We run this on the UI thread to avoid races on `isTextToSpeechReady`.
            runOnUiThread {
                if(status == TextToSpeech.SUCCESS) {
                    this.isTextToSpeechReady = true
                    this.supportInvalidateOptionsMenu()
                } else {
                    Log.d(null, "Unable to initialize text-to-speech")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.previousMenuItem = menu.add("Previous")
        this.previousMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.previousMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(-this.webView.width, 0)
            --this.currentPageIndex
            this.supportInvalidateOptionsMenu()
            true
        }

        this.nextMenuItem = menu.add("Next")
        this.nextMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.nextMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(this.webView.width, 0)
            ++this.currentPageIndex
            this.supportInvalidateOptionsMenu()
            true
        }

        val readAloudMenuItem = menu.add("Read Aloud")
        readAloudMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        readAloudMenuItem.setOnMenuItemClickListener {
            // This menu item is only enabled if `this.document != null`.
            if(android.os.Build.VERSION.SDK_INT >= 21) {
                this.textToSpeech.speak(
                        this.document!!.pages[this.currentPageIndex].lines[0].text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "")

            } else {
                this.textToSpeech.speak(
                        this.document!!.pages[this.currentPageIndex].lines[0].text,
                        TextToSpeech.QUEUE_FLUSH,
                        null)
            }
            true
        }
        this.readAloudMenuItem = readAloudMenuItem

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val document = this.document

        if(document == null) {
            this.previousMenuItem.isEnabled = false
            this.nextMenuItem.isEnabled = false
            this.readAloudMenuItem.isEnabled = false
        } else {
            this.previousMenuItem.isEnabled = this.currentPageIndex != 0
            this.nextMenuItem.isEnabled = this.currentPageIndex < document.pages.size - 1
            this.readAloudMenuItem.isEnabled = this.isTextToSpeechReady
        }

        return true
    }

    private fun accessibilityLineNumberForPoint(x: Double, y: Double): Int? {
        val document = this.document ?: return null

        document.pages[this.currentPageIndex].lines.withIndex().forEach { line ->
            if(line.value.pageRelativeRectangle.containsPoint(x, y)) {
                return line.index
            }
        }

        return null
    }

    private fun accessibilityContentForLineNumber(lineNumber: Int): String? {
        val document = this.document ?: return null

        return document.pages[this.currentPageIndex].lines[lineNumber].text
    }

    private fun accessibilityFrameForLineNumber(lineNumber: Int): LineByLineAccessibility.Rectangle {
        val document = this.document ?: return LineByLineAccessibility.Rectangle.zero

        return document.pages[this.currentPageIndex].lines[lineNumber].pageRelativeRectangle
    }

    private fun accessibilityPageContent(): String? {
        val document = this.document ?: return null

        return document.pages[this.currentPageIndex].lines.map({it.text}).joinToString(separator = " ")
    }

    private class GestureListener(val mainActivity: MainActivity) : GestureDetector.SimpleOnGestureListener() {
        // This needs to be implemented so it can return `true`. Without this,
        // `onSingleTapUp` will never be called.
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if(e == null) return false

            val density = mainActivity.resources.displayMetrics.density

            Log.d("line-tapped",
                    mainActivity.accessibilityLineNumberForPoint(
                            e.x.toDouble() / density,
                            e.y.toDouble() / density).toString())

            return true
        }
    }
}
