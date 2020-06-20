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

import android.app.AlertDialog
import android.view.MenuItem
import de.snfiware.szbsb.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.fullscrn.FullscreenActivity.Companion.fsa
import de.snfiware.szbsb.main.CfgSzHandler
import de.snfiware.szbsb.util.AcmtLogger
import java.text.SimpleDateFormat
import java.util.*

typealias tStringList = List<String>
//typealias tStringMutableList = MutableList<String>

class DeleteHandler() {
    private var bEmptyNow : Boolean = false
    fun isEmptyNow() : Boolean { return bEmptyNow }
    var myCurDatePointer :Int = -1
    var myCurPagePointer :Int = -1
    //
    fun deletePdf() :Boolean {
        val navi = fsa.myNavi
        if( navi.isLastPdfInFolderOrFolderAlreadyEmpty() ) {
            CTAG.log("deletePdf: delegating to deleteFolder...")
            return( deleteFolder() )
        }
        //
        val bRc = navi.deleteCurPdf(this)
        return bRc
    }
    //
    fun deleteFolder() :Boolean {
        val navi = fsa.myNavi
        if( navi.isLastFolderInRoot() ) {
            CTAG.log("deleteFolder: delegating to deleteAll...")
            return( deleteAll() )
        }
        val bRc = navi.deleteCurDateFolder(this)
        return bRc
    }
    //
    fun deleteAll() :Boolean {
        val navi = fsa.myNavi
        val f = navi.getCurRootFolder()
        val bRc = f.deleteRecursively()
        bEmptyNow = bRc
        CTAG.log("deleteAll success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }
    //
    fun deleteList(list :tStringList) :Boolean {
        val navi = fsa.myNavi
        val bRc = navi.deleteFolders(this,list)
        if( navi.isRootEmpty() ) {
            bEmptyNow = true
        }
        return bRc
    }
    //
    //////////////////////////////
    companion object {
        val CTAG = AcmtLogger("DH")
        //
        fun showInfoNothingToDo(item: MenuItem) {
            FullscreenActivity.showSnack("Nichts zu löschen...")
        }
        //
        fun handleOnOptionsItemSelected(item: MenuItem): Boolean {
            CTAG.enter("handleItemSel")
            var bRc = false
            val id = item.itemId
            //
            if( !fsa.myNavi.myRootDir.canRead() && fsa.myNavi.myRootDir.list().size > 0 ) {
                FullscreenActivity.showSnack("Keine Berechtigungen.")
            }
            else if( fsa.myNavi.myRootDir.list().size == 0 ) {
                FullscreenActivity.showSnack("Order leer.")
            }
            else if (id == R.id.mi_delete_pdf ) {
                CTAG.d("deletePdf")
                val d = DeleteHandler()
                d.deletePdf()
                if( d.isEmptyNow() )  {
                    CTAG.d("Navigate back to main after deletePdf...")
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                else {
                    fsa.rebuildNavigator(d)
                }
                bRc = true
                //
            } else if (id == R.id.mi_delete_folder) {
                CTAG.d("deleteFolder")
                val d = DeleteHandler()
                d.deleteFolder()
                if( d.isEmptyNow() )  {
                    CTAG.d("Navigate back to main after deleteFolder...")
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                else {
                    fsa.rebuildNavigator(d)
                }
                bRc = true
                //
            } else if (id == R.id.mi_delete_all ) {
                CTAG.d("deleteAll")
                //var bRc = true // DeleteHandler().deleteAll()
                val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(fsa)
                dlgAlert.setMessage("Ordner ${CfgSzHandler.getDownloadFolderFromUI()} mitsamt allen Inhalten komplett löschen?")
                dlgAlert.setTitle("Löschen")
                dlgAlert.setPositiveButton(
                    "Ok"
                ) { _,_ -> //dialog, which ->
                    CTAG.d("Dialog Ok was clicked...")
                    DeleteHandler().deleteAll()
                    CTAG.d("Navigate back to main after deleteAll...")
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                dlgAlert.setCancelable(true)
                CTAG.d("show dialog...")
                dlgAlert.create().show()
                bRc = true
                //
            } else if (id == R.id.mi_delete_older_90){
                if(fsa.myNavi.list01.isNotEmpty()) {
                    CTAG.d("delete_older_90")
                    val d = DeleteHandler()
                    bRc = d.deleteList(fsa.myNavi.list01)
                    //
                    if( d.isEmptyNow() )  {
                        CTAG.d("Navigate back to main after deleteFolder...")
                        MainActivity.myFsf.setIconFromState()
                        fsa.finish()
                    }
                    else {
                        fsa.rebuildNavigator(d)
                    }
                } else { showInfoNothingToDo(item) }
                //
            } else if (id == R.id.mi_delete_older_180){
                if(fsa.myNavi.list02.isNotEmpty()) {
                    CTAG.d("delete_older_180")
                    val d = DeleteHandler()
                    bRc = d.deleteList(fsa.myNavi.list02)
                    //
                    if (d.isEmptyNow()) {
                        CTAG.d("Navigate back to main after deleteFolder...")
                        MainActivity.myFsf.setIconFromState()
                        fsa.finish()
                    } else {
                        fsa.rebuildNavigator(d)
                    }
                } else { showInfoNothingToDo(item) }
            }
            //
            CTAG.leave("bRc: $bRc")
            return bRc
        }
        //
        fun checkAndPossiblyShowDeleteWarning(fsa :FullscreenActivity) {
            CTAG.enter("overdueCheck", "list01: ${fsa.myNavi.list01}; list02: ${fsa.myNavi.list02}")
            //
            val cnt = fsa.myNavi.list02.size
            if(cnt > 0) {
                val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(fsa)
                dlgAlert.setMessage("Sie haben ${cnt} Ordner älter als 180 Tage. Jetzt löschen?")
                dlgAlert.setTitle("BSB-Löschverpflichtung")
                dlgAlert.setPositiveButton(
                    "Ok"
                ) { _,_ -> //dialog, which ->
                    CTAG.d("Dialog Ok was clicked...")
                    fsa.myMenu.performIdentifierAction(R.id.mi_delete_older_180, 0);
                }
                dlgAlert.setCancelable(true)
                CTAG.d("show dialog...")
                dlgAlert.create().show()
            }
            CTAG.leave()
        }
        //
        fun getListOfOverduePdfFolders( st :tStringTree, pattern :String, nbrDaysValidFromNow :Int ) : tStringList {
            CTAG.enter("getOverduePdf","days:$nbrDaysValidFromNow")
            val locale = Locale.GERMANY
            val calendar = Calendar.getInstance(locale)
            val today = calendar
                today.setTime(Date())
            val oldestValidDate = today.clone() as Calendar
                oldestValidDate.add(Calendar.DATE,0-nbrDaysValidFromNow)
            //
            val format = SimpleDateFormat(pattern, locale)
            val oldestValidDateAsFormattedString = format.format(oldestValidDate.time)
            //
            val listRc = st.map{it.first}.filter{ T -> T < oldestValidDateAsFormattedString }
            CTAG.leave("oldestValid: $oldestValidDateAsFormattedString -> $listRc")
            return(listRc)
        }
    } // end-companion
} // end-class DeleteHandler
