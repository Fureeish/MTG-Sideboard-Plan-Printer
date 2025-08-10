package fureeish.mtg.sideboardplanprinter.latex

import fureeish.mtg.sideboardplanprinter.latex.LaTeXGenerator.Config.Layout
import fureeish.mtg.sideboardplanprinter.mtg.Card
import fureeish.mtg.sideboardplanprinter.mtg.SideboardPlan

object LaTeXGenerator {
    class Config private constructor() {
        companion object {
            operator fun invoke(builder: Config.() -> Unit) = Config().apply {
                builder()

                check(order.size == order.toSet().size) {
                    "Order options should not contain duplicates." +
                            "How would I sort based on a same criteria multiple times?"
                }

                val isJustByFileOccurrence = order.all { it is Order.ByFileOccurrence } && order.size == 1
                val noByFileOccurrencePresent = order.none { it is Order.ByFileOccurrence }

                check(isJustByFileOccurrence || noByFileOccurrencePresent) {
                    "ByFileOccurrence, if present, must be the only ordering criteria," +
                            "because any other ordering would render it meaningless."
                }
            }

            val default: Config
                get() = Config()
        }

        /**
         * Fields with default configuration values.
         */
        var withInOutHeader: Boolean = true
        var alignment: AlignTo = AlignTo.Left
        var layout: Layout = Layout.ClassicInOut
        var order: List<Order> = listOf(Order.ByPlanSize())


        /**
         * Types representing custom options.
         */

        /**
         * Represents alignment of columns' text in a single matchup table.
         */
        enum class AlignTo {
            Center, Left
        }

        /**
         * ClassicInOut represents a column for cards sided in and a column for cards sided out.
         * Full15 represents a column for cards sided in and a column for cards sided out extended by cards that were
         * fake-sided in.
         */
        enum class Layout {
            ClassicInOut, Full15
        }

        /**
         * Used for determining the order of the matchup plans in the resulting PDF.
         *
         * Criteria can be combined to create a set of rules for comparison.
         *
         * Example: `listOf(ByPlanSize(ascending = false), Alphabetically())` means that the resulting PDF's sideboard
         * plans are ordered from the greatest number of cards sided in/out to lowest (height of the sideboard plan
         * table). In case of ties (two tables are of the same height), they are ordered alphabetically with respect to
         * matchup name.
         *
         * Some criteria are exclusive, e.g., you can't sort twice by `ByFileOrdering(ascending = false)`
         * (or `ascending = true`), and if you specify `ByFileOccurrence`, you cannot specify any other criteria.
         */
        sealed class Order(open val ascending: Boolean = true) {
            data class ByFileOccurrence(override val ascending: Boolean = true) : Order(ascending)
            data class Alphabetically(override val ascending: Boolean = true) : Order(ascending)
            data class ByPlanSize(override val ascending: Boolean = true) : Order(ascending)
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
        val matchupPlans = withAlteredOrdering(getMatchupPlans(sideboardPlan, config), config)

        val latexRepresentation = produceLaTeXRepresentation(matchupPlans, columns, config)

        return """
            $preamble
            \begin{document}
            $latexRepresentation
            \end{document}
        """.trimIndent()
    }

    typealias MatchupPlan = Pair<String, List<Pair<String, String>>>

    private fun withAlteredOrdering(
        matchupPlans: List<MatchupPlan>,
        config: Config
    ): List<MatchupPlan> {
        val mutablePlans = matchupPlans.toMutableList()

        val comparators: List<Comparator<MatchupPlan>> = config.order
            .map {
                when (it) {
                    is Alphabetically -> Comparator.comparing { (name, _): MatchupPlan -> name } to it
                    is ByPlanSize -> Comparator.comparing { (_, plan): MatchupPlan -> plan.size } to it
                    is ByFileOccurrence -> Comparator.comparing { _: MatchupPlan -> 0 } to it
                }
            }.map { (comp, order) ->
                comp.takeIf { order.ascending } ?: comp.reversed()
            }

        mutablePlans.sortWith(comparators.reduce { acc, comp -> acc.thenComparing(comp) })

        return mutablePlans
    }

    private fun getMatchupPlans(
        sideboardPlan: SideboardPlan,
        config: Config
    ): List<MatchupPlan> {
        return sideboardPlan.matchupPlans
            .map { (matchup, sideboardForMatchup) ->
                var (cardsToAdd, cardsToRemove) = sideboardForMatchup.partition { it.count > 0 }

                if (config.layout == Layout.Full15) {
                    val fakeSideboardedCards = sideboardPlan.sideboard - cardsToAdd.toSet()
                    cardsToRemove += fakeSideboardedCards.map { it.copy(count = -it.count) }

                    // merge cards that are present in both sideboard and maindeck
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
            }
    }

    private fun produceLaTeXRepresentation(
        matchupPlans: List<MatchupPlan>,
        columns: Int,
        config: Config
    ): String {
        val rowSeparator = "\n\\vspace{1em}\n\n\\noindent\n"

        return matchupPlans
            .chunked(columns)
            .joinToString(
                prefix = "\\noindent\n",
                separator = rowSeparator
            ) { matchupsRow ->
                val columnSeparator = "\\hfill\n"

                matchupsRow
                    .joinToString(separator = columnSeparator) { (matchup, sideboardForMatchup) ->
                        val singlePlanTableWidth = String.format("%.3f", 1.0 / (1.05 * columns))

                        buildString {
                            append("\\begin{minipage}[t]{$singlePlanTableWidth\\textwidth}\n")
                            append("\\centering\n")
                            append("\\textbf{\\textsc{$matchup}}\\vphantom{Jy} \\\\\n")

                            val leftColumnAlignment = when (config.alignment) {
                                Config.AlignTo.Left -> "\\raggedright"
                                Config.AlignTo.Center -> "\\raggedleft"
                            }

                            append("\\begin{tabular}{@{}>{$leftColumnAlignment\\arraybackslash}p{0.45\\linewidth}|>{\\raggedright\\arraybackslash}p{0.45\\linewidth}@{}}")

                            if (config.withInOutHeader) {
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