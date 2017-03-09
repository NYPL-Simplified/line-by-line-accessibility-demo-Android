package org.nypl.linebylinedemo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MotionEvent
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
        // Disable scrolling.
        this.webView.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
        this.webView.loadUrl("file:///android_asset/example.html")
        this.layout.addView(this.webView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.previousMenuItem = menu.add("Previous")
        this.previousMenuItem.setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
        this.previousMenuItem.isEnabled = false

        this.nextMenuItem = menu.add("Next")
        this.nextMenuItem.setShowAsAction(SHOW_AS_ACTION_IF_ROOM)

        return true
    }
}
