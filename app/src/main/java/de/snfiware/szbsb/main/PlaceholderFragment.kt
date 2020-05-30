package de.snfiware.szbsb.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import de.snfiware.szbsb.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.MainActivity.Companion.myFsf
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getFragmentByPosition
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getMaxTabs
import de.snfiware.szbsb.util.AcmtLogger

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {
    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        CTAG.log("onCreate (ohne view) with bundle: " + savedInstanceState.toString())
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        CTAG.enter("onCreateView","with bundle: " + savedInstanceState.toString() +
                " viewGroup: " + container.toString())
        var v:View?
        val asn:Int = arguments!!.getInt(ARG_SECTION_NUMBER)
        CTAG.log("asn: $asn")
        if (asn == 0)
        {
            CTAG.log("inflate...")
            v = inflater.inflate(R.layout.fragment_settings, container, false)
            CTAG.log("inflated.")
        }
        else if (asn == 1)
        {
            CTAG.log("inflate...")
            v = inflater.inflate( R.layout.fragment_topics, container, false )
            CTAG.log("inflated v: " + v.toString())
            CfgSzHandler.file2dlgCreation(
                v,
                asn
            )
            CTAG.log("register...")
            Chip0Handler.registerListenerForAllChips(
                v,
                R.id.chipT0
            )
        }
        else if (asn == 2)
        {
            CTAG.log("inflate...")
            v = inflater.inflate(R.layout.fragment_pages, container, false)
            CTAG.log("inflated v: " + v.toString())
            CfgSzHandler.file2dlgCreation(
                v,
                asn
            )
            CTAG.log("register...")
            Chip0Handler.registerListenerForAllChips(
                v,
                R.id.chipP0
            )
        }
        else {
            throw Exception("unknown tab")
        }
        CTAG.leave("pos: "+asn+" returning v: "+v.toString())
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        CTAG.enter("onViewCreated", "v: "+view.toString() )
        super.onViewCreated(view, savedInstanceState)
        val frag = getFragmentByPosition( getMaxTabs()-1 )
        if( frag != null && view == frag.view ) {
            CTAG.log( "--> file2dlgSelection" )
            CfgSzHandler.file2dlgSelection()
            //
            CTAG.log( "--> setIconFromState" )
            MainActivity.myFsf.setIconFromState()
            //
            val textView = MainActivity.myMain!!.findViewById(R.id.editTextFolder) as TextView
            CTAG.log( "register doAfterTextChanged on ${textView}" )
            textView.doAfterTextChanged {
                    _ -> CTAG.enter("doAfterTxtChgd")
                            myFsf.setIconFromState()
                            CTAG.leave()
            }
        }
        CTAG.leave()
    }

    /*override fun onViewStateRestored(savedInstanceState: Bundle?) {
    }*/

    companion object {
        val CTAG = AcmtLogger("PHF")
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            CTAG.log("newInstance: $sectionNumber")
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}