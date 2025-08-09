package fureeish.mtg.sideboardplanprinter

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import fureeish.mtg.sideboardplanprinter.viewmodel.MainScreenViewModel
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.cdimascio.dotenv.dotenv
import mtg_sideboard_plan_printer.composeapp.generated.resources.JetBrainsMono_Regular
import mtg_sideboard_plan_printer.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.compose.viewmodel.rememberViewModel
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import kotlin.math.ceil

val di = DI {
    bindSingleton { dotenv() }
    bindProvider { MainScreenViewModel(instance()) }
}

@Composable
@Preview
fun App() = withDI(di) {
    Napier.base(DebugAntilog())

    MaterialTheme {
        val viewModel by rememberViewModel<MainScreenViewModel>()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = viewModel::chooseSideboardPlanFromFile) {
                Text("Choose sideboard plan from file")
            }

            Button(onClick = viewModel::printSideboardPlanToPDF) {
                Text("Print sideboard plan to PDF")
            }

            val jetBrainsMonoRegular = FontFamily(
                Font(Res.font.JetBrainsMono_Regular, weight = FontWeight.Normal)
            )

            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontFamily = jetBrainsMonoRegular
                )
            ) {
                SideboardPlanGrid(viewModel.sideboardPlanFileContents.collectAsState().value)
            }
        }
    }
}

@Composable
fun SideboardPlanGrid(
    table: List<List<String>>?,
) {
    if (table.isNullOrEmpty()) {
        Text("No file selected or file is empty.")
        return
    }

    val charWidth = rememberMonospaceCharWidth(
        LocalTextStyle.current.fontFamily!!, LocalTextStyle.current.fontSize
    )
    // Compute max width per column based on text length
    val columnWidths = remember(table) {
        val colCount = table.maxOfOrNull { it.size } ?: 0
        List(colCount) { colIndex ->
            val maxTextLength = table.maxOf { row ->
                val num = row.getOrNull(colIndex)?.length ?: 0
                num
            }
            (maxTextLength * ceil(charWidth)).coerceAtLeast(10f).dp
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Text("Preview:")
    Box(
        modifier = Modifier
            .fillMaxSize(0.75f)
            .background(Color.White)
            .border(1.dp, Color.Gray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
            ) {
                table.forEach { row ->
                    Row {
                        row.forEachIndexed { colIndex, cell ->
                            val spaceForPadding = 22.dp
                            Box(
                                modifier = Modifier
                                    .width(columnWidths[colIndex] + spaceForPadding)
                                    .border(1.dp, Color.Gray)
                                    .padding(8.dp),
                                contentAlignment = if (colIndex == 0) Alignment.CenterStart else Alignment.Center
                            ) {
                                Text(cell)
                            }
                        }
                    }
                }
            }
        }

        val scrollbarStyle = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 12.dp,
            shape = MaterialTheme.shapes.small,
            hoverDurationMillis = 300,
            unhoverColor = Color.DarkGray.copy(alpha = 0.5f),
            hoverColor = Color.Gray
        )

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalScrollState),
            modifier = Modifier.align(Alignment.CenterEnd),
            style = scrollbarStyle
        )

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomCenter),
            style = scrollbarStyle
        )
    }
}

@Composable
fun rememberMonospaceCharWidth(
    fontFamily: FontFamily,
    fontSize: TextUnit
): Float {
    val textMeasurer = rememberTextMeasurer()

    val widthPx = remember {
        val layoutResult = textMeasurer.measure(
            text = AnnotatedString("M"), // any character, since monospace
            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
        )
        layoutResult.size.width.toFloat()
    }

    return widthPx
}