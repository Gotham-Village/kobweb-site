package com.varabyte.kobweb.site.components.layouts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxHeight
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import com.varabyte.kobweb.site.components.sections.listing.ListingSideBar
import com.varabyte.kobweb.site.model.listing.*
import com.varabyte.kobwebx.markdown.markdown
import kotlinx.browser.window
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

fun PageContext.RouteInfo.toArticleInfo(): Pair<Category, Article>? {
    val categorySlug = path.substringBeforeLast('/').substringAfterLast('/')
    val category = SITE_LISTING.find { it.slug == categorySlug } ?: return null
    val subcategorySlug = path.substringAfterLast('/')
    val article = SITE_LISTING.findArticle(categorySlug, subcategorySlug) ?: return null
    return category to article
}

@Composable
fun DocsLayout(content: @Composable () -> Unit) {
    val ctx = rememberPageContext()

    val info = ctx.markdown?.let { ctx.route.toArticleInfo() }
    val title = if (info != null) {
        "Docs - ${info.first.title} - ${info.second.title}"
    } else "Docs"

    PageLayout(title) {
        Row(Modifier.fillMaxWidth().maxWidth(1200.px).fillMaxHeight()) {
            ListingSideBar()
            content()
        }
    }
}