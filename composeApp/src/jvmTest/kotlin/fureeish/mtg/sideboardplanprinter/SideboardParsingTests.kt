package fureeish.mtg.sideboardplanprinter

import fureeish.mtg.sideboardplanprinter.mtg.Card
import fureeish.mtg.sideboardplanprinter.mtg.SideboardPlan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SideboardParsingTests : StringSpec({
    "Loading a sample sideboard plan correctly extracts all sideboard cards" {
        val sideboard = SideboardPlan.fromResource("UB Reanimator BBB - UB Reanimator v1.1.tsv").sideboard
        val expected = listOf(
            Card(2, "Barrowgoyf"),
            Card(1, "Brazen Borrower // Petty Theft"),
            Card(2, "Consign to Memory"),
            Card(3, "Dauthi Voidwalker"),
            Card(2, "Fatal Push"),
            Card(2, "Force of Negation"),
            Card(2, "Null Rod"),
            Card(1, "Sheoldred's Edict"),
        )

        sideboard.toSet() shouldBe expected.toSet()
    }

    "Loading a sample sideboard plan correctly extracts all matchups" {
        val sideboardPlans = SideboardPlan.fromResource("UB Reanimator BBB - UB Reanimator v1.1.tsv").matchupPlans

        sideboardPlans.size shouldBe 34
    }
})