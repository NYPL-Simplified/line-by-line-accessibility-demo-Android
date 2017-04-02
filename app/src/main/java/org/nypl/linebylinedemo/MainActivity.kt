package org.nypl.linebylinedemo

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID
import android.speech.tts.UtteranceProgressListener
import android.support.v4.view.GestureDetectorCompat
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.util.*

class MainActivity : ToolbarActivity() {

    val tag = this::class.simpleName
    lateinit var webView: WebView
    lateinit var previousMenuItem: MenuItem
    lateinit var nextMenuItem: MenuItem
    lateinit var readAloudMenuItem: MenuItem
    lateinit var textToSpeech: TextToSpeech
    lateinit var gestureDetector: GestureDetectorCompat
    var document: LineByLineAccessibility.Document? = null
        set(value) {
            field = value
            this.supportInvalidateOptionsMenu()
        }
    var isTextToSpeechReady = false
        set(value) {
            field = value
            this.supportInvalidateOptionsMenu()
        }
    var currentPageIndex = 0
        set(value) {
            if(value == this.currentPageIndex) return
            val document = this.document ?: return
            if(value >= document.pages.count()) return
            field = value
            this.webView.scrollTo(this.webView.width * value, 0)
            if(this.isReadingContinuously) {
                this.speakCurrentPage()
            }
            this.supportInvalidateOptionsMenu()
            Log.d("currentPageIndex", value.toString())
        }
    var isReadingContinuously = false
        set(value) {
            field = value
            this.supportInvalidateOptionsMenu()
        }

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
        this.webView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(webView: WebView, url: String) {
                // This is a hack: Normally, the JS would call a URL with the result once it was ready.
                Timer().schedule(
                        object: TimerTask() {
                            override fun run() {
                                runOnUiThread {
                                    webView.evaluateJavascript("processedDocument") { string ->
                                        if (string != null && string != "null") {
                                            // We run this on the UI thread to avoid races on `document`.
                                            val documentObject = JSONObject(string)
                                            this@MainActivity.document =
                                                    LineByLineAccessibility.documentOfJSONObject(documentObject)
                                        }
                                    }
                                }
                            }
                        },
                        1000
                )

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
                if (status == TextToSpeech.SUCCESS) {
                    this.isTextToSpeechReady = true
                } else {
                    Log.d(tag, "Unable to initialize text-to-speech")
                }
            }
        }

        this.textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String) {
                if (this@MainActivity.isReadingContinuously && this@MainActivity.canGoToNextPage()) {
                    ++this@MainActivity.currentPageIndex
                    val content = this@MainActivity.accessibilityPageContent()
                    if (content != null) {
                        this@MainActivity.speak(content)
                    } else {
                        this@MainActivity.isReadingContinuously = false
                    }
                }
            }

            override fun onError(utteranceId: String) {

            }

            override fun onStart(utteranceId: String) {

            }

            override fun onStop(utteranceId: String, interrupted: Boolean) {

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.previousMenuItem = menu.add("Previous")
        this.previousMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.previousMenuItem.setOnMenuItemClickListener {
            --this.currentPageIndex
            true
        }

        this.nextMenuItem = menu.add("Next")
        this.nextMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.nextMenuItem.setOnMenuItemClickListener {
            ++this.currentPageIndex
            true
        }

        val readAloudMenuItem = menu.add("")
        readAloudMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        readAloudMenuItem.setOnMenuItemClickListener {
            val content = this.accessibilityPageContent()
            if (content != null) {
                this@MainActivity.isReadingContinuously = !this@MainActivity.isReadingContinuously
                if(this@MainActivity.isReadingContinuously) {
                    this.speak(content)
                } else {
                    this@MainActivity.textToSpeech.stop()
                }
            }
            true
        }
        this.readAloudMenuItem = readAloudMenuItem

        return true
    }

    private fun canGoToNextPage(): Boolean {
        val document = this.document ?: return false

        return this.currentPageIndex < document.pages.count() - 1
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val document = this.document

        if (this.isReadingContinuously) {
            this.readAloudMenuItem.title = "Stop Reading"
        } else {
            this.readAloudMenuItem.title = "Read Aloud"
        }

        if (document == null) {
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
            if (line.value.pageRelativeRectangle.containsPoint(x, y)) {
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

        return document.pages[this.currentPageIndex].lines.map({ it.text }).joinToString(separator = " ")
    }

    private class GestureListener(val mainActivity: MainActivity) : GestureDetector.SimpleOnGestureListener() {
        // This needs to be implemented so it can return `true`. Without this,
        // `onSingleTapUp` will never be called.
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (e == null) return false

            val density = mainActivity.resources.displayMetrics.density

            Log.d("line-tapped",
                    mainActivity.accessibilityLineNumberForPoint(
                            e.x.toDouble() / density,
                            e.y.toDouble() / density).toString())

            return true
        }
    }

    private fun speak(string: String) {
        this.textToSpeech.speak(
                string,
                TextToSpeech.QUEUE_FLUSH,
                hashMapOf(KEY_PARAM_UTTERANCE_ID to UUID.randomUUID().toString()))
    }

    private fun speakCurrentPage() {
        val content = this.accessibilityPageContent() ?: return
        this.speak(content)
    }
}

