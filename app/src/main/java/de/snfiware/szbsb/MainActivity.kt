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
package de.snfiware.szbsb

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import de.snfiware.szbsb.main.CfgSzHandler
import de.snfiware.szbsb.main.SectionsPagerAdapter
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getMaxTabs
import de.snfiware.szbsb.util.AcmtLogger
import de.snfiware.szbsb.util.ConfiguredLogHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.view.*


class MainActivity : AppCompatActivity() {
    val CTAG = AcmtLogger("Main")
    //
    companion object {
        lateinit var myFsf : FullScreenForwarder

        var counter :Int = 0
        fun nextCounter() :Int {
            counter += 1
            return counter
        }

        fun getCheckedRadioButtonId(): Int {
            val rgBereich = myMain?.findViewById(R.id.rgBereich) as RadioGroup
            return(rgBereich.checkedRadioButtonId)
        }

        fun getCheckedRadioButton(): RadioButton {
            val rbChecked = myMain?.findViewById(getCheckedRadioButtonId()) as RadioButton
            return(rbChecked)
        }

        fun getCheckedRadioButtonCaption(): String {
            val rbChecked = getCheckedRadioButton()
            val sArea = rbChecked.text.toString()
            return(sArea)
        }

        var myMain: MainActivity? = null
            get() {
                return field
            }
            set(value) {
                field = value
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //CTAG.e("Test-Logeintrag mit Klasse Fehler (E) - kommt das im Bugreport raus?")
        CTAG.enti("onCreate","App startet/wird neu aufgebaut"
                + "; vers: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}/${BuildConfig.BUILD_TYPE}/${BuildConfig.VERSION_CODE})"
                + "; sdk: ${Build.VERSION.SDK_INT}; release: ${Build.VERSION.RELEASE}.${Build.VERSION.INCREMENTAL}"
                + "; saved: ${savedInstanceState}."
        )
        //
        myMain = this
        super.onCreate(savedInstanceState)
        //
        CTAG.i("Tabs einrichten...")
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
        CTAG.i("Listener registrieren...")
        CTAG.d("Download-Knopf an CfzSzHandler binden...")
        var fab: FloatingActionButton = findViewById(R.id.fabDownload)
        val csf = CfgSzHandler(this )
        fab.setOnClickListener(csf) // evtl. nicht nur in den Button einhängen, sondern auch noch in die MainActivity selbst
        fab.setOnLongClickListener { _ ->
            val tv = findViewById<TextView>(R.id.editTextFolder)
            val oldVal = tv.visibility
            val newVal = when(oldVal) {
                View.VISIBLE -> View.GONE
                else         -> View.VISIBLE
            }
            //
            if( newVal == View.VISIBLE) {
                checkAndRequestPermission()
            }
            //
            CTAG.i("toggling visiblility of editTextFolder from $oldVal to $newVal")
            tv.visibility = newVal
            true
        }
        //
        CTAG.d("Fullscreen-Knopf an FullScreenForwarder binden...")
        fab = findViewById(R.id.fabFullscreen)
        myFsf = FullScreenForwarder(this, fab )
        fab.setOnClickListener(myFsf)
        //
        CTAG.d( "Env.getExternalStoragePublicDirectory (depr.): ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}")
        CTAG.d( "getExternalFilesDir(Download): ${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")
        //

        /*
        CTAG.enter("meth","wurst")
        CTAG.log("reinbeißen")
        val CTAG2 = AcmtLogger("Sub")
        CTAG2.log("classlog")
            CTAG2.enter("2er")
            CTAG2.log("in2")
                CTAG.enter("ctor","y")  // BZ.Main.ctor -> y
                CTAG.log_("xxx")        // BZ.Main xxx
                CTAG.log("wtf")         // BZ.Main.ctor wtf
                    CTAG2.enter("getX","17") // BZ.Main.getX .-> 17
                        CTAG.log("wtf")         // BZ.Main.getX .wtf
                    CTAG2.leave("bRc")  // BZ.Main.getX .<-back2:[BZ.Main.ctor] bRc:true
                CTAG.leave("z")         // BZ.Main.ctor <-back2:[BZ.Sub.2er] z
            CTAG.log("xxx")         // BZ.Main xxx
            CTAG2.leave()
//    CTAG2.leave()
        CTAG.enter("zelle","salzig")
//        CTAG2.enter("longlonglonglong")
        CTAG.log("Geschmacksverirrung")
        CTAG2.log("muffi")
        CTAG.leave("süß")
        CTAG.log("Händlmeier")
        CTAG.leave("brötchen")
        CTAG.enti("enti")
        CTAG.leavi()
        CTAG.enti("enti2","möp")
        CTAG.leavi("leavi2")
        //
        CTAG.enter("enter","dful")
        CTAG.leave()
        CTAG.enter("enter2")
        CTAG.leave("leave2")
        */
        //showSnack("piep")
        //putCurrentPidToWhitelist()
        //showSnack("hdl")
        CTAG.leavi()
    }
    //
    // Handling Permissions
    //
    val PERMISSION_REQUEST_CODE = 101 // any number to get together request and result
    //
    fun checkAndRequestPermission() {
        CTAG.enter("chkAndReqPerm","Checking permissions on api ${android.os.Build.VERSION.SDK_INT}...")
        // TODO: https://developer.android.com/training/permissions/requesting
        if( android.os.Build.VERSION.SDK_INT >= 23 ) {
            if( this.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                // Requesting the permission
                CTAG.i("Requesting permissions...")
                this.requestPermissions( arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE )
            }
        }
        CTAG.leave()
    }
    //
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        CTAG.enter("onReqPermResult",
            "requestCode: ${requestCode}; permissions: ${permissions.size}; grantResults: ${grantResults.size}")
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow in your app.
                    CTAG.d("permission was granted")
//                    CTAG.d("permission was granted, restart activity...")
//                    val intent = getIntent()
//                    finish()
//                    startActivity(intent)
                    //
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    CTAG.i("permission was DENIED")

                }
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
                CTAG.i("unknown request") // check if significant
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CTAG.leave()
    }
    //
    // Log Integration
    //
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        val loghelper = ConfiguredLogHelper(this, this.view_pager.rbExtra, this.view_pager)
        loghelper.showPopupMenu(this)
        return super.onKeyLongPress(keyCode, event)
    }
}

