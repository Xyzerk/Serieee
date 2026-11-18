package com.seriee.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "Missing only" chip in the add/edit sheet.
 *
 * The chip used to be hidden as soon as nothing was missing, even while the
 * filter was still active, which left the episode list empty with no control to
 * turn the filter off again.
 */
class MissingFilterChipTest {

    @Test
    fun `chip is hidden when there are no episodes to filter`() {
        assertFalse(showMissingOnlyChip(hasEpisodes = false, missingCount = 0, total = 10, filterActive = false))
    }

    @Test
    fun `chip is shown while some episodes are missing`() {
        assertTrue(showMissingOnlyChip(hasEpisodes = true, missingCount = 5, total = 10, filterActive = false))
        assertTrue(showMissingOnlyChip(hasEpisodes = true, missingCount = 1, total = 10, filterActive = false))
    }

    @Test
    fun `chip is hidden when nothing is missing and the filter is off`() {
        assertFalse(showMissingOnlyChip(hasEpisodes = true, missingCount = 0, total = 10, filterActive = false))
        // Missing == total means no episode is assigned, so filtering would change nothing.
        assertFalse(showMissingOnlyChip(hasEpisodes = true, missingCount = 10, total = 10, filterActive = false))
    }

    @Test
    fun `chip stays visible while the filter is active so it can be turned off`() {
        // The last gap was just filled.
        assertTrue(showMissingOnlyChip(hasEpisodes = true, missingCount = 0, total = 10, filterActive = true))
        // The episode count shrank below the assigned episodes.
        assertTrue(showMissingOnlyChip(hasEpisodes = true, missingCount = 0, total = 5, filterActive = true))
    }
}
