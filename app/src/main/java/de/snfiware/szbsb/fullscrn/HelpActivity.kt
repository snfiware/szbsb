package de.snfiware.szbsb.fullscrn

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.example.sztab.R
import de.snfiware.szbsb.MainActivity

import java.io.ByteArrayOutputStream


class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val wv = findViewById(R.id.webViewHelp) as WebView
        //wv.loadUrl("file:///snfiware_sz.png")
        //wv.loadData(readTextFromResource(R.raw.helptext), "text/html", "utf-8")

        // Mit loadDataWithBaseURL kann man im html, z.B. im <img> Tag die Dateien direkt aus dem Ordner
        // ~/dev/sz/app/src/main/assets benutzen. Achtung: assets Ordner Plural - baseUrl Singular (!)
        // Um den assets Ordner anzulegen: Rechtsklick im Baum auf main, new / folder / assets folder
        wv.loadDataWithBaseURL("file:///android_asset/", readTextFromResource(R.raw.helptext)
            , "text/html", "utf-8", null )
    }

    fun readTextFromResource( res : Int ) : String {
        var sRc : String
        var str = ByteArrayOutputStream()
        MainActivity.myMain?.resources?.openRawResource(res).use {
            it?.copyTo(str)
        }
        sRc = str.toString()
        return( sRc )
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        Log.d("HA::onOptItemSel", "id ${id.toString()} == ${android.R.id.home.toString()}")
//        if (id == android.R.id.home) {
//            // This ID represents the Home or Up button.
//            NavUtils.navigateUpFromSameTask(this)
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }
}
