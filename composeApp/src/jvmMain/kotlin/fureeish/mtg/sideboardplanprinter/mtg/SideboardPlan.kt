package fureeish.mtg.sideboardplanprinter.mtg

import java.io.BufferedReader
import java.io.File

class SideboardPlan
private constructor(
    private var _matchupPlans: MutableMap<String, List<Card>> = mutableMapOf(),
    val sideboard: List<Card>
) {
    companion object {
        fun fromFile(path: String) = fromFile(File(path))

        fun fromFile(file: File) = fromReader(file.bufferedReader())

        fun fromResource(resourceName: String) = fromReader(
            object {}.javaClass.getResourceAsStream("/$resourceName")?.bufferedReader()!!
        )

        fun fromReader(reader: BufferedReader): SideboardPlan {
            val matchupMatrix = reader
                .useLines { line ->
                    line.map { row -> row.split('\t') }.toMutableList()
                }
                .takeWhile { row -> row.first() != "Play/draw diff:" }
                .toMutableList()

            return fromMatchupMatrix(matchupMatrix)
        }

        fun fromMatchupMatrix(matchupMatrix: List<List<String>>): SideboardPlan {
            val (maindeckCardIndexes, sideboardCardIndexes) = getCardIndexesFrom(matchupMatrix)

            val columnIndexOfFirstMatchup = 2
            val columnIndexOfLastMatchup = matchupMatrix[0]
                .mapIndexed { index, matchup -> index to matchup }
                .dropWhile { (_, matchup) -> matchup.isBlank() }
                .dropWhile { (_, matchup) -> matchup.isNotBlank() }
                .first()
                .first - 1

            val sideboard = matchupMatrix
                .mapIndexed { index, cardRow ->
                    index to cardRow
                }
                .filter { (index, _) -> index in sideboardCardIndexes }
                .map { (_, row) -> Card(row[1].trim().toInt(), row[0].trim())  }

            return SideboardPlan(sideboard = sideboard).apply {

                (columnIndexOfFirstMatchup..columnIndexOfLastMatchup).map { matchupIndex ->
                    val matchup = matchupMatrix[0][matchupIndex].trim()

                    fun cardIndexToSideboardedCard(cardIndex: Int, from: From): Card? {
                        val cardName = matchupMatrix[cardIndex][0]
                        val inoutCount = matchupMatrix[cardIndex][matchupIndex]
                        if (inoutCount.replace("※", "").isBlank()) return null
                        return Card(from.alterCount(inoutCount.toInt()), cardName)
                    }

                    val cardsSidedIn = sideboardCardIndexes
                        .mapNotNull { cardIndexToSideboardedCard(it, From.Sideboard) }

                    val cardsSidedOut = maindeckCardIndexes
                        .mapNotNull { cardIndexToSideboardedCard(it, From.Maindeck) }

                    _matchupPlans.put(matchup, cardsSidedIn + cardsSidedOut)
                }
            }
        }

        private fun getCardIndexesFrom(matchupMatrix: List<List<String>>): CardIndexes {
            val allIndexedCards = matchupMatrix
                .mapIndexed { index, cardName -> index to cardName.first() }

            val maindeckCardIndexesList = allIndexedCards
                .dropWhile { (_, cardName) -> cardName.isBlank() }
                .takeWhile { (_, cardName) -> cardName != "Maindeck" }
                .map { (index, _) -> index }

            val sideboardCardIndexesList = allIndexedCards
                .dropWhile { (_, cardName) -> cardName != "Sideboard" }
                .drop(1)
                .map { (index, _) -> index }

            val maindeckCardIndexes = maindeckCardIndexesList.first()..maindeckCardIndexesList.last()
            val sideboardCardIndexes = sideboardCardIndexesList.first()..sideboardCardIndexesList.last()

            return CardIndexes(maindeckCardIndexes, sideboardCardIndexes)
        }
    }

    val matchupPlans: Map<String, List<Card>>
        get() = _matchupPlans

    private enum class From {
        Maindeck, Sideboard;

        fun alterCount(count: Int) = if (this == Maindeck) -count else count
    }

    private data class CardIndexes(
        val maindeckCardIndexes: IntRange,
        val sideboardCardIndexes: IntRange
    )
}

data class Card(val count: Int, val cardName: String) {
    companion object {
        val abbreviatedCardNames = mapOf(
            "Archon of Cruelty" to "Archon",
            "Atraxa, Grand Unifier" to "Atraxa",
            "Brazen Borrower // Petty Theft" to "Borrower",
            "Murktide Regent" to "Murktide",
            "Orcish Bowmasters" to "Bowmaster",
            "Tamiyo, Inquisitive Student // Tamiyo, Seasoned Scholar" to "Tamiyo",
            "Animate Dead" to "Animate",
            "Force of Will" to "FoW",
            "Consign to Memory" to "Consign",
            "Dauthi Voidwalker" to "Voidwalker",
            "Fatal Push" to "Push",
            "Force of Negation" to "FoN",
            "Sheoldred's Edict" to "Edict",
            "Thoughtseize" to "TSeize",
        )
    }

    override fun toString(): String = String.format("%+-2d %s", count, cardName).replace('-', '\u2212') // unicode minus

    fun toAbbreviated() = copy(
        count = this.count,
        cardName = abbreviatedCardNames[this.cardName] ?: this.cardName
    )
}