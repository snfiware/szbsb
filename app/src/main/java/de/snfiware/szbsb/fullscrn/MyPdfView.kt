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

import android.content.Context
import android.util.AttributeSet
import com.github.barteksc.pdfviewer.PDFView
import de.snfiware.szbsb.util.AcmtLogger
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.io.File


class MyPdfView(context: Context?, set: AttributeSet?) : PDFView(context, set) {
    companion object {
        val CTAG = AcmtLogger("MPV")

        fun setZoom(fsa: FullscreenActivity, bDefault: Boolean = true) {
            if(bDefault) {
                fsa.fullscreen_content.setMaxZoom(5.9f)
                fsa.fullscreen_content.setMidZoom(2.99f)
                fsa.fullscreen_content.setMinZoom(1.0f)
            }
            else {
                fsa.fullscreen_content.setMaxZoom(20.0f)
                fsa.fullscreen_content.setMidZoom(5.9f)
                fsa.fullscreen_content.setMinZoom(1.0f)
            }
        }
    }

    fun loadPdfFromFile( f :File ) {
        CTAG.enter("loadPdfFromFile","${f.name}")
        /////////////////////////////
        // get configurator
        val c = super.fromFile(f)
        // switch menu
        CTAG.d("register onTap and configure...")
        c.onTap { _ ->
            CTAG.d("onTap/onLongPress - toggle...")
            FullscreenActivity.fsa.toggle()
            false
        }
        c.enableDoubletap(true)

        ///////////////////////////////
        // show pdf - start loading...
        CTAG.i("start loading ${f.name} from ${f.parentFile.name}...")
        c.load()
        CTAG.leave()
    }

    override fun isZooming() : Boolean {
        val D = 0.2f
        return !(zoom > minZoom-D && zoom < minZoom+D)
    }

}