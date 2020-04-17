package de.snfiware.szbsb.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.sztab.R
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.MainActivity.Companion.myFsf
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getFragmentByPosition
import de.snfiware.szbsb.main.SectionsPagerAdapter.Companion.getMaxTabs

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        System.out.println("placeholderFrag::onCreate (ohne view) with bundle: " + savedInstanceState.toString())
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("PHF->onCreateView","with bundle: " + savedInstanceState.toString() +
                " viewGroup: " + container.toString())
        var v:View?
        val asn:Int = arguments!!.getInt(ARG_SECTION_NUMBER)
        if (asn == 0)
        {
            v = inflater.inflate(R.layout.fragment_settings, container, false)
        }
        else if (asn == 1)
        {
            v = inflater.inflate( R.layout.fragment_topics, container, false )
            CfgSzHandler.file2dlgCreation(
                v,
                asn
            )
            Chip0Handler.registerListenerForAllChips(
                v,
                R.id.chipT0
            )
        }
        else if (asn == 2)
        {
            v = inflater.inflate(R.layout.fragment_pages, container, false)
            CfgSzHandler.file2dlgCreation(
                v,
                asn
            )
            Chip0Handler.registerListenerForAllChips(
                v,
                R.id.chipP0
            )
        }
        else {
            throw Exception("unknown tab")
        }
        Log.i("PHF<-onCreateView", "pos: "+asn+" returning v: "+v.toString())
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i("PHF::onViewCreated", "-> v: "+view.toString() )
        super.onViewCreated(view, savedInstanceState)
        val frag = getFragmentByPosition( getMaxTabs()-1 )
        if( frag != null && view == frag.view ) {
            Log.i("PHF::onViewCreated", "--> file2dlgSelection" )
            CfgSzHandler.file2dlgSelection()
            MainActivity!!.myFsf.setIconFromState()
            //
            val textView = MainActivity.myMain!!.findViewById(R.id.editTextFolder) as TextView
            textView.doAfterTextChanged {
                    _ -> Log.e("tv.oatc","sifs ->")
                myFsf.setIconFromState()
                Log.e("tv.oatc","sifs <-")
            }
        }
        Log.i("PHF::onViewCreated", "<-" )
    }

    /*override fun onViewStateRestored(savedInstanceState: Bundle?) {
    }*/

    companion object {
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
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}