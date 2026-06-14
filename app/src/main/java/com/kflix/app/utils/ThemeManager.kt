package com.kflix.app.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.kflix.app.R

object ThemeManager {
    const val DEFAULT = "default"
    const val NERO_AMOLED_OLED = "nero_amoled_oled"
    const val SUNSET_CINEMA = "sunset_cinema"
    const val STEEL_BLUE = "steel_blue"
    const val FOREST_NIGHT = "forest_night"
    const val CRIMSON_NOIR = "crimson_noir"
    const val MIDNIGHT_VIOLET = "midnight_violet"
    const val NORD_FROST = "nord_frost"
    const val EMERALD_LUXE = "emerald_luxe"
    const val RETRO_NEON = "retro_neon"

    data class Palette(
        @ColorInt val mobileNavBackground: Int,
        @ColorInt val mobileNavActive: Int,
        @ColorInt val mobileNavInactive: Int,
        @ColorInt val systemBar: Int,
        @ColorInt val tvNavBackground: Int,
        @ColorInt val tvHeaderPrimary: Int,
        @ColorInt val tvHeaderSecondary: Int,
    )

    @StyleRes
    fun mobileThemeRes(theme: String): Int = when (theme) {
        NERO_AMOLED_OLED -> R.style.AppTheme_Mobile_NeroAmoledOled
        SUNSET_CINEMA -> R.style.AppTheme_Mobile_SunsetCinema
        STEEL_BLUE -> R.style.AppTheme_Mobile_SteelBlue
        FOREST_NIGHT -> R.style.AppTheme_Mobile_ForestNight
        CRIMSON_NOIR -> R.style.AppTheme_Mobile_CrimsonNoir
        MIDNIGHT_VIOLET -> R.style.AppTheme_Mobile_MidnightViolet
        NORD_FROST -> R.style.AppTheme_Mobile_NordFrost
        EMERALD_LUXE -> R.style.AppTheme_Mobile_EmeraldLuxe
        RETRO_NEON -> R.style.AppTheme_Mobile_RetroNeon
        else -> R.style.AppTheme_Mobile
    }

    @StyleRes
    fun tvThemeRes(theme: String): Int = when (theme) {
        NERO_AMOLED_OLED -> R.style.AppTheme_NeroAmoledOled
        SUNSET_CINEMA -> R.style.AppTheme_SunsetCinema
        STEEL_BLUE -> R.style.AppTheme_SteelBlue
        FOREST_NIGHT -> R.style.AppTheme_ForestNight
        CRIMSON_NOIR -> R.style.AppTheme_CrimsonNoir
        MIDNIGHT_VIOLET -> R.style.AppTheme_MidnightViolet
        NORD_FROST -> R.style.AppTheme_NordFrost
        EMERALD_LUXE -> R.style.AppTheme_EmeraldLuxe
        RETRO_NEON -> R.style.AppTheme_RetroNeon
        else -> R.style.AppTheme_Tv
    }

    @StringRes
    fun titleRes(theme: String): Int = when (theme) {
        NERO_AMOLED_OLED -> R.string.theme_nero_amoled_oled
        SUNSET_CINEMA -> R.string.theme_sunset_cinema
        STEEL_BLUE -> R.string.theme_steel_blue
        FOREST_NIGHT -> R.string.theme_forest_night
        CRIMSON_NOIR -> R.string.theme_crimson_noir
        MIDNIGHT_VIOLET -> R.string.theme_midnight_violet
        NORD_FROST -> R.string.theme_nord_frost
        EMERALD_LUXE -> R.string.theme_emerald_luxe
        RETRO_NEON -> R.string.theme_retro_neon
        else -> R.string.theme_default
    }

