package io.github.sds100.keymapper.Activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by sds100 on 19/12/2018.
 */

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            /*when the back button in the toolbar is pressed, call onBackPressed so it acts like the
            hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}