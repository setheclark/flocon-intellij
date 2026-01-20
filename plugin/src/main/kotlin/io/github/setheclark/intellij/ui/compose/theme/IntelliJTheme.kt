package io.github.setheclark.intellij.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.intellij.ui.JBColor
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * IntelliJ theme integration for Compose UI.
 * Leverages Jewel's built-in IntelliJ theme support and provides additional utilities.
 */
object IntelliJTheme {

    /**
     * Current color scheme from Jewel (automatically synced with IntelliJ theme)
     */
    val colors: Colors
        @Composable
        @ReadOnlyComposable
        get() = Colors

    /**
     * Typography styles for IntelliJ-themed text
     */
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = Typography

    /**
     * Provides access to IntelliJ platform colors as Compose colors
     */
    object Colors {
        val background: Color
            @Composable
            get() = JBColor.PanelBackground.toComposeColor()

        val foreground: Color
            @Composable
            get() = JBColor.foreground().toComposeColor()

        val border: Color
            @Composable
            get() = JBColor.border().toComposeColor()

        val separator: Color
            @Composable
            get() = JBColor.border().toComposeColor()

        val error: Color
            @Composable
            get() = JBColor.RED.toComposeColor()

        val warning: Color
            @Composable
            get() = JBColor.YELLOW.toComposeColor()

        val success: Color
            @Composable
            get() = JBColor.GREEN.toComposeColor()

        val selection: Color
            @Composable
            get() = JBColor.namedColor("Table.selectionBackground", JBColor(0x3875D6, 0x2F65CA)).toComposeColor()

        val selectedBackground: Color
            @Composable
            get() = JBColor.background().toComposeColor()

        val selectedForeground: Color
            @Composable
            get() = JBColor.foreground().toComposeColor()

        val hoverBackground: Color
            @Composable
            get() = JBColor.background().toComposeColor().copy(alpha = 0.1f)
    }

    /**
     * Typography styles for text
     */
    object Typography {
        val h1: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)

        val h2: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)

        val h3: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)

        val body: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp)

        val small: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)

        val caption: TextStyle
            @Composable
            get() = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp)
    }
}

/**
 * Converts AWT Color to Compose Color
 */
fun java.awt.Color.toComposeColor(): Color {
    return Color(red, green, blue, alpha)
}