    fun palette(theme: String): Palette = when (theme) {
        NERO_AMOLED_OLED -> Palette(
            mobileNavBackground = color("#1A0A2E"),
            mobileNavActive = color("#FF1493"),
            mobileNavInactive = color("#B899D4"),
            systemBar = color("#1A0A2E"),
            tvNavBackground = color("#1A0A2E"),
            tvHeaderPrimary = color("#F5E6FF"),
            tvHeaderSecondary = color("#B899D4"),
        )
        SUNSET_CINEMA -> Palette(
            mobileNavBackground = color("#E8EDF5"),
            mobileNavActive = color("#FFB6C1"),
            mobileNavInactive = color("#6B7B8D"),
            systemBar = color("#E8EDF5"),
            tvNavBackground = color("#E8EDF5"),
            tvHeaderPrimary = color("#1A1A2E"),
            tvHeaderSecondary = color("#6B7B8D"),
        )
        STEEL_BLUE -> Palette(
            mobileNavBackground = color("#150F20"),
            mobileNavActive = color("#FF6B8A"),
            mobileNavInactive = color("#B8A0D4"),
            systemBar = color("#150F20"),
            tvNavBackground = color("#150F20"),
            tvHeaderPrimary = color("#F0E6FF"),
            tvHeaderSecondary = color("#B8A0D4"),
        )
        FOREST_NIGHT -> Palette(
            mobileNavBackground = color("#1A1C23"),
            mobileNavActive = color("#FF1493"),
            mobileNavInactive = color("#9AA5B8"),
            systemBar = color("#1A1C23"),
            tvNavBackground = color("#1A1C23"),
            tvHeaderPrimary = color("#F0F4FF"),
            tvHeaderSecondary = color("#9AA5B8"),
        )
        CRIMSON_NOIR -> Palette(
            mobileNavBackground = color("#1A0A0A"),
            mobileNavActive = color("#FF69B4"),
            mobileNavInactive = color("#D6B2BA"),
            systemBar = color("#1A0A0A"),
            tvNavBackground = color("#1A0A0A"),
            tvHeaderPrimary = color("#FFECEF"),
            tvHeaderSecondary = color("#D6B2BA"),
        )
        MIDNIGHT_VIOLET -> Palette(
            mobileNavBackground = color("#0A0515"),
            mobileNavActive = color("#DA70D6"),
            mobileNavInactive = color("#BFB9DD"),
            systemBar = color("#0A0515"),
            tvNavBackground = color("#0A0515"),
            tvHeaderPrimary = color("#F1EEFF"),
            tvHeaderSecondary = color("#BFB9DD"),
        )
        NORD_FROST -> Palette(
            mobileNavBackground = color("#0D1520"),
            mobileNavActive = color("#FFB6C1"),
            mobileNavInactive = color("#B1C9D6"),
            systemBar = color("#0D1520"),
            tvNavBackground = color("#0D1520"),
            tvHeaderPrimary = color("#EAF7FD"),
            tvHeaderSecondary = color("#B1C9D6"),
        )
        EMERALD_LUXE -> Palette(
            mobileNavBackground = color("#0D1A15"),
            mobileNavActive = color("#FF1493"),
            mobileNavInactive = color("#B6D0C0"),
            systemBar = color("#0D1A15"),
            tvNavBackground = color("#0D1A15"),
            tvHeaderPrimary = color("#EDF9F2"),
            tvHeaderSecondary = color("#B6D0C0"),
        )
        RETRO_NEON -> Palette(
            mobileNavBackground = color("#0A0015"),
            mobileNavActive = color("#FF00FF"),
            mobileNavInactive = color("#CFB7DA"),
            systemBar = color("#0A0015"),
            tvNavBackground = color("#0A0015"),
            tvHeaderPrimary = color("#F4F7FF"),
            tvHeaderSecondary = color("#CFB7DA"),
        )
        else -> Palette(
            mobileNavBackground = color("#FFF5F7"),
            mobileNavActive = color("#FF1493"),
            mobileNavInactive = color("#FFB6C1"),
            systemBar = color("#FFF5F7"),
            tvNavBackground = color("#FFF5F7"),
            tvHeaderPrimary = color("#2D0A1E"),
            tvHeaderSecondary = color("#7A4B6B"),
        )
    }

    @ColorInt
    private fun color(hex: String): Int = Color.parseColor(hex)
}
