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

import android.content.Context
import android.util.AttributeSet
import com.github.barteksc.pdfviewer.PDFView
import de.snfiware.szbsb.util.AcmtLogger
import java.io.File


class MyPdfView(context: Context?, set: AttributeSet?) : PDFView(context, set) {
    val CTAG = AcmtLogger("MPV")

    fun loadPdfFromFile( f :File ) {
        CTAG.enter("loadPdfFromFile","${f.name}")
        /////////////////////////////
        // get configurator
        val c = super.fromFile(f)
        // switch menu
//        c.onLongPress { e ->
        CTAG.log("register onTap and configure...")
        c.onTap { _ ->
            CTAG.log("onTap/onLongPress - toggle...")
            FullscreenActivity.fsa.toggle()
            false
        }
        c.enableDoubletap(true)

        ///////////////////////////////
        // show pdf - start loading...
        CTAG.log("start loading...")
        c.load()
        CTAG.leave()
    }

}