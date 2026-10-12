package com.fin.mobile.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

// FIN Canonical Radii from tokens.css
val FinRadiusSm = 6.dp
val FinRadiusMd = 10.dp
val FinRadiusLg = 14.dp

val FinMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(FinRadiusSm),
    small = RoundedCornerShape(FinRadiusSm),
    medium = RoundedCornerShape(FinRadiusMd),
    large = RoundedCornerShape(FinRadiusLg),
    extraLarge = RoundedCornerShape(24.dp)
)

@Immutable
data class FinCustomShapes(
    val sm: CornerBasedShape = RoundedCornerShape(FinRadiusSm),
    val md: CornerBasedShape = RoundedCornerShape(FinRadiusMd),
    val lg: CornerBasedShape = RoundedCornerShape(FinRadiusLg),
    val pill: CornerBasedShape = CircleShape
)

val LocalFinShapes = staticCompositionLocalOf { FinCustomShapes() }
