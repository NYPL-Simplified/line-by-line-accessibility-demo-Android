package org.nypl.linebylinedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.RelativeLayout

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView

    init {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = RelativeLayout(this)

        this.webView = WebView(this)
        this.webView.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
        // Disable scrolling.
        this.webView.setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
        this.webView.loadUrl("file:///android_asset/example.html")
        layout.addView(this.webView)

        setContentView(layout)
    }
}
