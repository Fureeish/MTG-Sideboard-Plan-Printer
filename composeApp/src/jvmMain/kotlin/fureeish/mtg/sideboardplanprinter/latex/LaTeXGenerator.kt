package fureeish.mtg.sideboardplanprinter.latex

import fureeish.mtg.sideboardplanprinter.latex.LaTeXGenerator.Config.Layout
import fureeish.mtg.sideboardplanprinter.mtg.Card
import fureeish.mtg.sideboardplanprinter.mtg.SideboardPlan

object LaTeXGenerator {
    class Config private constructor() {
        companion object {
            operator fun invoke(builder: Config.() -> Unit) = Config().apply(builder)
            val default: Config
                get() = Config()
        }

        var withInOut: Boolean = true
        var alignment: AlignTo = AlignTo.Left
        var layout: Layout = Layout.ClassicInOut

        sealed class AlignTo {
            object Center : AlignTo()
            object Left : AlignTo()
        }

        sealed class Layout {
            object ClassicInOut : Layout()
            object Full15 : Layout()
        }
    }

    private val preamble = """
        \documentclass[9pt]{extarticle}
        \usepackage[lining]{ebgaramond}
        \usepackage[margin=0.75in]{geometry}
        \usepackage{array}
        \usepackage{microtype}
    """.trimIndent()

    fun generateLaTeXFrom(
        sideboardPlan: SideboardPlan,
        columns: Int,
        config: Config = Config.default
    ): String {
        val matchupsSortedByPlanSizes = sideboardPlan.matchupPlans
            .map { (matchup, sideboardForMatchup) ->
                var (cardsToAdd, cardsToRemove) = sideboardForMatchup.partition { it.count > 0 }

                if (config.layout == Layout.Full15) {
                    val fakeSideboardedCards = sideboardPlan.sideboard - cardsToAdd.toSet()
                    cardsToRemove += fakeSideboardedCards.map { it.copy(count = -it.count) }
                    cardsToRemove = cardsToRemove.groupingBy { it.cardName }
                        .reduce { _, acc, newCard -> Card(acc.count + newCard.count, acc.cardName) }
                        .values
                        .toList()
                }

                val inOutPlan = zipWithPadding(
                    cardsToAdd.sortedByDescending(Card::count),
                    cardsToRemove.sortedBy(Card::count)
                ).map { (`in`, out) ->
                    val inAsText = `in`?.toAbbreviated()?.toString().orEmpty()
                    val outAsText = out?.toAbbreviated()?.toString().orEmpty()
                    inAsText to outAsText
                }

                matchup to inOutPlan
            }.sortedByDescending { (_, inoutPlan) ->
                inoutPlan.size
            }

        val latexRepresentation = matchupsSortedByPlanSizes
            .chunked(columns)
            .joinToString(
                prefix = "\\noindent\n",
                separator = "\n\\vspace{1em}\n\n\\noindent\n"
            ) { matchupsRow ->
                matchupsRow
                    .joinToString(separator = "\\hfill\n") { (matchup, sideboardForMatchup) ->
                        val singlePlanWidth = String.format("%.3f", 1.0 / columns / 1.05)

                        buildString {
                            append("\\begin{minipage}[t]{$singlePlanWidth\\textwidth}\n")
                            append("\\centering\n")
                            append("\\textbf{\\textsc{$matchup}}\\vphantom{Jy} \\\\\n")

                            val leftColumnAlignment = when (config.alignment) {
                                Config.AlignTo.Left -> "\\raggedright"
                                Config.AlignTo.Center -> "\\raggedleft"
                            }

                            append("\\begin{tabular}{@{}>{$leftColumnAlignment\\arraybackslash}p{0.45\\linewidth}|>{\\raggedright\\arraybackslash}p{0.45\\linewidth}@{}}")

                            if (config.withInOut) {
                                append("\\hfill\\textbf{\\textsc{in}} & \\textbf{\\textsc{out}} \\\\\n")
                            }

                            append("\\hline\n")

                            sideboardForMatchup.forEach { (`in`, out) ->
                                append("$`in` & $out\\\\\n")
                            }

                            append("\\end{tabular}\n")
                            append("\\end{minipage}\n")
                        }
                    }
            }

        return """
            $preamble
            \begin{document}
            $latexRepresentation
            \end{document}
        """.trimIndent()
    }

    private fun <T, U> zipWithPadding(
        list1: List<T>, list2: List<U>
    ): List<Pair<T?, U?>> {
        val maxSize = maxOf(list1.size, list2.size)

        return (0 until maxSize).map { i ->
            val first = list1.getOrNull(i)
            val second = list2.getOrNull(i)
            first to second
        }
    }
}