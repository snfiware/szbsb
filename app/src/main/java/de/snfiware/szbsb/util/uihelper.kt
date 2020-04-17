package de.snfiware.szbsb.util

import android.util.TypedValue
import android.content.Context

fun convertDpToPxFloat( c :Context, dp :Float ) : Float {
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.resources.displayMetrics)
    return px
}

fun convertDpToPx( c :Context, dp :Int ) : Int {
    val px = convertDpToPxFloat(c, dp.toFloat())
    return px.toInt()
}

