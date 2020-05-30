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
package de.snfiware.szbsb.main

import android.app.AlertDialog
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.snfiware.szbsb.MainActivity
import de.snfiware.szbsb.fullscrn.DeleteHandler
import de.snfiware.szbsb.util.AcmtLogger

/**
 * Der Chip0Handler implementiert die Logik für die gesamte Chip-Gruppe, um den "Alle"-Knopf (C0) und
 * die Einzel-Knöpfe (Cn) stimmig zu halten.
 *
 * Die gewünschte Logik ist:
 * - C0 folgt dem Status der Einzelknöpfe, ist aber selbst auch Auslöser für eine Sammeloperation
 *
 * Dies zerfällt in (<button><current-state><future-state/click>:<reaction>):
 * - C0-+: for-all: Cn+
 * - C0+-: for-all: Cn-
 * - Cn-+(all-other+already): C0+
 * - Cn+-: C0 -
 *
 * Praxis: Companion-Objekt mit registerListenerForAllChips() aufrufen.
 * Darin wird diese Klasse für jeden einzelnen Chip innerhalb der Gruppe
 * instanziiert, registriert und übernimmt damit die Behandlung, wenn er seinen Checked-Status ändert.
 * */
class Chip0Handler : CompoundButton.OnCheckedChangeListener {

    private var myChip: Chip? = null
    private var myChip0: Chip? = null
    private var myChipGroup: ChipGroup? = null

    constructor(c: Chip, c0: Chip, cg: ChipGroup) {
        myChip = c
        myChip0 = c0
        myChipGroup = cg
    }
    //
    override fun onCheckedChanged(v: CompoundButton, isChecked: Boolean) {
        // protect flow from recursively handling event
        if(bCheckChangeIsProgrammatic)
            return

        CTAG.enter("onCheckedChanged","view-cb: " + v.text
                + " was changed (by-user) to: " + isChecked.toString() )
        if( v.id == myChip0!!.id ) {
            // Es handelt sich um den Alles-Knopf (c0) der gerade seinen Status ändern
            if( isChecked ) {
                val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(MainActivity.myMain)
                dlgAlert.setMessage("Automatisierte Downloads widersprechen den BSB-Lizenzbedingungen.")
                dlgAlert.setTitle("BSB-Lizenzbedingungen beachten")
                dlgAlert.setPositiveButton(
                    "Verstanden"
                ) { _,_ -> //dialog, which ->
                    DeleteHandler.CTAG.d("Dialog Ok was clicked...")
                    //fsa.myMenu.performIdentifierAction(R.id.mi_delete_older_180, 0);
                }
                dlgAlert.setCancelable(false)
                DeleteHandler.CTAG.d("show dialog...")
                dlgAlert.create().show()
            }
            //
            for (oneChild in myChipGroup!!.children){
                var oneChip : Chip?
                try {
                    oneChip = oneChild as Chip
                } catch (e: Exception) {
                    oneChip = null
                }
                if(oneChip != null) {
                    setCCIP(true)
                    oneChip.isChecked = myChip0!!.isChecked
                    setCCIP(false)
                }
            }
        }
        else {
            // Es ist ein anderer Knopf - Logik: wenn es der "letzte" ist, chip0 mitselektieren
            var cntTotal: Int = 0
            var cntChecked: Int = 0
            var cntUnchecked: Int = 0
            for (oneChild in myChipGroup!!.children){
                var oneChip : Chip?
                try {
                    oneChip = oneChild as Chip
                } catch (e: Exception) {
                    oneChip = null
                }
                if(oneChip != null) {
                    cntTotal += 1
                    when {
                        oneChip.isChecked == false -> cntUnchecked += 1
                        oneChip.isChecked == true  -> cntChecked += 1
                    }
                }
            }
            // Wenn wir bei "unten alle gecheckt" sind, dann chip0 invertieren
            if( cntChecked + 1 == cntTotal ) {
                setCCIP(true)
                myChip0!!.isChecked = !(myChip0!!.isChecked)
                setCCIP(false)
            }
        }
        CTAG.leave()
    }
    ////////////////////////////////////////////////////////////////////////////
    companion object {
        val CTAG = AcmtLogger("C0H")
        //
        private var bCheckChangeIsProgrammatic :Boolean = false
        //
        fun setCCIP( b :Boolean ) { // doppelt setzen verhindern
            //CTAG.log_("setCCIP to: $b; current is: $bCheckChangeIsProgrammatic")
            if( b == bCheckChangeIsProgrammatic) {
                throw Exception("double set/reset is distorted flow")
            }
            bCheckChangeIsProgrammatic = b
        }
        //
        fun registerListenerForAllChips(v: View, chip0id: Int) {
            val c: Chip = v.findViewById(chip0id)
            var cg: ChipGroup = c.parent as ChipGroup
            c.setOnCheckedChangeListener(
                Chip0Handler(
                    c,
                    c,
                    cg
                )
            ) // c0, c0 - Alles-Knopf
            for (oneChild in cg.children){
                var oneChip : Chip?
                try {
                    oneChip = oneChild as Chip
                } catch (e: Exception) {
                    oneChip = null
                }
                // die anderen Knöpfe
                if(oneChip != null) {
                    oneChip.setOnCheckedChangeListener(
                        Chip0Handler(
                            oneChip,
                            c,
                            cg
                        )
                    )
                }
            }
        }
        //
        fun isC0( c :Chip ): Boolean {
            return( "Alle".toString() == c.text.toString() )
        }
        //
        fun isRealChip( v :View ): Boolean {
            try {
                val c:Chip = v as Chip
                //assert( c != null, "v: " + v.toString() + " must be a chip" )
                if(isC0(c))
                    return false
                return true
            } catch (e: Exception) {
                return false
            }
        }
    }
}
