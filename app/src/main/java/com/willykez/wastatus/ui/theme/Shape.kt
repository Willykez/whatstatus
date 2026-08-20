package com.willykez.wastatus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * A noticeably rounder, more expressive shape scale than Material3's
 * defaults (4/8/12/16/28dp) — used everywhere via `MaterialTheme.shapes.*`
 * so buttons, cards, sheets, dialogs, and text fields all pick it up
 * automatically for a softer, more contemporary silhouette without touching
 * any experimental API surface.
 */
val WaStatusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
