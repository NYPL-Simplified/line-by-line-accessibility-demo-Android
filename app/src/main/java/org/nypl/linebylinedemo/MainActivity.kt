package org.nypl.linebylinedemo

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : ToolbarActivity() {

    lateinit var webView: WebView
    lateinit var previousMenuItem: MenuItem
    lateinit var nextMenuItem: MenuItem
    lateinit var readAloudMenuItem: MenuItem
    lateinit var textToSpeech: TextToSpeech
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
                            this@MainActivity.document =
                                    LineByLineAccessibility.documentOfJSONObject(JSONObject(string))
                            this@MainActivity.supportInvalidateOptionsMenu()
                        }
                    }
                }
                return
            }
        })
        this.webView.settings.javaScriptEnabled = true
        this.webView.loadUrl("file:///android_asset/example.html")
        this.layout.addView(this.webView)

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

        for(line in document.pages[this.currentPageIndex].lines.withIndex()) {
            if(line.pageRelativeRect.contains(point) {
                return i
            }
        }

        return NSNotFound
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
}
