package fureeish.mtg.sideboardplanprinter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class MainScreenViewModel() : ViewModel() {
    val chosenSideboardPlanFile = MutableStateFlow<File?>(null)

    val sideboardPlanFileContents: StateFlow<List<List<String>>?> = chosenSideboardPlanFile
        .map { file ->
            file?.takeIf { it.exists() }?.let {
                runCatching {
                    it.readText()
                        .lines()
                        .filter { line -> line.isNotBlank() }
                        .map { row -> row.split('\t') }
                }.getOrNull()
            } ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = emptyList()
        )

    fun chooseSideboardPlanFromFile() {
        val dialog = FileDialog(null as Frame?, "Select a file", FileDialog.LOAD)
        dialog.isVisible = true
        chosenSideboardPlanFile.value = dialog.files.firstOrNull()
    }

    fun printSideboardPlanToPDF() {
        println("Print sideboard plan to PDF")
    }
}