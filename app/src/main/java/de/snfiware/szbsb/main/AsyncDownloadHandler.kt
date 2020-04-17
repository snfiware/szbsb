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

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

import de.snfiware.szbsb.FullScreenForwarder
import de.snfiware.szbsb.MainActivity
import com.example.sztab.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

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
 */
class AsyncDownloadHandler : AsyncTask<View, String?, String?>() {
    //
    var pb: ProgressBar? = null
    var pyModule: PyObject? = null
    var pf: PyObject? = null
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
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(MainActivity.myMain!!.applicationContext))
        }
    }

    override fun onPreExecute() {
        initChaquo()
        super.onPreExecute()
        //
        val fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
        fab.isEnabled = false // Knopf während Download ausschalten
        //
        Log.d("ADH::DIB","getInst...")
        val pi = Python.getInstance()
        Log.d("ADH::DIB","getModule...")
        pyModule = pi.getModule("sz")
        //
        Log.d("ADH::DIB","get: executeScript...")
        pf = pyModule?.get("executeScript")
        //
        var sRc : String
        sRc = "Download läuft..."
        Log.d("ADH::OPRE",sRc)
        Snackbar.make(v, sRc, Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
        //
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.visibility = View.VISIBLE
        pb!!.bringToFront()
    }

    override fun doInBackground(vararg va: View?): String? {
        Log.i("ADH::DIB", "-> v: "+va.toString())
        myErr = E_OK
        //v = va.get(0) as View
        // Skript ausführen - wichtig: ohne .py
        // Python.getInstance().getModule("sz")
        var sRc :String = "PDF(s) wurden erfolgreich heruntergeladen"
        try {
            val strContext = MainActivity.nextCounter().toString()
            Log.d("ADH::DIB","CALLING: executeScript (${strContext})...")
            pf?.call(strContext)
            Log.d("ADH::DIB","RETURN: executeScript (${strContext})...")
        }
        catch (e: Exception) {
            sRc = e.message!! //printStackTrace().toString()
            Log.e("ADH::DIB", "UI-Msg: "+sRc)
            myErr = sRc // for the UI
            Log.i("ADH::DIB", "Stack: "+e.printStackTrace().toString() )
        }
        finally {
            Log.d("ADH::DIB", "UNUSED FINALLY?") // TODO
        }
        //
        Log.i("ADH::DIB", "<- sRc: "+sRc)
        return sRc
    }

    override fun onPostExecute(sRc: String?) {
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.visibility = View.INVISIBLE
        //
        val fab = MainActivity.myMain?.findViewById(R.id.fabDownload) as FloatingActionButton
        fab.isEnabled = true // Knopf nach Download wieder einschalten
        //
        if( myErr == E_OK )
            // Weiterleiten an Sekundärview
            FullScreenForwarder(MainActivity.myMain!!,fab).showFullScreen()
        else {
            // Fehlerausgabe
            Log.d("ADH::OPOST", myErr)
            Snackbar.make(v, myErr, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            //
        }
    }

    override fun onProgressUpdate(vararg values: String?) {
        pb = MainActivity.myMain?.findViewById(R.id.progressBar) as ProgressBar
        pb!!.progress = values[0]!!.toInt()
        super.onProgressUpdate(*values)
    }
}