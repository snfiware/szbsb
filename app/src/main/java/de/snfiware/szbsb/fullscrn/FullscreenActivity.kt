/**
 * Copyright 2020 (Corona-Version) Schnuffiware - https://github.com/snfiware/szbsb
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.snfiware.szbsb.fullscrn

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.android.material.snackbar.Snackbar
import de.snfiware.szbsb.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.MainActivity.Companion.myFsf
import de.snfiware.szbsb.main.CfgSzHandler
import de.snfiware.szbsb.util.AcmtLogger
import de.snfiware.szbsb.util.ConfiguredLogHelper
import kotlinx.android.synthetic.main.activity_fullscreen.*
import kotlin.math.abs


/**
 * A full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * dispatchTouchEvent() constantly monitors the touch events, evaluates and forwards
 * the navigation events to the DatePageNavigator instance myNavi (if a swipe is
 * detected).
 */
class FullscreenActivity : AppCompatActivity() {
    //
    lateinit var myNavi : DatePageNavigator
    lateinit var myMenu : Menu
    //
    val MIN_DIST_SWIPE = 50 // min distance between down and up for successful swipe detection
    var myLastDownPos : Point = Point(-1,-1)
    var myLastUpPos : Point = Point(-1,-1)
    //
    val MAX_DIST_THREE_TAP = 10
    //
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        var bRc = true
        if (event != null) {
            val e: MotionEvent = event
            val ptrCnt = e.pointerCount
            CTAG.v("dTouchEvent - x: " + e.x.toString() + "; y: " + e.y.toString()
                        + "; evt: " + e.action + "; ptrCnt: " + ptrCnt.toString()
            )
            if (ptrCnt == 3) {
                if (e.action == 517) {
                    CTAG.log("dTouchEvent -> resetZoomWithAnimation")
                    fullscreen_content.resetZoomWithAnimation()
                }
/*
                if ((e.action and MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP) {
                    val deltaClickPos = Point(
                        (myLastUpPos.x - myLastDownPos.x),
                        (myLastUpPos.y - myLastDownPos.y)
                    )
                    val absDelta = Point(abs(deltaClickPos.x), abs(deltaClickPos.y))
                    if (absDelta.x < MAX_DIST_THREE_TAP &&
                        absDelta.y < MAX_DIST_THREE_TAP) {
                        fullscreen_content.resetZoomWithAnimation()
                    }
                }
*/
            }
            else {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    myLastDownPos.x = e.getX(e.actionIndex).toInt()
                    myLastDownPos.y = e.getY(e.actionIndex).toInt()
                } else if (e.action == MotionEvent.ACTION_UP) {
                    myLastUpPos.x = e.getX(e.actionIndex).toInt()
                    myLastUpPos.y = e.getY(e.actionIndex).toInt()

                    val deltaClickPos = Point(
                        (myLastUpPos.x - myLastDownPos.x),
                        (myLastUpPos.y - myLastDownPos.y)
                    )

                    val absDelta = Point(abs(deltaClickPos.x), abs(deltaClickPos.y))
                    if (absDelta.x > MIN_DIST_SWIPE
                        && absDelta.x > 2 * absDelta.y // Winkel ist unter 30°
                    ) {
                        // we got a swipe
                        if (fullscreen_content.isZooming()) {
                            // do nothing when the pdf is zoomed in
                        } else {
                            // navigate when the pdf is zoomed out fully
                            if (myLastUpPos.x < myLastDownPos.x) {
                                CTAG.log("dTouchEvent - Nach rechts navigieren...")
                                myNavi.onClickPdfRechts()
                            } else {
                                CTAG.log("dTouchEvent - Nach links navigieren...")
                                myNavi.onClickPdfLinks()
                            }
                        }
                    }
                }
            }
        }
        if( bRc == true ) {
            bRc = super.dispatchTouchEvent(event)
        }
        return bRc
    }

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        CTAG.log("Hide Runnable Part 2")
        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            SYSTEM_UI_FLAG_LOW_PROFILE or
                    SYSTEM_UI_FLAG_FULLSCREEN or
