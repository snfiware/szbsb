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
package de.snfiware.szbsb.main

import android.os.Environment
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import android.view.View
import de.snfiware.szbsb.MainActivity
import android.widget.TextView
import androidx.core.view.children
import de.snfiware.szbsb.R
import de.snfiware.szbsb.main.Chip0Handler.Companion.isRealChip
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getChipGroupByResId
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getTextViewByResId
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getViewByPosition
import de.snfiware.szbsb.util.assert
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.snfiware.szbsb.util.AcmtLogger
import java.io.File
import java.io.FileOutputStream


/** Diese Klasse implementiert zum einen das Verhalten für den Download-Button (onClick) und
 * zum anderen im CompanionObject die statischen Methoden um die Konfiguration aus der Datei
 * in die Oberfläche zu spiegeln (file2dlg) und für den umgekehrten Weg (dlg2file).
 */
class CfgSzHandler() : View.OnClickListener {

    constructor(mainActivity: MainActivity) : this() {
        CTAG.log( "ctor-this: "+ this.toString() + " old main: " +
            myMainActivity.toString() + " new main: " + mainActivity)
        //assert(myMainActivity==null , "singleton!" )
        myMainActivity = mainActivity
    }

    override fun onClick(v: View) {
        CTAG.enti("onClick", "Download-Button geklickt. Zunächst Konfig von UI in Datei speichern...")
        // Über die umkopierte Config-Datei res/raw/szconfig wird mit python kommuniziert
        // Aktuellen Stand der Einstellungen dorthin schreiben
        dlg2file()
        //
        //v.isEnabled = false // Knopf nur einmalig zulassen
        //
        var sRc = "create AsyncHandler..."
        CTAG.log(sRc)
        //
        val adh = AsyncDownloadHandler()
        try {
            // Hintergrundprozess starten
            adh.setView(v)
            sRc = "execute AsyncHandler..."
            CTAG.log(sRc)
            adh.execute("all")
            //
        } catch (e: Exception) {
            sRc = e.message!! //printStackTrace().toString()
            CTAG.e("sRc: ${sRc}; adh.err: ${adh.myErr}")
            Snackbar.make(v, adh.myErr, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        CTAG.leavi()
    }

    companion object {
        val CTAG = AcmtLogger("CfgH")
        //
        private var myMainActivity: MainActivity? = null

        private fun configFile(): String {
            // /data/user/0/de.snfiware.szbsb/files/szconfig
            return (myMainActivity?.getFilesDir()?.absolutePath + "/" +
                myMainActivity?.resources?.getResourceEntryName(R.raw.szconfig))
        }

        // Konfigurationsparameternamen in der Datei - die werden ausgelesen
        val cfgUsername = "username"
        val cfgPassword = "password"
        val cfgDownloadFolder = "downloadFolder"
        val cfgAllTopics = "allTopics"
        val cfgTopics = "myTopics"
        val cfgAllPages = "allPages"
        val cfgPages = "myPages"

        // Definition
        //var username = ""
        //var password = ""
        //var downloadFolder = "" //#downloadFolder = "/storage/emulated/0/Download/sz"
        //var allTopics = 'a','b','c'      // collection of strings of generated chips
        //var myTopics = 'b','c'           // collection of selected strings
        //var cntPages  = '1','2','3','4'  // collection of strings of generated chips
        //var myPages  = '1','3'           // collection of selected strings

        // Kopiert die mitgelieferte Konfigurationsdatei aus dem Ressourcenbereich (res/raw)
        // ins Benutzerverzeichnis - aber nur, falls dort noch keine existiert. Dies passiert
        // also i.d.R. nur beim allerersten Start. Updates an der Ressource werden 
        // in diesem Fall also nicht ohne weiteres an den Benutzer weitergegeben.
        private fun ensureMasterCfgFileExistsInUserScope() {
            CTAG.enter("ensureMCfgInUsr")
            val filename = configFile()
            val bExists = File(filename).exists()
            CTAG.d("User config file '$filename' exists: $bExists")
            if( !bExists ) {
                FileOutputStream(configFile()).use { out ->
                    myMainActivity?.resources?.openRawResource(R.raw.szconfig).use {
                        it?.copyTo(out)
                    }
                }
                CTAG.i("User config file '$filename' created.")
            }
            CTAG.leave()
        }

        fun getArrayOfChipStrings( s:String ) :MutableList<String> {
            var aRc :MutableList<String> = mutableListOf<String>()
            for( elem in s.split(',') ) {
                aRc.add( elem.trim().trim('\'').trim().toString() )
            }
            assert(aRc.isNotEmpty(), "aRc.size > 0")
            return aRc
        }

        fun createAllChips( v :View, s :String, resIdChipGroup :Int, layoutId :Int ) {
            val chipGroup = v.findViewById<ChipGroup>(resIdChipGroup)
            val inflater = LayoutInflater.from(chipGroup.context)
            val tags = getArrayOfChipStrings(s)
            CTAG.i("creating ${tags.indices} items...")
            for (index in tags.indices) {
                val chip = inflater.inflate(layoutId, chipGroup, false) as Chip
                chip.text= "${tags[index]}"
                chipGroup.addView(chip)
            }
        }

        fun createAllTopics( v :View, s :String ) {
            CTAG.enter( "createAllTopics", s )
            createAllChips( v, s, R.id.cgTopics, R.layout.chip_layout_topics )
            CTAG.leave()
        }

        fun createAllPages( v :View, s :String ) {
            CTAG.enter( "createAllPages", s )
            createAllChips( v, s, R.id.cgPages, R.layout.chip_layout_pages )
            CTAG.leave()
        }

        ////////////////////////////////////////////////////////////////////////////////
        // Liest die Konfigurationseinträge aus der Datei und setzt sie in die UI
        ////////////////////////////////////////////////////////////////////////////////
        // Basisbestückung zum Zeitpunkt onCreateView()
        //
        fun file2dlgCreation(v:View, asn:Int) {
            CTAG.enter( "file2dlgCrea","lade config für asn:$asn")
            //
            ensureMasterCfgFileExistsInUserScope()
            //
            File(configFile()).forEachLine {
                if(doSecHide(it)) CTAG.log("cfgFile-iterator: " + "***")
                             else CTAG.log("cfgFile-iterator: " + it)
                var i = 0
                var sTake = ""
                for (elem in it.split(" = ")) {
                    if( ++i == 1 ){
                        when {
                            cfgAllTopics == elem && asn == 1 -> sTake =
                                cfgAllTopics
                            cfgAllPages == elem && asn == 2 -> sTake =
                                cfgAllPages
                        }
                    } else {
                        when {
                            cfgAllTopics == sTake     -> createAllTopics(
                                v,
                                elem
                            )
                            cfgAllPages == sTake     -> createAllPages(
                                v,
                                elem
                            )
                        }
                        if( sTake != "" )
                            break
                    }
                }
            }
            CTAG.leave()
        }

        // Anschalten der Chips anhand der Config zum Zeitpunkt onViewCreated()
        // und Befüllen der Texte
        fun file2dlgSelection() {
            CTAG.enter( "file2dlgSel", "set programmatic mode" )
            Chip0Handler.setCCIP(true)
            try {
                file2dlgSelectionInt()
            } catch ( e :Exception ) {
                CTAG.leave_ex("reraise: $e")
                throw e
            } finally {
                CTAG.log("reset programmatic mode in finally")
                Chip0Handler.setCCIP(false)
            }
            CTAG.leave()
        }
        fun doSecHide(s:String) :Boolean{
            var bRc = false
            if(s.contains(cfgPassword) || s.contains(cfgUsername))
                bRc = true
            return(bRc)
        }
        private fun file2dlgSelectionInt() {
            CTAG.log( "Lade properties in die UI" )
            ensureMasterCfgFileExistsInUserScope()
            //
            CTAG.log("Lese Datei...")
            File(configFile()).forEachLine {
                if(doSecHide(it)) CTAG.log("cfgFile-iterator: " + "***")
                             else CTAG.log("cfgFile-iterator: " + it)
                var i = 0
                var sTake = ""
                for (elem in it.split(" = ")) {
                    if( ++i == 1 ){
                        when {
                            cfgUsername == elem       -> sTake = cfgUsername
                            cfgPassword == elem       -> sTake = cfgPassword
                            cfgDownloadFolder == elem -> sTake = cfgDownloadFolder
                            cfgTopics == elem         -> sTake = cfgTopics
                            cfgPages == elem          -> sTake = cfgPages
                        }
                    } else {
                        when {
                            cfgUsername == sTake       -> copyString(
                                elem,
                                R.id.editTextBsbId
                            )
                            cfgPassword == sTake       -> copyString(
                                elem,
                                R.id.editTextPassword
                            )
                            cfgDownloadFolder == sTake -> copyString(
                                elem,
                                R.id.editTextFolder
                            )
                            cfgPages == sTake          -> checkChips(
                                elem,
                                R.id.cgPages
                            )
                            cfgTopics == sTake         -> checkChips(
                                elem,
                                R.id.cgTopics
                            )
                        }
                        if( sTake != "" )
                            break
                    }
                }
            }
        }

        public fun getDownloadFolderFromUI() : String {
            val textView = getTextViewByResId(R.id.editTextFolder)
            val sRc = textView.text.toString().trim()
            CTAG.log("getDownloadFolderFromUI: $sRc")
            return(sRc)
        }

        // do not place logging in here - user+password is handled herein
        private fun copyString(s:String, resId:Int) {
            val textView = getTextViewByResId(resId)
            var sText = s.trim().trim('\'').trim()
            if( resId == R.id.editTextFolder )
                if( sText.length == 0 || sText == "./SZ" ) {
                    val f = myMainActivity!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    sText = f.absolutePath
                    sText += "/SZ"
                }
            textView.text = sText
        }

        private fun checkChips(s:String, resId:Int) {
            CTAG.enter("checkChips", "s: $s; resId: $resId")
            var checkedCount = 0
            val v = getChipGroupByResId(resId)
            //Chip0Handler.setCCIP(true)
            var lastChipChecked :Chip? = null
            try {
                val myChipGroup = v //as ChipGroup
                for( oneChild in myChipGroup.children ){
                    var oneChip : Chip?
                    try {
                        oneChip = oneChild as Chip
                    } catch (e: Exception) {
                        oneChip = null
                    }
                    if( oneChip != null ) {
                        oneChip.isChecked = false // assume off
                        //CTAG.log( "view: " + v.id + " chip: " + oneChip.text + " was unchecked (pauschal)")
                        for( elem in s.split(',') ) {
                            if( elem.trim().trim('\'').trim().toString() == oneChip.text.trim().toString() ) {
                                ++checkedCount
                                if (lastChipChecked == null) {
                                    CTAG.d("remember chip '${oneChip.text}' to recall at the end")
                                    lastChipChecked = oneChip // merken bis zum Schluss, damit dann dort "C0-Alles" berücksichtigt wird
                                } else {
                                    oneChip.isChecked = true  // alle übrigen gleich anstellen - switch on
                                    CTAG.v( "view: " + v.id + " chip: " + oneChip.text + " was checked (by cfg)")
                                }
                                break
                            }
                        }
                    }
                }
            } finally {
                CTAG.log( "finally")
                Chip0Handler.setCCIP(false)
                if (lastChipChecked != null) {
                    CTAG.log( "change chip state...")
                    lastChipChecked.isChecked = true
                    CTAG.log( "view: " + v.id + " chip: " + lastChipChecked.text + " was checked (cfg-deferred)")
                }
                Chip0Handler.setCCIP(true)
            }
            CTAG.leavi("$checkedCount Chip(s) gecheckt")
        }

        ////////////////////////////////////////////////////////////////////////////////
        // Schreibt die im Dialog vorhandenen Konfigurationseinträge in die Datei weg
        ////////////////////////////////////////////////////////////////////////////////
        fun dlg2file() {
            CTAG.enter( "dlg2file" )
            try {
                val f = File(configFile())
                var cfgContent: String
                cfgContent = getCfgFileTextRowFromUiText(
                    0,
                    R.id.editTextBsbId
                ) + "\n" +
                        getCfgFileTextRowFromUiText(
                            0,
                            R.id.editTextPassword
                        ) + "\n" +
                        getCfgFileTextRowFromUiText(
                            0,
                            R.id.editTextFolder
                        ) + "\n" +
                        getCfgFileTextRowsFromUiChipGroup(
                            1,
                            R.id.cgTopics
                        ) + "\n" +
                        getCfgFileTextRowsFromUiChipGroup(
                            2,
                            R.id.cgPages
                        ) + "\n"
                f.writeText( cfgContent )
            } finally {
                CTAG.leave()
            }
        }

        fun getCfgFileTextRowFromUiText( tabPos:Int, resId:Int ) :String {
            val v = getViewByPosition(tabPos).findViewById<View>(resId)
            val textView = v as TextView
            var cfgRow = ""
            when {
                resId == R.id.editTextBsbId    -> cfgRow = cfgUsername + " = '" + textView.text.trim() + "'"
                resId == R.id.editTextPassword -> cfgRow = cfgPassword + " = '" + textView.text.trim() + "'"
                resId == R.id.editTextFolder   -> cfgRow = cfgDownloadFolder + " = '" + textView.text.trim() + "'"
            }
            return cfgRow
        }
        
        fun getCfgFileTextRowsFromUiChipGroup( tabPos:Int, resId:Int ) :String {
            CTAG.enter( "getCfgTxtRows", "tabPos: $tabPos; resId: $resId" )
            var sRc = ""
            try {
                val v = getViewByPosition(tabPos).findViewById<View>(resId)
                var cfgRowCur = ""
                var cfgRowAll = ""
                when {
                    resId == R.id.cgTopics -> {cfgRowCur = cfgTopics + " = "; cfgRowAll = cfgAllTopics + " = "}
                    resId == R.id.cgPages  -> {cfgRowCur = cfgPages + " = "; cfgRowAll = cfgAllPages + " = "}
                }
                val myChipGroup = v as ChipGroup
                //
                var i = 0
                var j = 0
                for( oneChild in myChipGroup.children ) {
                    if( isRealChip(oneChild) ) { // c0 "Alles-Knopf" + Sep ignorieren
                        var oneChip: Chip?
                        try {
                            oneChip = oneChild as Chip
                        } catch (e: Exception) {
                            oneChip = null
                        }
                        if (oneChip != null) {
                            // alle
                            j += 1
                            if (j > 1) {
                                cfgRowAll += ","
                            }
                            cfgRowAll += "'" + oneChip.text + "'"
                            // nur die abgehakten
                            if (oneChip.isChecked) {
                                i += 1
                                if (i > 1) {
                                    cfgRowCur += ","
                                }
                                CTAG.log("view: " + v.id + " chip: " + oneChip.text + " is checked")
                                cfgRowCur += "'" + oneChip.text + "'"
                            }
                        }
                    }
                }
                if( i == 0 ) {
                    cfgRowCur += "'GARNIX'"
                }
                sRc = cfgRowCur + "\n" + cfgRowAll
            }
            finally {
                CTAG.leave(sRc)
            }
            return sRc
        }
    }
}

