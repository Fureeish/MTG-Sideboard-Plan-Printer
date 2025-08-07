package fureeish.mtg.sideboardplanprinter

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mtg_sideboard_plan_printer.composeapp.generated.resources.Res
import mtg_sideboard_plan_printer.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
    ) {
        App()
    }
}