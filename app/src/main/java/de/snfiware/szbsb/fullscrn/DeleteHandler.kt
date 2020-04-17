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
import android.util.Log
import android.view.MenuItem
import com.example.sztab.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.fullscrn.FullscreenActivity.Companion.fsa
import de.snfiware.szbsb.main.CfgSzHandler

class DeleteHandler() {
    private var bEmptyNow : Boolean = false
    fun isEmptyNow() : Boolean { return bEmptyNow }
    var myCurDatePointer :Int = -1
    var myCurPagePointer :Int = -1

    fun deletePdf() :Boolean {
        val navi = fsa.myNavi
        if( navi.isLastPdfInFolderOrFolderAlreadyEmpty() ) {
            Log.i("DH::deletePdf", "delegating to deleteFolder...")
            return( deleteFolder() )
        }
        //
        val bRc = navi.deleteCurPdf(this)
        return bRc
    }

    fun deleteFolder() :Boolean {
        val navi = fsa.myNavi
        if( navi.isLastFolderInRoot() ) {
            Log.i("DH::deleteFolder", "delegating to deleteAll...")
            return( deleteAll() )
        }
        val bRc = navi.deleteCurDateFolder(this)
        return bRc
    }

    fun deleteAll() :Boolean {
        val navi = fsa.myNavi
        val f = navi.getCurRootFolder()
        val bRc = f.deleteRecursively()
        bEmptyNow = bRc
        Log.i("DH::deleteAll", "success: " + bRc.toString() + "; file: " + f.absolutePath)
        return bRc
    }

    //////////////////////////////
    companion object {
        fun handleOnOptionsItemSelected(item: MenuItem): Boolean {
            var bRc = false
            val id = item.itemId
            //
            if (id == R.id.mi_delete_pdf ) {
                val d = DeleteHandler()
                d.deletePdf()
                if( d.isEmptyNow() )  {
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                else {
                    fsa.rebuildNavigator(d)
                }
                bRc = true

            } else if (id == R.id.mi_delete_folder) {
                val d = DeleteHandler()
                d.deleteFolder()
                if( d.isEmptyNow() )  {
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                else {
                    fsa.rebuildNavigator(d)
                }
                bRc = true

            } else if (id == R.id.mi_delete_all ) {
                //var bRc = true // DeleteHandler().deleteAll()
                val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(fsa)
                dlgAlert.setMessage("Ordner ${CfgSzHandler.getDownloadFolderFromUI()} mitsamt allen Inhalten komplett löschen?")
                dlgAlert.setTitle("Löschen")
                dlgAlert.setPositiveButton(
                    "Ok"
                ) { dialog, which ->
                    DeleteHandler().deleteAll()
                    MainActivity.myFsf.setIconFromState()
                    fsa.finish()
                }
                dlgAlert.setCancelable(true)
                dlgAlert.create().show()
                bRc = true
            }
            //
            return bRc
        }
    } // companion
}