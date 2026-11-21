package com.seriee.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterStoreTest {

    @Test
    fun `keeps known image extensions`() {
        assertEquals("jpeg", PosterStore.sanitizeExtension("jpeg"))
        assertEquals("png", PosterStore.sanitizeExtension("PNG"))
        assertEquals("webp", PosterStore.sanitizeExtension(".webp"))
        assertEquals("heic", PosterStore.sanitizeExtension(" heic "))
    }

    @Test
    fun `falls back to jpg for unknown or unsafe extensions`() {
        assertEquals("jpg", PosterStore.sanitizeExtension(null))
        assertEquals("jpg", PosterStore.sanitizeExtension(""))
        assertEquals("jpg", PosterStore.sanitizeExtension("svg+xml"))
        assertEquals("jpg", PosterStore.sanitizeExtension("../../evil"))
        assertEquals("jpg", PosterStore.sanitizeExtension("php"))
    }
}
