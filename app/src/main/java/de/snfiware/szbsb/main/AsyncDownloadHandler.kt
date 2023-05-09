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

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.children
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import de.snfiware.szbsb.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.snfiware.szbsb.FullScreenForwarder
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.util.AcmtLogger
import de.snfiware.szbsb.util.LogHelper
import java.io.File


/**
 * Hier wird Chaquopy initialisiert, das Python-Skript für den Download geladen
 * und der eigentliche Download im Hintergrund angestartet während im Vordergrund
 * eine einfache Fortschrittsanzeige zum Warten anhält.
 *
 * Das Python-Skript führt den eigentlichen Download durch und ist auch ohne die SzBsb-App
 * ablauffähig. Die Kommunikation zum Skript läuft über die Konfigurationsdatei szconfig.properties.
 * Die Kommunikation vom Skript zurück in die App läuft über das Dateisystem (downloadFolder).
 *
 * https://developer.android.com/reference/android/os/AsyncTask
 * https://stackoverflow.com/questions/3391272/how-to-use-separate-thread-to-perform-http-requests
 * https://stackoverflow.com/questions/9671546/asynctask-android-example (picture in the middle)
 */
class AsyncDownloadHandler : AsyncTask<String, String, String>() {
    companion object {
        val CTAG = AcmtLogger("ADH",bSeparateStack = true)
        val CTAGPY = AcmtLogger("APY",bSeparateStack = true)
        val CTAGUI = AcmtLogger("AUI")
        //
        val NO_DOWNLOAD_YET = ""
        var sFirstDownloadedFile: String = NO_DOWNLOAD_YET // the one to focus
    }
    //
    var pb: ProgressBar? = null
    var pyFuncExecuteScript: PyObject? = null
    var sAreaToLoad: String? = null
    //
    val E_OK = "OK"
    var myErr :String = E_OK
    //
    lateinit var v :View
    fun setView (v:View) {
        CTAG.d_("SetView ${v}; old: ${when(this::v.isInitialized) {true -> this.v.toString(); else -> "n/a"}}")
        this.v=v
    }

    fun setEnabledStatusOfControlsDuringDownload( bEnabled :Boolean) {
        // Floating-Knöpfe
        var fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
        fab.isEnabled = bEnabled
        //
        fab = MainActivity.myMain?.findViewById(R.id.fabFullscreen) as FloatingActionButton
        fab.isEnabled = bEnabled
        //
        // Radiobuttons
        var rg = MainActivity.myMain?.findViewById(R.id.rgBereich) as RadioGroup
        rg.children.forEach { it.isEnabled = bEnabled }
    }

    override fun onPreExecute() {
        CTAGUI.enter("onPreExecute", "switch off buttons and show progress bar")
        super.onPreExecute()
        //
        setEnabledStatusOfControlsDuringDownload(false)
        //
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.keepScreenOn = true
        pb!!.visibility = View.VISIBLE
        pb!!.bringToFront()
        //
        // have to be stored in member variable thus the background task has no access to UI
        sAreaToLoad = MainActivity.getCheckedRadioButtonCaption()
        //
        CTAGUI.leave()
    }

    private fun initChaquo() {
        CTAG.enter("initChaquo")
        if (!Python.isStarted()) {
            CTAG.i("Starting Chaquo...")
            Python.start(AndroidPlatform(MainActivity.myMain!!.applicationContext))
        }
        // this seems to be neccessary each time before using executeScript...
        CTAG.d("getInst...")
        val pi = Python.getInstance()
        CTAG.d("getModule...")
        val pyModule = pi.getModule("sz")
        //
        CTAG.d("get: executeScript...")
        pyFuncExecuteScript = pyModule?.get("executeScript")
        //
        CTAG.leave()
    }

    override fun doInBackground(vararg va: String): String? {
        CTAG.enter("doInBackground","va: "+va.toString())
        myErr = E_OK
        //v = va.get(0) as View
        publishProgress( "Initialisierung..." )
        // Skriptausführung via Chaquopy vorbereiten
        initChaquo()
        assert( pyFuncExecuteScript != null ) // "initChaquopy failed"
        //
        publishProgress( "Starte Skript..." )
        //
        var sRc :String = "PDF(s) wurden erfolgreich heruntergeladen"
        try {
            val strContext = MainActivity.nextCounter().toString()
            //
            CTAG.i("CALLING: executeScript (${strContext}; ${sAreaToLoad})...")
            pyFuncExecuteScript?.call(this, strContext, sAreaToLoad)
            CTAG.i("RETURN: executeScript (${strContext}; ${sAreaToLoad}).")
        }
        catch (e: Exception) {
            sRc = e.message!! //printStackTrace().toString()
            CTAG.e( "UI-Msg: "+sRc)
            myErr = sRc // for the UI
            CTAG.i( "Stack: "+e.printStackTrace().toString() )
        }
        //
        CTAG.leave(sRc)
        return sRc
    }

