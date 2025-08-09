package fureeish.mtg.sideboardplanprinter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fureeish.mtg.sideboardplanprinter.latex.LaTeXGenerator
import fureeish.mtg.sideboardplanprinter.mtg.SideboardPlan
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.flow.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class MainScreenViewModel(private val dotenv: Dotenv) : ViewModel() {
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
        val lualatexExe = "lualatex.exe"
        val exePath = if (dotenv.get("DEV_RUN").toBoolean()) {
            File("binaries/$lualatexExe")
        } else {
            File(System.getProperty("compose.application.resources.dir"), lualatexExe)
        }

        val dir = chosenSideboardPlanFile.value?.parentFile

        val latexFile = File("${dir?.absolutePath}/main.tex")
        latexFile.createNewFile()

        val plan = SideboardPlan.fromMatchupMatrix(sideboardPlanFileContents.value!!)
        val sideboardAsLaTeX = LaTeXGenerator.generateLaTeXFrom(plan.matchupPlans, 4)
        latexFile.writeText(sideboardAsLaTeX)

        ProcessBuilder(
            exePath.absolutePath,
            "-file-line-error",
            "-interaction=nonstopmode",
            "-synctex=1",
            "-output-format=pdf",
            "-output-directory=${chosenSideboardPlanFile.value?.parentFile?.absolutePath}",
            latexFile.absolutePath
        ).start()
    }
}