package com.seriee.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.lifecycle.ViewModelProvider
import com.seriee.app.data.MediaItem
import com.seriee.app.data.MediaType
import com.seriee.app.data.SerieeRepository
import com.seriee.app.data.WatchStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class TmpProbeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun probe() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SerieeRepository(context)
        repo.saveLastSeenVersionCode(BuildConfig.VERSION_CODE)
        repo.saveItems(
            listOf(
                MediaItem(
                    id = "item-1",
                    title = "Arcane",
                    type = MediaType.Series,
                    status = WatchStatus.Watching,
                    poster = "",
                    season = 1,
                    currentEp = 3,
                    totalEp = 9,
                )
            )
        )
        println("PROBE repo items = " + repo.loadItems().map { it.id + ":" + it.title })
        composeRule.runOnUiThread { composeRule.activity.viewModelStore.clear() }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        val vm = ViewModelProvider(composeRule.activity)[SerieeViewModel::class.java]
        println("PROBE vm items = " + vm.items.map { it.id + ":" + it.title })
        println("PROBE nodes = " + composeRule.onAllNodesWithText("Arcane", substring = true).fetchSemanticsNodes().size)
    }
}
