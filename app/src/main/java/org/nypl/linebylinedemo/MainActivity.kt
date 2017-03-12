package org.nypl.linebylinedemo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView

class MainActivity : ToolbarActivity() {

    lateinit var webView: WebView
    lateinit var previousMenuItem: MenuItem
    lateinit var nextMenuItem: MenuItem

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
        this.webView.settings.javaScriptEnabled = true
        this.webView.loadUrl("file:///android_asset/example.html")
        this.layout.addView(this.webView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.previousMenuItem = menu.add("Previous")
        this.previousMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.previousMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(-this.webView.width, 0)
            true
        }

        this.nextMenuItem = menu.add("Next")
        this.nextMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        this.nextMenuItem.setOnMenuItemClickListener {
            this.webView.scrollBy(this.webView.width, 0)
            true
        }

        return true
    }
}
