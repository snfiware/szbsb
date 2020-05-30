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

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.snfiware.szbsb.fullscrn.FullscreenActivity
import de.snfiware.szbsb.fullscrn.HelpActivity
import de.snfiware.szbsb.main.CfgSzHandler
import de.snfiware.szbsb.util.AcmtLogger
import java.io.File

/**
 * FSF kann die FullScreens Hilfe und PDF starten.
 * Die Klasse steuert ferner den FloatingActionButton fabFullscreen.
 * Dieser hat zwei Zustände:
 * 1) Hilfe     true  @android:drawable/ic_menu_help
 * 2) PDF-Lesen false @android:drawable/ic_menu_view
 */
class FullScreenForwarder : View.OnClickListener {
    val CTAG = AcmtLogger("FSF")

    companion object {
        private var myMainActivity: MainActivity? = null
    }
    private var myButton: FloatingActionButton

    constructor(mainActivity: MainActivity, button: FloatingActionButton) {
        CTAG.log("ctor-this: "+ this.toString() + " old main: " +
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
        CTAG.log("isFirstRun: ${bRc}")
        return( bRc )
    }

    fun setIconFromState() :Boolean {
        val bRc = isFirstRun()
        val i :Int
        if( bRc ) {
            i = android.R.drawable.ic_menu_help
        } else {
            i = android.R.drawable.ic_menu_view
        }
        CTAG.log("setIconFromState ${i}")
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
        CTAG.enter("onClick", "Konfig speichern...")
        CfgSzHandler.dlg2file()
        CTAG.log("in den Vollbildmodus wechseln...")
        showFullScreen()
        CTAG.leave()
    }

    fun showFullScreen() {
        CTAG.enter( "showFullScreen")
        if( myButton.id == R.id.fabDownload || !setIconFromState() ) {
            val i = Intent(myMainActivity!!.applicationContext, FullscreenActivity::class.java)
            myMainActivity!!.startActivity(i)
        } else {
            val i = Intent(myMainActivity!!.applicationContext, HelpActivity::class.java)
            myMainActivity!!.startActivity(i)
        }
        CTAG.leave()
    }
}
