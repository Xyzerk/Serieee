package com.seriee.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeFileParserTest {

    @Test
    fun `parses SxxExx filenames`() {
        assertEquals(1 to 5, EpisodeFileParser.parseSeasonEpisode("Show.S01E05.1080p.mkv"))
        assertEquals(4 to 12, EpisodeFileParser.parseSeasonEpisode("Show - s4e12 - title.mp4"))
        assertEquals(1 to 5, EpisodeFileParser.parseSeasonEpisode("Show.S01.E05.mkv"))
    }

    @Test
    fun `returns null when no season and episode pair exists`() {
        assertNull(EpisodeFileParser.parseSeasonEpisode("Show.E05.mkv"))
        assertNull(EpisodeFileParser.parseSeasonEpisode("random-file.mkv"))
    }

    @Test
    fun `parses episode markers`() {
        assertEquals(7, EpisodeFileParser.parseEpisode("Show.EP07.mkv"))
        assertEquals(7, EpisodeFileParser.parseEpisode("Show.Episode 7.mkv"))
    }

    @Test
    fun `parses trailing episode numbers`() {
        assertEquals(12, EpisodeFileParser.parseEpisode("Show - 12.mkv"))
        assertEquals(3, EpisodeFileParser.parseEpisode("Show_03.mp4"))
    }

    @Test
    fun `returns null when no episode number exists`() {
        assertNull(EpisodeFileParser.parseEpisode("Show.mkv"))
        assertNull(EpisodeFileParser.parseEpisode("Show.Special.mkv"))
    }

    @Test
    fun `parses release-group names with no show title`() {
        assertEquals(1, EpisodeFileParser.parseEpisode("CHFY--01.mkv"))
        assertEquals(12, EpisodeFileParser.parseEpisode("CHFY--12.mkv"))
        assertEquals(7, EpisodeFileParser.parseEpisode("[SubsPlease] CHFY - 07 (1080p) [ABCD1234].mkv"))
        assertEquals(1, EpisodeFileParser.parseEpisode("CHFY--01v2.mkv"))
    }

    @Test
    fun `ignores resolution, codec, year and audio tokens`() {
        assertEquals(3, EpisodeFileParser.parseEpisode("Show.2024.1080p.x265.03.mkv"))
        assertEquals(2, EpisodeFileParser.parseEpisode("Show - 02 [DD5.1].mkv"))
        assertNull(EpisodeFileParser.parseEpisode("Show.2024.1080p.mkv"))
    }

    @Test
    fun `strips hidden alternate titles before scanning`() {
        val titles = listOf("Kimetsu no Yaiba", "Demon Slayer")
        assertEquals(5, EpisodeFileParser.parseEpisode("[Group] Kimetsu no Yaiba - 05 [1080p].mkv", titles))
        assertEquals(9, EpisodeFileParser.parseEpisode("Demon.Slayer.09.1080p.mkv", titles))
    }

    @Test
    fun `respects the known episode count`() {
        assertEquals(7, EpisodeFileParser.parseEpisode("Show 1080 07.mkv", listOf("Show"), maxEpisodes = 12))
        assertNull(EpisodeFileParser.parseEpisode("Show 1080 99.mkv", listOf("Show"), maxEpisodes = 12))
    }

    @Test
    fun `natural order sorts episode numbers numerically`() {
        val names = listOf("ep10.mkv", "ep2.mkv", "ep1.mkv")
        assertEquals(
            listOf("ep1.mkv", "ep2.mkv", "ep10.mkv"),
            names.sortedWith(Comparator(::naturalFileOrder))
        )
    }
}
