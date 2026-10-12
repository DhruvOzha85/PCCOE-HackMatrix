package com.fin.mobile

import androidx.compose.ui.graphics.Color
import com.fin.mobile.ui.theme.FinAccentSaffron
import com.fin.mobile.ui.theme.FinBgPrimary
import com.fin.mobile.ui.theme.FinBorderSubtle
import com.fin.mobile.ui.theme.FinColorError
import com.fin.mobile.ui.theme.FinColorSuccess
import com.fin.mobile.ui.theme.FinColorWarning
import com.fin.mobile.ui.theme.FinPrimaryGreen
import com.fin.mobile.ui.theme.FinSurfaceWhite
import com.fin.mobile.ui.theme.FinTextMuted
import com.fin.mobile.ui.theme.FinTextPrimary
import com.fin.mobile.ui.theme.FinTextSecondary
import org.junit.Assert.assertEquals
import org.junit.Test

class FinThemeTokensTest {

    @Test
    fun testMandatoryPaletteColorsMatchSpec() {
        assertEquals("Background must match #F8F7F3", Color(0xFFF8F7F3), FinBgPrimary)
        assertEquals("Surface must match #FFFFFF", Color(0xFFFFFFFF), FinSurfaceWhite)
        assertEquals("Primary green must match #005B50", Color(0xFF005B50), FinPrimaryGreen)
        assertEquals("Dark navy text must match #10243A", Color(0xFF10243A), FinTextPrimary)
        assertEquals("Secondary text must match #344054", Color(0xFF344054), FinTextSecondary)
        assertEquals("Muted text must match #667085", Color(0xFF667085), FinTextMuted)
        assertEquals("Saffron accent must match #E98A00", Color(0xFFE98A00), FinAccentSaffron)
        assertEquals("Success must match #087443", Color(0xFF087443), FinColorSuccess)
        assertEquals("Warning must match #C56A00", Color(0xFFC56A00), FinColorWarning)
        assertEquals("Error must match #D92D20", Color(0xFFD92D20), FinColorError)
        assertEquals("Subtle border must match #E4E7EC", Color(0xFFE4E7EC), FinBorderSubtle)
    }
}
