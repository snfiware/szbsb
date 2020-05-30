package de.snfiware.szbsb.util

import android.util.Log

fun assert( b:Boolean, s:String ="" ) {
    if( !b ) {
        val sLog = "ASSERTION FAILED: " + s
        Log.e("ASSERT", sLog)
        throw AssertionError(sLog)
    }
}