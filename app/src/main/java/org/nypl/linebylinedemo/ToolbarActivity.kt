package org.nypl.linebylinedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout

open class ToolbarActivity : AppCompatActivity() {

    lateinit var layout: LinearLayout
    lateinit var toolbar: Toolbar

    private fun dataForThemeAttribute(attribute: Int): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(attribute, typedValue, true)

        return typedValue.data
    }

    fun newToolbar(): Toolbar {
        val toolbar = Toolbar(this)
        toolbar.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                TypedValue.complexToDimensionPixelOffset(
                        this.dataForThemeAttribute(R.attr.actionBarSize),
                        this.resources.displayMetrics))
        // This must be set manually else the toolbar will not be shown.
        toolbar.setBackgroundColor(this.dataForThemeAttribute(R.attr.colorPrimary))

        return toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.layout = LinearLayout(this)
        this.layout.orientation = LinearLayout.VERTICAL
        this.setContentView(this.layout)

        this.toolbar = this.newToolbar()
        this.layout.addView(this.toolbar)

        this.setSupportActionBar(this.toolbar)
    }
}
