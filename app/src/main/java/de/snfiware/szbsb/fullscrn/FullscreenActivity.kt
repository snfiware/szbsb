/**
 * Copyright 2020 (Corona-Version) Schnuffiware - snuffo@freenet.de
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

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.example.sztab.R // TODO rename to szbsb
import de.snfiware.szbsb.FullScreenForwarder
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.MainActivity.Companion.myFsf
import de.snfiware.szbsb.main.CfgSzHandler
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
    lateinit var myNavi : DatePageNavigator

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
            Log.d(
                "FSA::dTouchEvent",
                "x: " + e.x.toString() + "; y: " + e.y.toString()
                        + "; evt: " + e.action + "; ptrCnt: " + ptrCnt.toString()
            )
            if (ptrCnt == 3) {
                if (e.action == 517) {
                    Log.d("FSA::dTouchEvent","-> resetZoomWithAnimation")
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
                            if (myLastUpPos.x < myLastDownPos.x)
                                myNavi.onClickPdfRechts()
                            else
                                myNavi.onClickPdfLinks()
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
        // Delayed display of UI elements
        supportActionBar?.show()
        sz_fullscreen_content_controls.visibility = VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    // @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("FSA::onCreate", "->")
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
        Log.i("FSA::onCreate", "<-")
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        Log.i("FSA::onPostCreate", "->")
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100)

        //fullscreen_content.bringToFront()
//        fullscreen_content.setMaxZoom(10f)
//        fullscreen_content.setMidZoom(5.7f)
//        fullscreen_content.setMinZoom(2.8f)
        //
        fullscreen_content.setMaxZoom(5.7f)
        fullscreen_content.setMidZoom(2.8f)
        fullscreen_content.setMinZoom(1.0f)
        //
        rebuildNavigator(null)
        myFsf.setIconFromState()
        //
        Log.i("FSA::onPostCreate", "<-")
    }

    fun rebuildNavigator( d :DeleteHandler? ) {
        Log.i("FSA::rebuildNavigator", "->")
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
        Log.i("FSA::rebuildNavigator", "<-")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = this.menuInflater
        inflater.inflate(R.menu.fullscr_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        Log.d("FSA::onOptItemSel", "id ${id.toString()} == ${android.R.id.home.toString()}")
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this)
            return true

        } else if (id == R.id.mi_help) {
            val i = Intent(MainActivity.myMain!!.applicationContext, HelpActivity::class.java)
            MainActivity.myMain!!.startActivity(i)
            return true

        } else if (id == R.id.mi_delete_pdf || id == R.id.mi_delete_folder || id == R.id.mi_delete_all ) {
            return DeleteHandler.handleOnOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    fun toggle() {
        Log.i("FSA","toggle; mVisible: " + mVisible.toString())
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        Log.i("FSA","hide" )
        // Hide UI first
        supportActionBar?.hide()
        sz_fullscreen_content_controls.visibility = GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        Log.i("FSA","show" )
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
        Log.i("FSA","delayedHide: " + delayMillis.toString() )
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
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
    }

    /** Navigation for the Fullscreen internals via RelativeLayoutViews
     * */
//    override fun onClick(v: View?) {
//        Log.e("NAVI","FUNZT")
//        myNavi.setCurPointer(1,2)
//    }
}
