package vadiole.opaltest.feature.referral

import org.junit.Assert.assertEquals
import org.junit.Test
import vadiole.opaltest.feature.referral.PaneLayout.ShortTwoPane
import vadiole.opaltest.feature.referral.PaneLayout.Single
import vadiole.opaltest.feature.referral.PaneLayout.TwoPane

class ReferralLayoutTest {
    @Test
    fun singleTierMapsToTheTiersPane() {
        val single = PanePositions(single = ScrollPosition(4, 30))
        assertEquals(PanePositions(end = ScrollPosition(1, 30)), remapScroll(Single, TwoPane, single))
        assertEquals(PanePositions(end = ScrollPosition(2, 30)), remapScroll(Single, ShortTwoPane, single))
    }

    @Test
    fun singleHeaderMapsToTheHeaderPane() {
        val share = PanePositions(single = ScrollPosition(2, 10))
        assertEquals(PanePositions(start = ScrollPosition(2, 10)), remapScroll(Single, TwoPane, share))
        assertEquals(PanePositions(start = ScrollPosition(1, 10)), remapScroll(Single, ShortTwoPane, share))
    }

    @Test
    fun inviteCardMovesToTheEndPaneInShortWindows() {
        val invite = PanePositions(single = ScrollPosition(0, 50))
        assertEquals(PanePositions(end = ScrollPosition(0, 50)), remapScroll(Single, ShortTwoPane, invite))
        assertEquals(invite, remapScroll(ShortTwoPane, Single, PanePositions(end = ScrollPosition(0, 50))))
    }

    @Test
    fun scrolledTiersPaneWinsOverTheHeaderPane() {
        val panes = PanePositions(start = ScrollPosition(1, 20), end = ScrollPosition(3, 40))
        assertEquals(PanePositions(single = ScrollPosition(6, 40)), remapScroll(TwoPane, Single, panes))
        assertEquals(PanePositions(single = ScrollPosition(5, 40)), remapScroll(ShortTwoPane, Single, panes))
    }

    @Test
    fun headerPaneIsUsedWhenTiersAreAtTheTop() {
        val panes = PanePositions(start = ScrollPosition(1, 20))
        assertEquals(PanePositions(single = ScrollPosition(1, 20)), remapScroll(TwoPane, Single, panes))
        assertEquals(PanePositions(single = ScrollPosition(2, 20)), remapScroll(ShortTwoPane, Single, panes))
    }

    @Test
    fun twoPaneVariantsMapThroughTheSingleList() {
        val tier = PanePositions(end = ScrollPosition(2, 5))
        assertEquals(PanePositions(end = ScrollPosition(3, 5)), remapScroll(TwoPane, ShortTwoPane, tier))
        assertEquals(tier, remapScroll(ShortTwoPane, TwoPane, PanePositions(end = ScrollPosition(3, 5))))
    }

    @Test
    fun codeRowsAreBalanced() {
        assertEquals(5, balancedCellsPerRow(length = 5, fit = 7))
        assertEquals(7, balancedCellsPerRow(length = 7, fit = 7))
        assertEquals(4, balancedCellsPerRow(length = 8, fit = 7))
        assertEquals(5, balancedCellsPerRow(length = 10, fit = 7))
        assertEquals(4, balancedCellsPerRow(length = 10, fit = 4))
        assertEquals(1, balancedCellsPerRow(length = 3, fit = 0))
    }
}
