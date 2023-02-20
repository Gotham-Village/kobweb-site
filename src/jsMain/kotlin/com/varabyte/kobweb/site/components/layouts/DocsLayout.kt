package com.varabyte.kobweb.site.components.layouts

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.site.model.listing.SITE_LISTING
import com.varabyte.kobweb.site.model.listing.findArticle
import com.varabyte.kobwebx.markdown.markdown
import kotlinx.browser.window

@Composable
fun DocsLayout(content: @Composable () -> Unit) {
    val ctx = rememberPageContext()

    val title = ctx.markdown?.let {
        // If here, we know we're inside a markdown docs page

        // e.g. convert "/docs/widgets/button" to "widgets"
        val categorySlug = window.location.pathname.substringBeforeLast('/').substringAfterLast('/')
        @Suppress("DEPRECATION")
        val categoryName = SITE_LISTING.find { it.slug == categorySlug }?.title ?: categorySlug.capitalize()
        val articleSlug = window.location.pathname.substringAfterLast('/')
        @Suppress("DEPRECATION")
        val articleName = SITE_LISTING.findArticle(categorySlug, articleSlug)?.title ?: ctx.slug.capitalize()
        "Docs - $categoryName - $articleName"
    } ?: "Docs"

    PageLayout(title, content)
}