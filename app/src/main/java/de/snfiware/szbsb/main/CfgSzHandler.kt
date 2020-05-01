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
package de.snfiware.szbsb.main

import android.os.Environment
import com.google.android.material.snackbar.Snackbar
import android.view.View
import de.snfiware.szbsb.MainActivity
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.core.view.children
import com.example.sztab.R
import de.snfiware.szbsb.main.Chip0Handler.Companion.isRealChip
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getChipByIdx
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getChipGroupByResId
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getTextViewByResId
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getViewByPosition
import de.snfiware.szbsb.util.assert
import de.snfiware.szbsb.util.convertDpToPxFloat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt


/** Diese Klasse implementiert zum einen das Verhalten für den Download-Button (onClick) und
 * zum anderen im CompanionObject die statischen Methoden um die Konfiguration aus der Datei
 * in die Oberfläche zu spiegeln (file2dlg) und für den umgekehrten Weg (dlg2file).
 */
class CfgSzHandler() : View.OnClickListener {

    constructor(mainActivity: MainActivity) : this() {
        Log.i("CSH::ctor", "this: "+ this.toString() + " old main: " +
            myMainActivity.toString() + " new main: " + mainActivity)
        //assert(myMainActivity==null , "singleton!" )
        myMainActivity = mainActivity
    }

