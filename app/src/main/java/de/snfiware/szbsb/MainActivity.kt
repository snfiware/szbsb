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
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import de.snfiware.szbsb.main.SectionsPagerAdapter
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getMaxTabs
import de.snfiware.szbsb.main.CfgSzHandler
import de.snfiware.szbsb.util.AcmtLogger

class MainActivity : AppCompatActivity() {
    val CTAG = AcmtLogger("Main")

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
        CTAG.e("Test-Logeintrag mit Klasse Fehler (E) - kommt das im Bugreport raus?")
        CTAG.enter("onCreate","saved: "+ savedInstanceState.toString())
        //
        CTAG.log("Checking permissions...")
        // TODO: https://developer.android.com/training/permissions/requesting
        if( android.os.Build.VERSION.SDK_INT >= 23 ) {
            if( this.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                // Requesting the permission
                CTAG.log("Requestion permissions...")
                this.requestPermissions( arrayOf(WRITE_EXTERNAL_STORAGE),101 )
            }
        }
        //
        myMain = this
        super.onCreate(savedInstanceState)
        //
        CTAG.log("Tabs einrichten...")
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
        CTAG.log("Download-Knopf an CfzSzHandler binden...")
        var fab: FloatingActionButton = findViewById(R.id.fabDownload)
        val csf = CfgSzHandler(this )
        fab.setOnClickListener(csf) // evtl. nicht nur in den Button einhängen, sondern auch noch in die MainActivity selbst
        //
        CTAG.log("Fullscreen-Knopf an FullScreenForwarder binden...")
        fab = findViewById(R.id.fabFullscreen)
        myFsf = FullScreenForwarder(this, fab )
        fab.setOnClickListener(myFsf)
        //
        CTAG.log( "Env.getExternalStoragePublicDirectory (depr.): ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}")
        CTAG.log( "getExternalFilesDir(Download): ${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")

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
//        CTAG2.leave()
        CTAG.enter("zelle","salzig")
//        CTAG2.enter("longlonglonglong")
        CTAG.log("Geschmacksverirrung")
        CTAG2.log("muffi")
        CTAG.leave("süß")
        CTAG.log("Händlmeier")
        CTAG.leave("brötchen")
        */
        CTAG.leave()
    }
}
