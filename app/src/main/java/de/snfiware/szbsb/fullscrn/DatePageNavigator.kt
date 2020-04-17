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

import android.util.Log
import android.view.View
import android.widget.*
import com.example.sztab.R
import java.io.File

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
class DatePageNavigator { //}: View.OnClickListener { //AdapterView.OnItemSelectedListener {
    var myFullScrAct : FullscreenActivity
    var myRootDirString :String = ""
    var myRootDir :File
    //
    var myBtnDatLinks  :Button
    var myBtnDatRechts :Button
    var myBtnPdfLinks  :Button
    var myBtnPdfRechts :Button
    //
    var myPdfView : MyPdfView
    var myTxtInf :TextView // Info-Textfeld im Zentrum z.B. wenn keine PDFs da sind
    var myDateSpinner :Spinner // Download-Datum; entspricht einem Ordner im Verzeichnis myRootDir
    var myPageSpinner :Spinner // entspricht einer Datei im Ordner (=einer darstellbaren Seite)
    //
    var myStringTree : MutableList<Pair<String,MutableList<File>>> = mutableListOf()
    var myCurDatePointer : Int = -1
    var myCurPagePointer : Int = -1
    //
    constructor(fullScrAct : FullscreenActivity, myRootDir :String ) {
        Log.i("DPN::ctor","->")
        myFullScrAct = fullScrAct
        myRootDirString = myRootDir
        //
        val f = File(myRootDir)
        Log.d("DPN::ctor", "canRead '" + myRootDir + "':" + f.canRead().toString() + "; isDir: " + f.isDirectory.toString() )
        this.myRootDir = f
        //
        Log.v("DPN::ctor","registering navi listeners...")
        myBtnDatLinks  = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnDatumLinks ); myBtnDatLinks.setOnClickListener  { onClickDatumLinks() }
        myBtnDatRechts = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnDatumRechts); myBtnDatRechts.setOnClickListener { onClickDatumRechts() }
        myBtnPdfLinks  = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnPdfLinks   ); myBtnPdfLinks.setOnClickListener  { onClickPdfLinks() }
        myBtnPdfRechts = myFullScrAct.findViewById<Button>(R.id.buttonFullScrnPdfRechts  ); myBtnPdfRechts.setOnClickListener { onClickPdfRechts() }
        Log.v("DPN::ctor","registered listeners.")
        //
        myPdfView= myFullScrAct.findViewById<MyPdfView> (R.id.fullscreen_content)
        myTxtInf = myFullScrAct.findViewById<TextView>(R.id.textViewFullScrnInfo)
        myDateSpinner = myFullScrAct.findViewById<Spinner>(R.id.spinnerFullScrnDatum)
        myPageSpinner = myFullScrAct.findViewById<Spinner>(R.id.spinnerFullScrnPdf)
        //
        val icon1 = myFullScrAct.findViewById<ImageView>(R.id.imageViewDatum)
        icon1.setOnClickListener { v -> myDateSpinner.performClick() }
        val icon2 = myFullScrAct.findViewById<ImageView>(R.id.imageViewPdf)
        icon2.setOnClickListener { v -> myPageSpinner.performClick() }
        //
        populateTree()
        // Initialbefüllung des Datum Spinners - der ist mengentechnisch stabil
        // über die gesamte Lebenszeit der FullscreenView (sie wird jedes mal neu angelegt, wenn
        // zwischen den Activities gewechselt wird)
        //
        Log.d("DPN::ctor","Adapter for DateSpinner...") // der füllt die Daten in die UI
        var i = 0
        val aa : ArrayAdapter<String> = ArrayAdapter<String>(
            myFullScrAct.applicationContext, android.R.layout.simple_spinner_item)
        for( s in myStringTree ) {
            i += 1
            aa.add( s.first + " (${i.toString()}/${myStringTree.size})"
//                    + " - ${s.second.size} PDF(s)"
                    + " [${s.second.size}]"
            )
        }
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        if( aa.count > 0)
            myDateSpinner.setAdapter(aa)
        //
        //if (FullscreenActivity.fsa.myNavi == null) {
        toggleSpinnerListeners(true)
        //}
        Log.i("DPN::ctor","<-")
    }

    private fun populateTree() {
        Log.i("DPN::poTr","->")
        if( myRootDir.exists() && myRootDir.canRead() && myRootDir.list().size > 0 )
            for( f1 in myRootDir.listFiles().sorted() )
                if( f1.isDirectory() && f1.canRead() ) {
                    Log.d("DPN::poTr", "f1.name: " + f1.name + " initializing date collection..." )
                    val mlo = mutableListOf<File>()
                    myStringTree.add(Pair(f1.name, mlo))
                    //myTxtDat.setTag(0,f1.name)
                    //
                    Log.d("DPN::poTr", "f1.name: " + f1.name + " looping over contents..." )
                    for( f2 in f1.listFiles().sorted() )
                        if( f2.isFile() && f2.canRead() && f2.name.contains(
                                ".pdf",
                                ignoreCase = true
                            )
                        ) {
                            mlo.add(f2)
                            //myTxtPdf.setTag(0,f2)
                            Log.d("DPN::poTr", "f2.name: " + f2.name + " added" )
                        }
                }
        Log.i("DPN::poTr", "<- populated directories: " + myStringTree.size )
    }

    private fun toggleSpinnerListeners( bStartListening : Boolean ) {
        if( bStartListening ) {
            Log.d("DPN::toggleSL", "Register Listeners on DateSpinner...")
            myDateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val curIdxPosition = getCurDatePointerIndex()
                    Log.i(
                        "DPN:Date:onItemSelected",
                        "v: ${view.toString()}; stack-pos: ${position.toString()}" +
                                "cur-pos: ${curIdxPosition.toString()}"
                    )
                    if( position != curIdxPosition )
                        setCurPointer(position, 0)
                    // else // ignoring
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.i("DPN:Date:onNthngSel", " ")
                }
            }
            //
            Log.d("DPN::toggleSL", "Register Listeners on PageSpinner...")
            myPageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val curIdxPosition = getCurPagePointerIndex()
                    Log.i(
                        "DPN:Page:onItemSelected",
                        "v: ${view.toString()}; pos: ${position.toString()}" +
                                "cur-pos: ${curIdxPosition.toString()}"
                    )
                    if( position != curIdxPosition )
                        setCurPointer(myCurDatePointer, position)
                    // else // ignoring
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.i("DPN:Page:onNthngSel", " ")
                }
            }
        } else {
            Log.d("DPN::toggleSL", "Stop Listening on Spinners...")
            myDateSpinner.onItemSelectedListener = null
            myPageSpinner.onItemSelectedListener = null
        }
    }

    fun isLastPdfInFolderOrFolderAlreadyEmpty() : Boolean {
        var bRc : Boolean = myStringTree.get(myCurDatePointer).second.size == 1 ||
                            myStringTree.get(myCurDatePointer).second.size == 0
        return bRc
    }
    fun isLastFolderInRoot() : Boolean {
        var bRc : Boolean = myRootDir.list().size == 1
        return bRc
    }
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
        Log.i("DPN::deleteCurPdf", "success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }
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
        Log.i("DPN::delCurDateFldr", "success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }
    fun getCurRootFolder() : File {
        var fRc :File? = myRootDir
        return fRc!!
    }
    private fun getCurDatePointerIndex() :Int {return(myCurDatePointer)}
    private fun getCurPagePointerIndex() :Int {return(myCurPagePointer)}
    private fun setCurPointer( idxDate :Int, idxPage :Int ) {
        Log.i("DPN::setCurPointer", "-> myRootDir.readable: " + myRootDir.canRead().toString() +
            "; idxDate: ${idxDate.toString()}; idxPage: ${idxPage.toString()}")
        //
        var iDate = idxDate
        var iPage = idxPage
        var fn: String = "fnix"
        var dir: String = "dnix"
        try {
            // Rollover
            if( iDate < 0 ) iDate = myStringTree.lastIndex
            if( iDate > myStringTree.lastIndex ) iDate = 0
            dir = myStringTree.get(iDate).first
            //
            if( iPage < 0 ) iPage = myStringTree.get(iDate).second.lastIndex
            if( iPage > myStringTree.get(iDate).second.lastIndex ) iPage = 0
            Log.d( "DPN::setCurPointer", "after Rollover - " +
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
                myPdfView.loadPdfFromFile(f) // start loading pdf
                myPdfView.bringToFront()
                myTxtInf.visibility = View.INVISIBLE
            } else {
                myPdfView.recycle() // or reset to empty
                //
                myTxtInf.text = "Keine PDFs unter '" + myRootDirString + "/" + dir +
                     "' oder fehlende Berechtigungen."
                myTxtInf.visibility = View.VISIBLE
                myTxtInf.bringToFront()
            }
            //
            Log.d("DPM::setCurPointer","setting pointers, adjust spinners...")
            if( myCurDatePointer != iDate ) {
                Log.d("DPM::setCurPointer",
                    "date pointer changing from ${myCurDatePointer.toString()} to ${iDate.toString()}")
                myCurDatePointer = iDate
                myDateSpinner.setSelection(iDate)
                //
                // (re)fill Pages Spinner
                val aa : ArrayAdapter<String> = ArrayAdapter<String>(
                    myFullScrAct.applicationContext, android.R.layout.simple_spinner_item)
                val pagesList = myStringTree.get(iDate).second
                var i = 0
                for( f in pagesList ) {
                    i += 1
                    aa.add( f.name.removeSuffix(".pdf") + " (${i.toString()}/${pagesList.size})" )
                }
                aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                myPageSpinner.setAdapter(aa)
            }
            //
            Log.d("DPN::setCurPointer",
                "page pointer changing from ${myCurPagePointer.toString()} to ${iPage.toString()}")
            myCurPagePointer = iPage
            myPageSpinner.setSelection(iPage)
            //
        } catch (e: Exception) {
            Log.e("DPN::SMC", "EXC! " + e.message)
            //
            myTxtInf.text = "Interner Fehler."
            myTxtInf.visibility = View.VISIBLE
            myTxtInf.bringToFront()
        }
        //
        Log.i("DPN::setCurPointer", "<- now showing dir: " + dir
                + "; PDF: " + fn + "; within: "+myRootDir.absolutePath)
    }

    fun showMostCurrentPdf() {
        Log.i("DPNavi","showMostCurrentPdf; index: ${myStringTree.lastIndex}")
        setCurPointer(myStringTree.lastIndex,0)
    }
    fun showNextPdfAfterDelete(d: DeleteHandler) {
        Log.i("DPNavi","showNextPdfAfterDelete; index: ${myStringTree.lastIndex}")
        setCurPointer(d.myCurDatePointer,d.myCurPagePointer)
    }

    fun onClickDatumLinks()  {Log.i("DPNavi","DatumLinks"); setCurPointer(myCurDatePointer - 1, 0)}
    fun onClickDatumRechts() {Log.i("DPNavi","DatumRechts"); setCurPointer(myCurDatePointer + 1, 0)}
    fun onClickPdfLinks()    {Log.i("DPNavi","PdfLinks"); setCurPointer(myCurDatePointer,myCurPagePointer - 1)}
    fun onClickPdfRechts()   {Log.i("DPNavi","PdfRechts"); setCurPointer(myCurDatePointer,myCurPagePointer + 1)}
}