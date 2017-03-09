package org.nypl.linebylinedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.widget.RelativeLayout

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
    lateinit var previousMenuItem: MenuItem
    lateinit var nextMenuItem: MenuItem

    init {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun dataForThemeAttribute(attribute: Int): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(attribute, typedValue, true)

        return typedValue.data
    }

    private fun newToolbar(): Toolbar {
        val toolbar = Toolbar(this)
        toolbar.layoutParams = ViewGroup.LayoutParams(
            MATCH_PARENT,
            TypedValue.complexToDimensionPixelOffset(
                this.dataForThemeAttribute(R.attr.actionBarSize),
                this.resources.displayMetrics))
        // This must be set manually else the toolbar will not be shown.
        toolbar.setBackgroundColor(this.dataForThemeAttribute(R.attr.colorPrimary))

        return toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = RelativeLayout(this)

        this.webView = WebView(this)
        this.webView.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
        // Disable scrolling.
        this.webView.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
        this.webView.loadUrl("file:///android_asset/example.html")
        layout.addView(this.webView)

        setContentView(layout)

        val toolbar = this.newToolbar()
        layout.addView(toolbar)
        this.setSupportActionBar(toolbar)
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
