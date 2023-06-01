package com.varabyte.kobweb.site

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.components.layout.Surface
import com.varabyte.kobweb.silk.components.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerBaseStyle
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.getColorMode
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.vh

private const val COLOR_MODE_KEY = "app:colorMode"

@InitSilk
fun initSilk(ctx: InitSilkContext) {
    ctx.config.initialColorMode = localStorage.getItem(COLOR_MODE_KEY)?.let { ColorMode.valueOf(it) } ?: ColorMode.DARK

    ctx.stylesheet.apply {
        registerBaseStyle("body") { Modifier.fontFamily("Roboto", "sans-serif") }
        registerBaseStyle("code, pre") { Modifier.fontFamily("Roboto Mono", "monospace") }

        val headerCommon = Modifier.fontWeight(FontWeight.Bold).textAlign(TextAlign.Center).margin(top = 2.cssRem, bottom = 1.cssRem)
        registerBaseStyle("h1") { headerCommon.fontSize(3.5.cssRem) }
        registerBaseStyle("h2") { headerCommon.fontSize(2.5.cssRem) }
        registerBaseStyle("h3") { headerCommon.fontSize(1.5.cssRem) }
        registerBaseStyle("h4") { headerCommon.fontSize(1.35.cssRem) }
    }
}

@App
@Composable
fun MyApp(content: @Composable () -> Unit) {
    SilkApp {
        val colorMode = getColorMode()
        LaunchedEffect(colorMode) {
            localStorage.setItem(COLOR_MODE_KEY, colorMode.name)
        }

        Surface(SmoothColorStyle.toModifier().fillMaxWidth().minHeight(100.vh)) {
            content()
        }
    }
}