    // call from python
    fun logFromPythonToAndroid(s :String, loglevel :Int) {
        CTAGPY.log_(s,loglevel)
    }

    // call from python
    fun getFolderToPutSzLogFileToFromPython(): String {
        CTAGPY.enter("getLogFolderPy" )
        val sRc = MainActivity.myMain?.externalCacheDir.toString() + "/"
        CTAGPY.leavi("rc: ${sRc}")
        return sRc
    }

    // call from python
    fun showSnackMsgFromPythonViaPublishProgress(s :String)
    {
        CTAGPY.enti("showSnackFromPy","publishProgressFromPython: ${s}")
        publishProgress(s)
        CTAGPY.leave("publishProgressFromPython: ${s}")
    }

    // call from python
    fun publishFileFromPythonToAndroid(filepath :String, isFirst :Boolean, isLast :Boolean)
    {
        val file = File(filepath)
        val mimeType = "application/pdf"
        val s = file.name
        CTAGPY.enter("pubFileFromPy","isFirst: ${isFirst} isLast: ${isLast} ${s}")
        //
        if( false ) {
            val dm = MainActivity.myMain?.baseContext!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            //
            CTAGPY.d("addCompletedDownload...")
            dm.addCompletedDownload(
                file.parentFile.parentFile.name + "/" + file.parentFile.name + "/" + s,
                filepath,
                true,
                mimeType,
                file.getAbsolutePath(),
                file.length(),
                false
            )
        }
        // memorize the first downloaded file to navigate to after everything is completed
        if(isFirst){
            sFirstDownloadedFile = filepath
        }
        // make available the folder to system index
        if(isLast){ // https://stackoverflow.com/questions/32789157/how-to-write-files-to-external-public-storage-in-android-so-that-they-are-visibl
            CTAGPY.d("start MediaScanner.scanFile...")
            //val ctxt = MainActivity.myMain?.baseContext!!
            //MediaScannerConnection.scanFile(ctxt, arrayOf(file.getAbsolutePath()), arrayOf(mimeType), null)
            MediaScannerConnection.scanFile(MainActivity.myMain, arrayOf(file.getAbsolutePath()), arrayOf(mimeType), null)
        }
        CTAGPY.leave("${s}")
    }

    // called in UI thread when publishProgress is invoked by background task
    override fun onProgressUpdate(vararg values: String) {
        CTAGUI.enter("onProgrsUpd","count: ${values.size.toString()}")
        //pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        //pb!!.progress = values[0]!!.toInt()
        //
        val sRc : String = values[0]
        CTAGUI.i("show snack '${sRc}'")
        Snackbar.make(v, sRc, Snackbar.LENGTH_INDEFINITE)
            .setAction("Action", null).show()
        //
        super.onProgressUpdate(*values)
        CTAGUI.leave()
    }

    override fun onPostExecute(sRc: String) {
        CTAGUI.enter("onPostExecute", "sRc: "+sRc+"; myErr: "+myErr)
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.keepScreenOn = false
        pb!!.visibility = View.INVISIBLE
        //
        setEnabledStatusOfControlsDuringDownload(true)
        //
        if( myErr == E_OK ) {
            // Dieser 1ms lange Snack überschreibt die unendlich lang angezeigten Vorgänger
            Snackbar.make(v, "Fertig", 1).setAction("Action", null).show()
            //
            // Weiterleiten an Sekundärview
            var fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
            FullScreenForwarder(MainActivity.myMain!!, fab).showFullScreen()
        }
        else {
            // Fehlerausgabe
            CTAGUI.w(myErr)
            val snackbar = Snackbar.make(v, myErr, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            val textView =
                snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 3 // show multiple line
            snackbar.setAction("Action", null).show()
            //
        }
        CTAGUI.leave()
    }
}