    override fun onClick(v: View) {
        Log.i("CSH::onClick","->")
        // Über die umkopierte Config-Datei res/raw/szconfig wird mit python kommuniziert
        // Aktuellen Stand der Einstellungen dorthin schreiben
        dlg2file()
        //
        //v.isEnabled = false // Knopf nur einmalig zulassen
        //
        var sRc : String = "starte..."
        //sRc = "Prozess gestartet"
        //Log.d("CSH::onClick",sRc)
        //Snackbar.make(v, sRc, Snackbar.LENGTH_LONG)
        //    .setAction("Action", null).show()
        //
        val adh = AsyncDownloadHandler()
        try {
            // Hintergrundprozess starten
            adh.setView(v)
            adh.execute("all")
            //
        } catch (e: Exception) {
            sRc = e.message!! //printStackTrace().toString()
            Log.i("CSH::onClick","sRc: ${sRc}; adh.err: ${adh.myErr}")
            Snackbar.make(v, adh.myErr, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        Log.i("CSH::onClick","<-")
    }

    companion object {
        private var myMainActivity: MainActivity? = null

        private fun configFile(): String {
            // /data/user/0/com.example.de.snfiware.szbsb/files/szconfig
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
            if( ! File(configFile()).exists() ) {
                FileOutputStream(configFile()).use { out ->
                    myMainActivity?.resources?.openRawResource(R.raw.szconfig).use {
                        it?.copyTo(out)
                    }
                }
            }
        }

        fun getArrayOfChipStrings( s:String ) :MutableList<String> {
            var aRc :MutableList<String> = mutableListOf<String>()
            for( elem in s.split(',') ) {
                aRc.add( elem.trim().trim('\'').trim().toString() )
            }
            assert(aRc.isNotEmpty(), "aRc.size > 0")
            return aRc
        }

        fun createAllTopics( v :View, s: String ) {
            Log.i( "createAllChips", "->" )
            val chipGroup = v.findViewById<ChipGroup>(R.id.cgTopics)
            val chip0 :Chip = getChipByIdx(chipGroup,0)
            //var layoutChip0 = chip0.layoutParams
            val width = MATCH_PARENT
            val height= chip0.chipMinHeight.roundToInt() // convertDpToPxFloat(v.context, chip0.chipMinHeight).roundToInt()
            //val tags = arrayOf("a1","b2","c3","d4","e5","f6","g7","h8","i9","j10","k11","a1","b2","c3","d4","e5","f6","g7","h8","i9","j10","k11")
            val tags =
                getArrayOfChipStrings(
                    s
                )
            for (index in tags.indices) {
                val chip = Chip(chipGroup.context)
                chip.text= "${tags[index]}"
                chip.setTypeface( chip0.typeface, chip0.typeface.style )

                chip.isClickable = true
                chip.isCheckable = true

                chipGroup.addView(chip,width,height)
            }
            Log.i( "createAllChips", "<-" )
        }

        fun createAllPages( v :View, s: String ) {
            Log.i( "createAllPages", "->" )
            val chipGroup = v.findViewById<ChipGroup>(R.id.cgPages)
            val chip0 :Chip = getChipByIdx(chipGroup,0)
            //#
            //LayoutInflater.from(chipGroup.context).inflate(R.layout.fragment_chip_templates, v, false)
            //
            //var layoutChip0 = chip0.layoutParams
            //val height = 32
            val height= convertDpToPxFloat(
                v.context,
                chip0.height * 30f / 27f
            )
            //val width = height
            //val tags = arrayOf("a1","b2","c3","d4","e5","f6","g7","h8","i9","j10","k11","a1","b2","c3","d4","e5","f6","g7","h8","i9","j10","k11")
            val tags =
                getArrayOfChipStrings(
                    s
                )
            for (index in tags.indices) {
                //val chip = Chip(chipGroup.context,null, R.attr.CustomChipChoiceStyle)
                val chip = Chip(chipGroup.context,null)
                //chip.chipMinHeight = height
                chip.height = height.toInt()
                chip.width = height.toInt()
                //chip.minWidth = height.toInt()
                chip.chipCornerRadius = height
                chip.chipStartPadding = 1f
                chip.chipEndPadding = 0f
                //
                chip.text= "${tags[index]}"
                chip.setTypeface( chip0.typeface, chip0.typeface.style )

                chip.isClickable = true
                chip.isCheckable = true

                chipGroup.addView(chip)
            }
            Log.i( "createAllPages", "<-" )
        }

        ////////////////////////////////////////////////////////////////////////////////
        // Liest die Konfigurationseinträge aus der Datei und setzt sie in die UI
        ////////////////////////////////////////////////////////////////////////////////
        // Basisbestückung zum Zeitpunkt onCreateView()
        //
        fun file2dlgCreation(v:View, asn:Int) {
            Log.i( "file2dlgCreation", "->" )
            //
            ensureMasterCfgFileExistsInUserScope()
            //
            File(configFile()).forEachLine {
                Log.d("file2dlgCreation","cfgFile-iterator: " + it)
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
            Log.i( "file2dlgCreation", "<-" )
        }

        // Anschalten der Chips anhand der Config zum Zeitpunkt onViewCreated()
        // und Befüllen der Texte
        fun file2dlgSelection() {
            Log.i( "file2dlgSelection", "->" )
            //
            ensureMasterCfgFileExistsInUserScope()
            //
            File(configFile()).forEachLine {
                Log.d("file2dlg","cfgFile-iterator: " + it)
                var i = 0
                var sTake = ""
                for (elem in it.split(" = ")) {
                    if( ++i == 1 ){
                        when {
                            cfgUsername == elem       -> sTake =
                                cfgUsername
                            cfgPassword == elem       -> sTake =
                                cfgPassword
                            cfgDownloadFolder == elem -> sTake =
                                cfgDownloadFolder
                            cfgTopics == elem         -> sTake =
                                cfgTopics
                            cfgPages == elem          -> sTake =
                                cfgPages
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
            Log.i( "file2dlgSelection", "<-" )
        }

        public fun getDownloadFolderFromUI() : String {
            val textView = getTextViewByResId(R.id.editTextFolder)
            return( textView.text.toString().trim() )
        }

        private fun copyString(s:String, resId:Int) {
            val textView = getTextViewByResId(resId)
            var sText = s.trim().trim('\'').trim()
            if( resId == R.id.editTextFolder )
                if( sText == "./SZ" || sText.length == 0 ) {
                    val f = myMainActivity!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    sText = f.absolutePath
                    sText += "/SZ"
                }
            textView.text = sText
        }

        private fun checkChips(s:String, resId:Int) {
            val v = getChipGroupByResId(resId)
            Chip0Handler.setCCIP(true)
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
                        System.out.println( "view: " + v.id + " chip: " + oneChip.text + " was unchecked (pauschal)")
                        for( elem in s.split(',') ) {
                            if( elem.trim().trim('\'').trim().toString() == oneChip.text.trim().toString() ) {
                                if (lastChipChecked == null) {
                                    lastChipChecked = oneChip // merken bis zum Schluss, damit dann dort "C0-Alles" berücksichtigt wird
                                } else {
                                    oneChip.isChecked = true  // alle übrigen gleich anstellen - switch on
                                    System.out.println( "view: " + v.id + " chip: " + oneChip.text + " was checked (cfg)")
                                }
                                break
                            }
                        }
                    }
                }
            } finally {
                Chip0Handler.setCCIP(false)
                if (lastChipChecked != null) {
                    lastChipChecked.isChecked = true
                    System.out.println( "view: " + v.id + " chip: " + lastChipChecked.text + " was checked (cfg-deferred)")
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // Schreibt die im Dialog vorhandenen Konfigurationseinträge in die Datei weg
        ////////////////////////////////////////////////////////////////////////////////
        fun dlg2file() {
            Log.i( "dlg2file", "->" )
            //
            val f = File(configFile())
            var cfgContent :String
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
            Log.i( "dlg2file", "<-" )
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
                            System.out.println("view: " + v.id + " chip: " + oneChip.text + " is checked")
                            cfgRowCur += "'" + oneChip.text + "'"
                        }
                    }
                }
            }
            if( i == 0 ) {
                cfgRowCur += "'GARNIX'"
            }
            return cfgRowCur + "\n" + cfgRowAll
        }
    }
}

