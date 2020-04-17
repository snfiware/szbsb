package de.snfiware.szbsb.util

fun assert( b:Boolean, s:String ="" ) {
    if( !b )
        throw IllegalStateException("ASSERTION FAILED: " + s)
}