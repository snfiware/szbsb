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

import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.sztab.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.snfiware.szbsb.FullScreenForwarder
import de.snfiware.szbsb.MainActivity


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
    //
    var pb: ProgressBar? = null
    // var pyModule: PyObject? = null
    var pyFuncExecuteScript: PyObject? = null
    //
    val E_OK = "OK"
    var myErr :String = E_OK
    //
    lateinit var v :View
    fun setView (v:View) {
        Log.d("ADH::SetView","view: "+v.toString())
        this.v=v
    }

    private fun initChaquo() {
        Log.i("ADH::INICHAQ","->")
        if (!Python.isStarted()) {
            Log.i("ADH::INICHAQ","Starting Chaquo...")
            Python.start(AndroidPlatform(MainActivity.myMain!!.applicationContext))
        }
        // this seems to be neccessary each time before using executeScript...
        Log.d("ADH::OPRE","getInst...")
        val pi = Python.getInstance()
        Log.d("ADH::OPRE","getModule...")
        val pyModule = pi.getModule("sz")
        //
        Log.d("ADH::OPRE","get: executeScript...")
        pyFuncExecuteScript = pyModule?.get("executeScript")
        //
        Log.i("ADH::INICHAQ","<-")
    }

    override fun onPreExecute() {
        Log.i("ADH::OPRE","->")
        super.onPreExecute()
        //
        var fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
        fab.isEnabled = false // Knopf während Download ausschalten
        //
        fab = MainActivity.myMain?.findViewById(R.id.fabFullscreen) as FloatingActionButton
        fab.isEnabled = false // Knopf während Download ausschalten
        //
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.visibility = View.VISIBLE
        pb!!.bringToFront()
        Log.i("ADH::OPRE","<-")
    }

    override fun doInBackground(vararg va: String): String? {
        Log.i("ADH::DIB", "-> va: "+va.toString())
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
            Log.d("ADH::DIB","CALLING: executeScript (${strContext})...")
            pyFuncExecuteScript?.call(this, strContext)
            Log.d("ADH::DIB","RETURN: executeScript (${strContext})...")
        }
        catch (e: Exception) {
            sRc = e.message!! //printStackTrace().toString()
            Log.e("ADH::DIB", "UI-Msg: "+sRc)
            myErr = sRc // for the UI
            Log.i("ADH::DIB", "Stack: "+e.printStackTrace().toString() )
        }
        //
        Log.i("ADH::DIB", "<- sRc: "+sRc)
        return sRc
    }

    override fun onPostExecute(sRc: String) {
        Log.i("ADH::OPOST", "-> sRc: "+sRc+"; myErr: "+myErr)
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.visibility = View.INVISIBLE
        //
        var fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
        fab.isEnabled = true // Knopf nach Download wieder einschalten
        //
        fab = MainActivity.myMain?.findViewById(R.id.fabFullscreen) as FloatingActionButton
        fab.isEnabled = true // Knopf nach Download wieder einschalten
        //
        if( myErr == E_OK ) {
            Snackbar.make(v, "Fertig", Snackbar.LENGTH_SHORT).setAction("Action", null).show()
            // Weiterleiten an Sekundärview
            FullScreenForwarder(MainActivity.myMain!!, fab).showFullScreen()
        }
        else {
            // Fehlerausgabe
            Log.w("ADH::OPOST", myErr)
            val snackbar = Snackbar.make(v, myErr, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            val textView =
                snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 3 // show multiple line
            snackbar.setAction("Action", null).show()
            //
        }
        Log.i("ADH::OPOST", "<-")
    }

    // call from python
    fun showSnackMsgFromPythonViaPublishProgress(s :String)
    {
        Log.i("ADH::SHOWMSG","-> publishProgressFromPython: ${s}")
        publishProgress(s)
        Log.i("ADH::SHOWMSG","<- publishProgressFromPython: ${s}")
    }

    override fun onProgressUpdate(vararg values: String) {
        Log.i("ADH::OPRGUPD","count: ${values.size.toString()}")
        //pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        //pb!!.progress = values[0]!!.toInt()
        //
        val sRc : String = values[0]
        Log.i("ADH::OPRGUPD","show snack '${sRc}'")
        Snackbar.make(v, sRc, Snackbar.LENGTH_INDEFINITE)
            .setAction("Action", null).show()
        //
        super.onProgressUpdate(*values)
    }
}