//                    SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        CTAG.log("Show Runnable")
        // Delayed display of UI elements
        supportActionBar?.show()
        sz_fullscreen_content_controls.visibility = VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { CTAG.log("Hide Runnable Part 1"); hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = OnTouchListener { _, _ ->
        CTAG.log("OnTouchListener")
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        CTAG.enter("onCreate","savedInstanceState: $savedInstanceState")
        fsa = this
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if( android.os.Build.VERSION.SDK_INT >= 21 ) {
            imageViewDatum.z = 50f
            imageViewPdf.z = 50f
        }

        mVisible = true

        //fullscreen_frame.setOnTouchListener(this)

        //findViewById<RelativeLayout>(R.id.reLaRight).setOnClickListener(this)

        // Set up the user interaction to manually show or hide the system UI.
        //fullscreen_content.setOnTouchListener(this)
        //fullscreen_content.setOnClickListener { toggle() }
        //fullscreen_content.call

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //dummy_button.setOnTouchListener(mDelayHideTouchListener)
        CTAG.leave()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        CTAG.enter("onPostCreate","savedInstanceState: $savedInstanceState")
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100)
        //
        MyPdfView.setZoom(this)
        //
        rebuildNavigator(null)
        myFsf.setIconFromState()
        //
        DeleteHandler.checkAndPossiblyShowDeleteWarning(this)
        //
        CTAG.leavi("now showing Pdf Fullscreen-View")
    }

    fun rebuildNavigator( d :DeleteHandler? ) {
        CTAG.enter("rebuildNavi", "d: $d")
        myNavi = DatePageNavigator(
            this,
            CfgSzHandler.getDownloadFolderFromUI()
        )
        if( d == null ) {
            myNavi.showMostCurrentPdf()
        } else {
            myNavi.showNextPdfAfterDelete(d)
        }
        //myNavi.toggleSpinnerListeners(true)
        CTAG.leave()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        CTAG.enter("onCreaOptMenu", "menu: $menu")
        super.onCreateOptionsMenu(menu)
        myMenu = menu!!
        val inflater = this.menuInflater
        inflater.inflate(R.menu.fullscr_menu, menu)
        CTAG.leave()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        CTAG.enter("onOptItemSel", "item: ${item.title} selected")
        var bRc = true
        val id = item.itemId
        if (id == android.R.id.home) {
            CTAG.i("Navigate (back) to home...")
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this)
        }
        else if (id == R.id.mi_help) {
            CTAG.i("Navigate to help...")
            val i = Intent(MainActivity.myMain!!.applicationContext, HelpActivity::class.java)
            MainActivity.myMain!!.startActivity(i)
        }
        else if (id == R.id.mi_delete_pdf || id == R.id.mi_delete_folder || id == R.id.mi_delete_all
              || id == R.id.mi_delete_older_90 || id == R.id.mi_delete_older_180) {
            CTAG.i("Delegate Delete to Sub-Handler...")
            bRc = DeleteHandler.handleOnOptionsItemSelected(item)
        }
        else {
            CTAG.log("Call super...")
            bRc = super.onOptionsItemSelected(item)
        }
        CTAG.leave("id: $id; bRc: $bRc")
        return bRc
    }
    //
    fun toggle() {
        CTAG.log("toggle; mVisible: " + mVisible.toString())
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }
    //
    private fun hide() {
        CTAG.i("hide controls" )
        // Hide UI first
        supportActionBar?.hide()
        sz_fullscreen_content_controls.visibility = GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }
    //
    private fun show() {
        CTAG.i("show controls" )
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                    SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }
    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        CTAG.i("delayedHide: " + delayMillis.toString() )
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }
    //
    companion object {
        val CTAG = AcmtLogger("Full",true)
        //
        lateinit var fsa : FullscreenActivity
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        //
        fun showSnack(s:String,dur:Int = Snackbar.LENGTH_SHORT) {
            Snackbar.make(fsa.findViewById(R.id.fullscreen_content), s, dur)
                .setAction("Action", null).show()
        }
    }
    //
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        val loghelper = ConfiguredLogHelper(this, this.fullActFooterHook, this.fullscreen_content )
        loghelper.showPopupMenu(this)
        return super.onKeyLongPress(keyCode, event)
    }
}
