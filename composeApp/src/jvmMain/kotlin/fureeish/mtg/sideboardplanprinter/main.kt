package fureeish.mtg.sideboardplanprinter

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MTG-Sideboard-Plan-Printer",
    ) {
        App()
    }
}