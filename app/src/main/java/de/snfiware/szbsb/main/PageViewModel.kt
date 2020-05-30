package de.snfiware.szbsb.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import de.snfiware.szbsb.util.AcmtLogger

class PageViewModel : ViewModel() {
    companion object {val CTAG = AcmtLogger("PVM")}
    //
    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: $it"
    }
    //
    fun setIndex(index: Int) {
        CTAG.log("pageViewModel::setIndex: " + index.toString())
        _index.value = index
    }
}