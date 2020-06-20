package de.snfiware.szbsb.main

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import de.snfiware.szbsb.R
import de.snfiware.szbsb.util.assert
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.snfiware.szbsb.util.AcmtLogger


/**PlaceholderFragment.newInstance(position + 1000)
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        var f = PlaceholderFragment.newInstance(
            position
        )
        CTAG.d("getItem - writing pos: ${position}; new: ${f}; old: ${FRAGS[position]}." )
        FRAGS[position] = f
        return f
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return getMaxTabs()
    }

    companion object {
        val CTAG = AcmtLogger("SPA")
        private val TAB_TITLES = arrayOf(
            R.string.tab_text_1,
            R.string.tab_text_2,
            R.string.tab_text_3
        )
        // Verwaltung um keine Fragments doppelt zu erzeugen - bin mir nicht sicher, ob das
        // nicht das Framework übernehmen sollte...
        private val FRAGS = mutableMapOf<Int, PlaceholderFragment>()
        //
        fun getMaxTabs(): Int {
            return TAB_TITLES.size
        }

        fun getFragmentByPosition( position :Int ) : PlaceholderFragment? {
            return FRAGS[position]
        }

        fun getViewByPosition( position :Int ) : View {
            var v:View?
            val phf =
                getFragmentByPosition(
                    position
                )
            assert(
                phf != null,
                "phf at position " + position + " must exist"
            )
            //
            v = phf?.requireView()
            assert(
                v != null,
                "view at position " + position + " must exist"
            )
            return v!!
        }

        fun getChipGroupByResId( resId :Int ) : ChipGroup {
            var position: Int
            if(      resId == R.id.cgTopics ) position = 1
            else if( resId == R.id.cgPages  ) position = 2
            else throw UnknownError("Es sind nur zwei Chip Groups bekannt")
            //
            val v =
                getViewByPosition(
                    position
                )
            return v.findViewById(resId) as ChipGroup
        }

        fun getChipByIdx( chipGroup :ChipGroup, idx :Int ) : Chip {
            val v = chipGroup[idx]
            return v as Chip
        }

        fun getTextViewByResId( resId :Int ) : TextView {
            val v =
                getViewByPosition(
                    0
                )
            return v.findViewById(resId) as TextView
        }
    }
}