package de.snfiware.szbsb.fullscrn

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import de.snfiware.szbsb.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.util.AcmtLogger
import de.snfiware.szbsb.util.ConfiguredLogHelper
import kotlinx.android.synthetic.main.activity_help.*
import java.io.ByteArrayOutputStream


class HelpActivity : AppCompatActivity() {
    val CTAG = AcmtLogger("Help")

    override fun onCreate(savedInstanceState: Bundle?) {
        CTAG.enter("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //
        val wv = findViewById(R.id.webViewHelp) as WebView
        //wv.loadUrl("file:///snfiware_sz.png")
        //wv.loadData(readTextFromResource(R.raw.helptext), "text/html", "utf-8")
        // Mit loadDataWithBaseURL kann man im html, z.B. im <img> Tag die Dateien direkt aus dem Ordner
        // ~/dev/sz/app/src/main/assets benutzen. Achtung: assets Ordner Plural - baseUrl Singular (!)
        // Um den assets Ordner anzulegen: Rechtsklick im Baum auf main, new / folder / assets folder
        CTAG.log("WebView::loadDataWithBaseURL...")
        wv.loadDataWithBaseURL("file:///android_asset/", readTextFromResource(R.raw.helptext)
            , "text/html", "utf-8", null )
        CTAG.leavi("now showing Help Fullscreen-View")
    }

    fun readTextFromResource( res : Int ) : String {
        CTAG.enter("readTextRes","res: $res")
        var sRc : String
        var str = ByteArrayOutputStream()
        MainActivity.myMain?.resources?.openRawResource(res).use {
            it?.copyTo(str)
        }
        sRc = str.toString()
        CTAG.leave("read ${sRc.length} chars.")
        return( sRc )
    }
    //
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        val loghelper = ConfiguredLogHelper(this, this.helpActFooterHook, this.webViewHelp)
        loghelper.showPopupMenu(this)
        return super.onKeyLongPress(keyCode, event)
    }
}
