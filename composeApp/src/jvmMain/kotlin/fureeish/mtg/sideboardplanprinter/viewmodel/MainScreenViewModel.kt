package fureeish.mtg.sideboardplanprinter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fureeish.mtg.sideboardplanprinter.latex.LaTeXGenerator
import fureeish.mtg.sideboardplanprinter.mtg.SideboardPlan
import fureeish.mtg.sideboardplanprinter.ui.ToastHostState
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class MainScreenViewModel(private val dotenv: Dotenv, private val toastHostState: ToastHostState) : ViewModel() {
    val outputFileName = "Sideboard"

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

    fun showToast(message: String, durationMillis: Int = 2000) {
        toastHostState.let { state ->
            CoroutineScope(Dispatchers.Main).launch {
                state.showToast(message, durationMillis)
            }
        }
    }

    fun printSideboardPlanToPDF() {
        if (chosenSideboardPlanFile.value == null) {
            showToast("File with sideboard plan not chosen!")
            return
        }

        val workingDirectory = chosenSideboardPlanFile.value?.parentFile ?: error("Cannot access chosen file's directory.")
        val filesThatWereThereBeforeWeMadeMess = workingDirectory.listFiles()

        val latexFile = File("${workingDirectory.absolutePath}/$outputFileName.tex")
        latexFile.createNewFile()

        val plan = SideboardPlan.fromMatchupMatrix(sideboardPlanFileContents.value!!)
        val sideboardAsLaTeX = LaTeXGenerator.generateLaTeXFrom(plan.matchupPlans, 4)
        latexFile.writeText(sideboardAsLaTeX)

        CoroutineScope(Dispatchers.IO).launch {
            val process = ProcessBuilder(
                locateLuaLaTeXExecutable().absolutePath,
                "-file-line-error",
                "-interaction=nonstopmode",
                "-synctex=1",
                "-output-format=pdf",
                "-output-directory=${workingDirectory.absolutePath}",
                latexFile.absolutePath
            ).start()

            process.waitFor()

            removeAllAuxiliaryFiles(workingDirectory, filesThatWereThereBeforeWeMadeMess)
        }
    }

    private fun locateLuaLaTeXExecutable(): File {
        val lualatexExe = "lualatex.exe"
        val exePath = if (dotenv.get("DEV_RUN").toBoolean()) {
            File("binaries/$lualatexExe")
        } else {
            File(System.getProperty("compose.application.resources.dir"), lualatexExe)
        }
        return exePath
    }

    private fun removeAllAuxiliaryFiles(
        workingDirectory: File,
        filesThatWereThereBeforeWeMadeMess: Array<File>
    ) {
        val filesToRetain = (filesThatWereThereBeforeWeMadeMess + File("${workingDirectory.absolutePath}/$outputFileName.pdf")).map(File::getCanonicalFile)
        workingDirectory
            .walk()
            .maxDepth(1)
            .filter { it !in filesToRetain }
            .forEach { it.delete() }
    }
}