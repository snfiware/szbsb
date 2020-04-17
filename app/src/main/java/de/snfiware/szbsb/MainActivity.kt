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
package de.snfiware.szbsb

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.annotation.IntegerRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import de.snfiware.szbsb.main.SectionsPagerAdapter
import com.example.sztab.R
import de.snfiware.szbsb.fullscrn.FullscreenActivity
import de.snfiware.szbsb.fullscrn.HelpActivity
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getMaxTabs
import de.snfiware.szbsb.main.CfgSzHandler
import java.io.File

/**
 * FSF kann die FullScreens Hilfe und PDF starten.
 * Die Klasse steuert ferner den FloatingActionButton fabFullscreen.
 * Dieser hat zwei Zustände:
 * 1) Hilfe     true  @android:drawable/ic_menu_help
 * 2) PDF-Lesen false @android:drawable/ic_menu_view
 */
class FullScreenForwarder : View.OnClickListener {
    companion object {
        private var myMainActivity: MainActivity? = null
    }
    private var myButton: FloatingActionButton

    constructor(mainActivity: MainActivity, button: FloatingActionButton) {
        Log.i("FSF::ctor", "this: "+ this.toString() + " old main: " +
                myMainActivity.toString() + " new main: " + mainActivity)
        //assert(myMainActivity==null , "singleton!" )
        myMainActivity = mainActivity
        myButton = button
    }

    private fun isFirstRun() :Boolean {
        var bRc = true
        val s = CfgSzHandler.getDownloadFolderFromUI()
        try {
            bRc = !File(s).exists()
        } catch (e: Exception) {
            // ign.
        }
        return( bRc )
    }

    fun setIconFromState() :Boolean {
        Log.i("main","setIconFromState")
        val bRc = isFirstRun()
        val i :Int
        if( bRc ) {
            i = android.R.drawable.ic_menu_help
        } else {
            i = android.R.drawable.ic_menu_view
        }
        val d : Drawable
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            d = myMainActivity!!.getResources().getDrawable(i, myMainActivity!!.theme)
        } else {
            d = myMainActivity!!.getResources().getDrawable(i)
        }
        myButton.setImageDrawable(d)
        return( bRc )
    }

    override fun onClick(v: View) {
        Log.i("FSF::onClick", "->")
        CfgSzHandler.dlg2file()
        showFullScreen()
        Log.i("FSF::onClick", "<-")
    }

    fun showFullScreen() {
        Log.i("FSF::SFS", "->")
        if( myButton.id == R.id.fabDownload || !setIconFromState() ) {
            val i = Intent(myMainActivity!!.applicationContext, FullscreenActivity::class.java)
            myMainActivity!!.startActivity(i)
        } else {
            val i = Intent(myMainActivity!!.applicationContext, HelpActivity::class.java)
            myMainActivity!!.startActivity(i)
        }
        Log.i("FSF::SFS", "<-")
    }
}

class MainActivity : AppCompatActivity() {

    companion object {
        public lateinit var myFsf : FullScreenForwarder

        var counter :Int = 0
        fun nextCounter() :Int {
            counter += 1
            return counter
        }
        public var myMain: MainActivity? = null
            get() {
                return field
            }
            set(value) {
                field = value
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("main::onCreate","-> saved: "+ savedInstanceState.toString())
        //
        // TODO: https://developer.android.com/training/permissions/requesting
        if( android.os.Build.VERSION.SDK_INT >= 23 ) {
            if( this.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                // Requesting the permission
                this.requestPermissions( arrayOf(WRITE_EXTERNAL_STORAGE),101 )
            }
        }
        //
        myMain = this
        //
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //
        val sectionsPagerAdapter = SectionsPagerAdapter(
            this,
            supportFragmentManager
        )
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.offscreenPageLimit = getMaxTabs()
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        // init the app and register listeners
        var fab: FloatingActionButton = findViewById(R.id.fabDownload)
        val csf = CfgSzHandler(this )
        fab.setOnClickListener(csf) // evtl. nicht nur in den Button einhängen, sondern auch noch in die MainActivity selbst
        //
        fab = findViewById(R.id.fabFullscreen)
        myFsf = FullScreenForwarder(this, fab )
        fab.setOnClickListener(myFsf)
        //
        Log.e( "main::onCreate", "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}")
        Log.e( "main::onCreate", "${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")
        //Log.i("main::onCreate","test usage of pdf: "+PDFView(this.applicationContext,null).isRecycled.toString())
        //
        Log.i("main::onCreate","<-")
    }

//    override fun onResume() {
//        super.onResume()
//        myFsf.setIconFromState()
//    }

    //    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        Log.i("main::onPostCreate","->")
//        super.onPostCreate(savedInstanceState, persistentState)
//        //
//        myFsf.setIconFromState()
//        Log.i("main::onPostCreate","<-")
//    }
}
