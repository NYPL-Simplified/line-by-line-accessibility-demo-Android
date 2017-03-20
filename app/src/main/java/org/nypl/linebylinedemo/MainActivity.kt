package org.nypl.linebylinedemo

import android.os.Bundle
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
    var currentPageIndex = 0
    var document: LineByLineAccessibility.Document? = null

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
                        this@MainActivity.document = LineByLineAccessibility.documentOfJSONObject(JSONObject(string))
                        runOnUiThread {
                            this@MainActivity.updatePreviousNextStates()
                        }
                    }
                }
                return
            }
        })
        this.webView.settings.javaScriptEnabled = true
        this.webView.loadUrl("file:///android_asset/example.html")
        this.layout.addView(this.webView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.previousMenuItem = menu.add("Previous")
        this.previousMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.previousMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(-this.webView.width, 0)
            --this.currentPageIndex
            this.updatePreviousNextStates()
            true
        }

        this.nextMenuItem = menu.add("Next")
        this.nextMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.nextMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(this.webView.width, 0)
            ++this.currentPageIndex
            this.updatePreviousNextStates()
            true
        }

        this.updatePreviousNextStates()

        return true
    }

    private fun updatePreviousNextStates() {
        val document = this.document
        if(document == null) {
            this.previousMenuItem.isEnabled = false
            this.nextMenuItem.isEnabled = false
        } else {
            this.previousMenuItem.isEnabled = this.currentPageIndex != 0
            this.nextMenuItem.isEnabled = this.currentPageIndex < document.pages.size - 1
        }
    }
}
