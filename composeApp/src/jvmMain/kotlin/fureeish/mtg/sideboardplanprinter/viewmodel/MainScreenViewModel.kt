package fureeish.mtg.sideboardplanprinter.viewmodel

import androidx.lifecycle.ViewModel
import java.awt.FileDialog
import java.awt.Frame

class MainScreenViewModel() : ViewModel() {
    fun chooseSideboardPlanFromFile() {
        val dialog = FileDialog(null as Frame?, "Select a file", FileDialog.LOAD)
        dialog.isVisible = true
        dialog.files.firstOrNull()
    }

    fun printSideboardPlanToPDF() {
        println("Print sideboard plan to PDF")
    }
}