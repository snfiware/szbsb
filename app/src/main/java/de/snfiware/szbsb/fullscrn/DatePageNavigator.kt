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

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.R
import de.snfiware.szbsb.main.AsyncDownloadHandler
import de.snfiware.szbsb.util.AcmtLogger
import java.io.File

typealias tStringTree = MutableList<Pair<String,MutableList<File>>>

/**
 * DatePageNavigator implementiert für den Fullscreen-Modus ein zweistufiges Navigationsmenü.
 * Bei der Konstruktion wird das myRootDir-Verzeichnis ausgelesen und alle Ordnernamen darin in die
 * erste Ebene einsortiert (erwartet: Datums, funktioniert aber generisch). Alle PDFs innerhalb
 * eines Ordners werden in die zweite Ebene einsortiert.
 * Die Ebenen bestehen jeweils aus zwei Knöpfen (Links/Rechts) und einem Spinner-Element, welches
 * aufgeklappt werden kann und größere Sprünge als das einschrittige Links/Rechts ermöglicht.
 *
 * Diese Klasse kümmert sich um das Menü samt dem einfachen Eventhandling und das Modell.
 * Die Wisch-Gesten-Steuerung (swipe) ist ausgelagert (FullscreenActivity).
 */
class DatePageNavigator {
    companion object { val CTAG = AcmtLogger("DPN") }
    //
    val myFullScrAct :FullscreenActivity
    val myRootDirString :String
    val myRootDir :File
    //
    val myBtnDatLinks  :Button
    val myBtnDatRechts :Button
    val myBtnPdfLinks  :Button
    val myBtnPdfRechts :Button
    //
    val myPdfView :MyPdfView
    val myTxtInf :TextView // Info-Textfeld im Zentrum z.B. wenn keine PDFs da sind
    val myDateSpinner :Spinner // Download-Datum; entspricht einem Ordner im Verzeichnis myRootDir
    val myPageSpinner :Spinner // entspricht einer Datei im Ordner (=einer darstellbaren Seite)
    //
    private val pattern = "yyyy-MM-dd"
    val myStringTree :tStringTree = mutableListOf()
    var myCurDatePointer :Int = -1
    var myCurPagePointer :Int = -1
    //
    val list01 :tStringList // Vorwarnliste
    val list02 :tStringList // Abgelaufen
    //
    constructor(fullScrAct : FullscreenActivity, sMyRootDir :String ) {
        CTAG.enter("ctor","fullScrAct: $fullScrAct; sMyRootDir: $sMyRootDir")
        myFullScrAct = fullScrAct
        myRootDirString = sMyRootDir
        //
        val f = File(myRootDirString)
        CTAG.log("canRead '" + myRootDirString + "':" + f.canRead().toString() + "; isDir: " + f.isDirectory.toString() )
        myRootDir = f
        //
        CTAG.log("registering navi listeners...")
        myBtnDatLinks  = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnDatumLinks ); myBtnDatLinks.setOnClickListener  { onClickDatumLinks() }
        myBtnDatRechts = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnDatumRechts); myBtnDatRechts.setOnClickListener { onClickDatumRechts() }
        myBtnPdfLinks  = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnPdfLinks   ); myBtnPdfLinks.setOnClickListener  { onClickPdfLinks() }
        myBtnPdfRechts = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnPdfRechts  ); myBtnPdfRechts.setOnClickListener { onClickPdfRechts() }
        CTAG.log("registered listeners.")
        //
        myPdfView= myFullScrAct.findViewById<MyPdfView> (R.id.fullscreen_content)
        myTxtInf = myFullScrAct.findViewById<TextView>(R.id.textViewFullScrnInfo)
        myDateSpinner = myFullScrAct.findViewById<Spinner>(R.id.spinnerFullScrnDatum)
        myPageSpinner = myFullScrAct.findViewById<Spinner>(R.id.spinnerFullScrnPdf)
        //
        CTAG.log("registering icon listeners...")
        val icon1 = myFullScrAct.findViewById<ImageView>(R.id.imageViewDatum)
        icon1.setOnClickListener { _ -> myDateSpinner.performClick() }
        val icon2 = myFullScrAct.findViewById<ImageView>(R.id.imageViewPdf)
        icon2.setOnClickListener { _ -> myPageSpinner.performClick() }
        //
        CTAG.log("populateTree...")
        populateTree()
        // Initialbefüllung des Datum Spinners - der ist mengentechnisch stabil
        // über die gesamte Lebenszeit der FullscreenView (sie wird jedes mal neu angelegt, wenn
        // zwischen den Activities gewechselt wird)
        //
        CTAG.log("Fill Adapter for DateSpinner...") // der füllt die Daten in die UI
        var i = 0
        val aa = getArrayAdapter()
        for( s in myStringTree ) {
            i += 1
            aa.add( s.first
//                    + " #${i.toString()}"
                    + " (${i.toString()}/${myStringTree.size})"
//                    + " - ${s.second.size} PDF(s)"
                    + " [${s.second.size}]"
            )
        }
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        CTAG.log("Set Adapter and toggleSpinnerListeners...")
        if( aa.count > 0) {
            myDateSpinner.setAdapter(aa)
        }
        //
        list01 = DeleteHandler.getListOfOverduePdfFolders(myStringTree, pattern, 90).map { it.substring(0,10) }
        list02 = DeleteHandler.getListOfOverduePdfFolders(myStringTree, pattern,180).map { it.substring(0,10) }
        CTAG.log("list01: $list01; list02: $list02")
        //
        //if (FullscreenActivity.fsa.myNavi == null) {
        toggleSpinnerListeners(true)
        //}
        CTAG.leave()
    }
    //
    private fun getArrayAdapter() : ArrayAdapter<String> {
        CTAG.enter("getAAObj")
        val aa = object // create an unnamed derived class, override method and return object of this class
        /*START-iCLASS*/:ArrayAdapter<String>(myFullScrAct.applicationContext, android.R.layout.simple_spinner_item) {
            //
            fun markText(position: Int, v: View?, parent: ViewGroup, bIsDropdown :Boolean): Int {
                var iApplied = -1
                var rowcontent = "init"
                if(v != null) {
                    val tv = v as TextView
                    rowcontent = tv.text.substring(0,10)
                    if(list02.contains(rowcontent)) {
                        iApplied = 2
                        tv.setBackgroundColor(Color.RED)
                    }
                    else if(list01.contains(rowcontent)) {
                        iApplied = 1
                        tv.setTextColor(Color.RED)
                    }
                    else {
                        // Das ist nicht ganz sauber, da die Farbe hart verdrahtet wird. Saubere
                        // Lösung wäre sich vor dem allerersten Setzen das zu merken und hier auf
                        // das Gemerkte zurückzusetzen.
                        iApplied = 0
                        tv.setTextColor(when(bIsDropdown){ true -> Color.BLACK else -> Color.WHITE })
                        tv.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                CTAG.log("'${rowcontent}';' iApplied: $iApplied; bDD: $bIsDropdown; list01: $list01; list02: $list02; position: $position; v: $v; parent: $parent")
                return(iApplied)
            }
            //
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                markText(position, v, parent, false)
                return v
            }
            //
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                markText(position, v, parent, true)
                return v
            }
        /*END-iCLASS*/}
        CTAG.leave()
        return(aa)
    }
    //
    private fun populateTree() {
        CTAG.enter("populateTree")
        var pdfOverallCount = 0
        if( myRootDir.exists() && myRootDir.canRead() && myRootDir.list().size > 0 ) {
            CTAG.d("accessible rootDir")
            for (f1 in myRootDir.listFiles().sorted()) {
                if (f1.isDirectory() && f1.canRead()) {
                    CTAG.enter("populatePdf", "f1.name: " + f1.name + " initializing date collection...")
                    val mlo = mutableListOf<File>()
                    myStringTree.add(Pair(f1.name, mlo))
                    //myTxtDat.setTag(0,f1.name)
                    //
                    CTAG.d("f1.name: " + f1.name + " looping over contents...")
                    for (f2 in f1.listFiles().sorted()) {
                        if (f2.isFile() && f2.canRead() && f2.name.contains(
                                ".pdf",
                                ignoreCase = true
                            )
                        ) {
                            mlo.add(f2)
                            //myTxtPdf.setTag(0,f2)
                            //CTAG.d( "f2.name: " + f2.name + " added")
                        }
                    }
                    CTAG.leave("populated tree node '${f1.name}' with ${mlo.size} files.")
                    pdfOverallCount += mlo.size
                }
            }
        }
        CTAG.leavi("populated ${myStringTree.size} nodes with ${pdfOverallCount} PDF(s)" )
    }
    // looks duplicate but is for date + page slightly different - both needs to be registered...
    private fun toggleSpinnerListeners( bStartListening : Boolean ) {
        CTAG.enter("toggleSL","Date + Page; bListening: $bStartListening")
        if( bStartListening ) {
            CTAG.d("Register Listeners on DateSpinner...")
            myDateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val curIdxPosition = getCurDatePointerIndex()
                    CTAG.i("Date:onItemSelected - pos: ${position}; cur-pos: ${curIdxPosition}")
                    if( position != curIdxPosition ) {
                        setCurPointer(position, 0)
                    } // else // ignoring
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    CTAG.i("Date:onNthngSel")
                }
            }
            //
            CTAG.d("Register Listeners on PageSpinner...")
            myPageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val curIdxPosition = getCurPagePointerIndex()
                    CTAG.i("Page:onItemSelected - pos: ${position}; cur-pos: ${curIdxPosition}")
                    if( position != curIdxPosition ) {
                        setCurPointer(myCurDatePointer, position)
                    } // else // ignoring
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    CTAG.i("Page:onNthngSel")
                }
            }
        } else {
            myDateSpinner.onItemSelectedListener = null
            myPageSpinner.onItemSelectedListener = null
        }
        CTAG.leave()
    }
    //
    fun isLastPdfInFolderOrFolderAlreadyEmpty() : Boolean {
        var bRc : Boolean = myStringTree.get(myCurDatePointer).second.size == 1 ||
                            myStringTree.get(myCurDatePointer).second.size == 0
        CTAG.i("isLastPdfInFolderOrFolderAlreadyEmpty: $bRc")
        return bRc
    }
    //
    fun isLastFolderInRoot() : Boolean {
        var bRc : Boolean = myRootDir.list().size == 1
        CTAG.i("isLastFolderInRoot: $bRc")
        return bRc
    }
    //
    fun isRootEmpty() : Boolean {
        var bRc : Boolean = myRootDir.list().size == 0
        CTAG.i("isRootEmpty: $bRc")
        return bRc
    }
    //
    fun deleteCurPdf( d :DeleteHandler ) : Boolean {
        if( myCurPagePointer == myStringTree.get(myCurDatePointer).second.lastIndex ) {
            // letztes
            d.myCurDatePointer = this.myCurDatePointer
            d.myCurPagePointer = this.myCurPagePointer - 1 // eins runter zählen
        } else {
            // mittendrin - stehen lassen
            d.myCurDatePointer = this.myCurDatePointer
            d.myCurPagePointer = this.myCurPagePointer
        }
        //
        val f :File = myStringTree.get(myCurDatePointer).second.get(myCurPagePointer)
        val bRc = f.delete()
        CTAG.i("deleteCurPdf - success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }
    //
    fun deleteCurDateFolder( d :DeleteHandler ) : Boolean {
        if( myCurDatePointer == myStringTree.lastIndex ) {
            // letztes
            d.myCurDatePointer = this.myCurDatePointer - 1 // eins runter zählen
            d.myCurPagePointer = 0  // einfach erstes im neuen Set
        } else {
            // mittendrin
            d.myCurDatePointer = this.myCurDatePointer     // stehen lassen
            d.myCurPagePointer = 0  // einfach erstes im neuen Set
        }
        //
        val dir :String = myStringTree.get(myCurDatePointer).first
        val f = File(myRootDir.absolutePath + "/" + dir)
        val bRc = f.deleteRecursively()
        CTAG.i("delCurDateFldr - success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }
    //
    fun deleteFolders( d :DeleteHandler, list :tStringList ) : Boolean {
        CTAG.enter("deleteFolders","list: $list")
        var bRc = false
        var i = 0
        var step = "init"
        try {
            if (myRootDir.exists() && myRootDir.canRead() && myRootDir.list().size > 0) {
                step = "loop"
                for (f1 in myRootDir.listFiles().sorted()) {
                    step = "checking ${f1.name}"
                    val dirName = f1.name.substring(0, 10)
                    if (list.contains(dirName) && f1.isDirectory() && f1.canWrite()) {
                        step = "deleting ${f1.name}"
                        bRc = f1.deleteRecursively()
                        if (bRc) {
                            ++i
                        } else {
                            CTAG.e("error deleting '${f1.absolutePath}'")
                            break
                        }
                    }
                }
            }
        } catch (e:java.lang.Exception) {
            CTAG.e("EX! "+e.message)
            e.printStackTrace()
            bRc = false
        }
        //
        if(bRc) {
            d.myCurDatePointer = 0
            d.myCurPagePointer = 0
            FullscreenActivity.showSnack("$i Ordner gelöscht.")
        } else {
            FullscreenActivity.showSnack("Interner Fehler bei: $step")
        }
        CTAG.leave("deleted $i folders - bRc: $bRc - step: $step")
        return(bRc)
    }
    //
    fun getCurRootFolder() : File {
        var fRc :File? = myRootDir
        return fRc!!
    }
    //
    private fun getCurDatePointerIndex() :Int {return(myCurDatePointer)}
    private fun getCurPagePointerIndex() :Int {return(myCurPagePointer)}
    private fun setCurPointer( idxDate :Int, idxPage :Int ) {
        CTAG.enter("setCurPointer", "myRootDir.readable: ${myRootDir.canRead()}"+
                "; idxDate: $idxDate; idxPage: $idxPage")
        //
        var iDate = idxDate
        var iPage = idxPage
        var fn: String = "fnix"
        var dir: String = "dnix"
        if(myRootDir.canRead() && myRootDir.list().size > 0) {
            try {
                // Rollover
                if( iDate < 0 ) iDate = myStringTree.lastIndex
                if( iDate > myStringTree.lastIndex ) iDate = 0
                dir = myStringTree.get(iDate).first
                //
                if( iPage < 0 ) iPage = myStringTree.get(iDate).second.lastIndex
                if( iPage > myStringTree.get(iDate).second.lastIndex ) iPage = 0
                CTAG.d("after Rollover - " +
                        "iDate: ${iDate.toString()}; iPage: ${iPage.toString()}" )
                //
                // Get file at desired location
                var f = File("dummy.pdf")
                val bPageIdxIsValid = iPage >= 0 && iPage < myStringTree.get(iDate).second.size
                if( bPageIdxIsValid ) {
                    f = myStringTree.get(iDate).second.get(iPage)
                    fn = f.name // for logging only
                }
                /////////////////////////////
                if( bPageIdxIsValid ) {
                    if(f.canRead()) {
                        myPdfView.loadPdfFromFile(f) // start loading pdf
                        myPdfView.bringToFront()
                        myTxtInf.visibility = View.INVISIBLE
                    } else {
                        myPdfView.recycle() // reset to empty
                        //
                        myTxtInf.text = "PDF '" + f.name +
                                "' konnte nicht gelesen werden."
                        myTxtInf.visibility = View.VISIBLE
                        myTxtInf.bringToFront()
                    }
                } else {
                    myPdfView.recycle() // reset to empty
                    //
                    myTxtInf.text = "Keine PDFs unter '" + myRootDirString + "/" + dir +
                         "' oder fehlende Berechtigungen."
                    myTxtInf.visibility = View.VISIBLE
                    myTxtInf.bringToFront()
                }
                //
                CTAG.d("setting pointers, adjust spinners...")
                if( myCurDatePointer != iDate ) {
                    CTAG.d("date pointer changing from ${myCurDatePointer} to ${iDate}")
                    myCurDatePointer = iDate
                    myDateSpinner.setSelection(iDate)
                    //
                    // (re)fill Pages Spinner
                    val aa = getArrayAdapter()
                    val pagesList = myStringTree.get(iDate).second
                    var i = 0
                    for( pdf in pagesList ) {
                        i += 1
                        aa.add( pdf.name.removeSuffix(".pdf") + " (${i.toString()}/${pagesList.size})" )
                    }
                    aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    myPageSpinner.setAdapter(aa)
                }
                //
                CTAG.d("page pointer changing from ${myCurPagePointer} to ${iPage}")
                myCurPagePointer = iPage
                myPageSpinner.setSelection(iPage)
                //
            } catch (e: Exception) {
                CTAG.e("EXC! " + e.message)
                e.printStackTrace()
                //
                myTxtInf.text = "Interner Fehler."
                myTxtInf.visibility = View.VISIBLE
                myTxtInf.bringToFront()
            }
        } else {
            myPdfView.recycle() // reset to empty
            //
            myTxtInf.text = "Keine Lese-Berechtigung für '${myRootDir.absolutePath}' oder Ordner leer."
            myTxtInf.visibility = View.VISIBLE
            myTxtInf.bringToFront()
        }
        //
        CTAG.leavi("now showing dir: " + dir
                + "; PDF: " + fn + "; within: "+myRootDir.absolutePath)
    }
    //
    fun showMostCurrentPdf() {
        var step = "init"
        val fn = AsyncDownloadHandler.sFirstDownloadedFile
        CTAG.enter("showMostCurPdf", "ADH.sFirstDownloadedFile: '$fn'")
        //
        if( fn == "") {
            step = "no download in current session"
            val sel = MainActivity.getCheckedRadioButtonId()
            val folder = when(sel){
                R.id.rbHauptausgabe -> "SZ"
                R.id.rbMagazin -> "Magazin"
                R.id.rbExtra -> "Extra"
                else -> ""
            }
            step += " - selecting most recent '${folder}'"
            CTAG.log(step)
            val idx = myStringTree.indexOf(myStringTree.findLast { T -> T.first.endsWith(folder) })
            setCurPointer(idx, 0)
        }
        else {
            step = "lookup file in myStringTree..."
            CTAG.log(step)
            val f = File(fn)
            val folder = f.parentFile.name
            var i = 0
            var j = 0
            for( dat in myStringTree ) { // .sortedByDescending { T -> T.first } # would be better for performance - but indexes get mixed up
                if(folder == dat.first) {
                    val pagesList = dat.second
                    j = 0
                    for (pdf in pagesList) {
                        if (pdf.name == f.name) {
                            CTAG.log("found file in myStringTree at idx: $i/$j")
                            break
                        }
                        j += 1
                    }
                    if(j < pagesList.size) {
                        break
                    }
                }
                i += 1
            }
            if(i < myStringTree.size) {
                step = "download present in current session - showing first pdf"
                setCurPointer(i, j)
            } else {
                step = "download present in current session - BUT NOT FOUND - showing standard"
                CTAG.e(step)
                setCurPointer(myStringTree.lastIndex, 0)
            }
        }
        CTAG.leave(step)
    }
    //
    fun showNextPdfAfterDelete(d: DeleteHandler) {
        CTAG.i("showNextPdfAfterDelete")
        setCurPointer(d.myCurDatePointer,d.myCurPagePointer)
    }
    //
    fun onClickDatumLinks()  {CTAG.i("DatumLinks") ; setCurPointer(myCurDatePointer - 1, 0)}
    fun onClickDatumRechts() {CTAG.i("DatumRechts"); setCurPointer(myCurDatePointer + 1, 0)}
    fun onClickPdfLinks()    {CTAG.i("PdfLinks")   ; setCurPointer(myCurDatePointer,myCurPagePointer - 1)}
    fun onClickPdfRechts()   {CTAG.i("PdfRechts")  ; setCurPointer(myCurDatePointer,myCurPagePointer + 1)}
}