import com.varabyte.kobweb.gradle.application.notifyKobwebAboutFrontendCodeGeneratingTask
import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.link
import kotlinx.html.script
import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomBlock
import org.gradle.configurationcache.extensions.capitalized

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kobweb.application)
    alias(libs.plugins.kobwebx.markdown)
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://us-central1-maven.pkg.dev/varabyte-repos/public")
}

group = "com.varabyte.kobweb.site"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            head.add {
                link {
                    rel = "stylesheet"
                    href = "/highlight.js/styles/dracula.css"
                }
                script {
                    src = "/highlight.js/highlight.min.js"
                }
            }
        }
    }
}

kotlin {
    configAsKobwebApplication("kobweb-site")

    @Suppress("UNUSED_VARIABLE") // sourceSets need to be defined for their property name
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.kobweb.core)
                implementation(libs.kobweb.silk.core)
                implementation(libs.kobweb.silk.icons.fa)
                implementation(libs.kobwebx.markdown)
             }
        }
    }
}

class MarkdownVisitor : AbstractVisitor() {
    private val _frontMatter = mutableMapOf<String, List<String>>()
    val frontMatter: Map<String, List<String>> = _frontMatter

    override fun visit(customBlock: CustomBlock) {
        if (customBlock is YamlFrontMatterBlock) {
            val yamlVisitor = YamlFrontMatterVisitor()
            customBlock.accept(yamlVisitor)
            _frontMatter.putAll(yamlVisitor.data)
        }
    }
}

class Article(val slug: String)
class Subcategory(
    val title: String,
    vararg val articles: Article
) {
    constructor(vararg articles: Article) : this("", *articles)
}

class Category(
    val slug: String,
    val title: String,
    vararg val subcategories: Subcategory
) {
    constructor(slug: String, vararg subcategories: Subcategory) :
            this(slug, slug.capitalized(), *subcategories)
}

private val SITE_LISTING = buildList {
    add(
        Category(
            "guides",
            Subcategory(
                "Getting Started",
                Article("gettingkobweb")
            ),
        )
    )

    add(
        Category(
            "widgets",
            Subcategory(
                "Forms",
                Article("button"),
            ),
            Subcategory(
                "Overlay",
                Article("tooltip"),
            ),
        )
    )

    add(
        Category(
            "tutorials",
            Subcategory(
                Article("createfirstsite")
            ),
        )
    )
}

class ArticleMetadata(val title: String)
class ArticleEntry(val slug: String, val metadata: ArticleMetadata)

fun List<Category>.find(categorySlug: String, articleSlug: String): Article? {
    return this.asSequence()
        .filter { it.slug == categorySlug }
        .flatMap { it.subcategories.asSequence() }
        .flatMap { it.articles.asSequence() }
        .firstOrNull { it.slug == articleSlug }
}

val generateSiteListingTask = task("generateSiteListing") {
    group = "kobweb site"
    val LISTINGS_INPUT_DIR = "src/jsMain/resources/markdown/docs"
    val LISTINGS_OUTPUT_FILE = "src/jsMain/kotlin/com/varabyte/kobweb/site/model/listing/SiteListing.kt"

    val discoveredMetadata = mutableMapOf<String, MutableList<ArticleEntry>>()

    inputs.dir(LISTINGS_INPUT_DIR)
    outputs.file(layout.projectDirectory.file(LISTINGS_OUTPUT_FILE))

    doLast {
        val parser = kobweb.markdown.features.createParser()
        val root = file(LISTINGS_INPUT_DIR)
        fileTree(root).forEach { mdArticle ->
            val rootNode = parser.parse(mdArticle.readText())
            val visitor = MarkdownVisitor()

            rootNode.accept(visitor)

            val fm = visitor.frontMatter
            val requiredFields = listOf("title")
            val (title) = requiredFields
                .map { key -> fm[key]?.singleOrNull() }
                .takeIf { values -> values.all { it != null } }
                ?.requireNoNulls()
                ?: run {
                    println("Skipping $mdArticle in the listing as it is missing required frontmatter fields (one of $requiredFields)")
                    return@forEach
                }

            val categorySlug = mdArticle.parentFile.name
            discoveredMetadata.getOrPut(categorySlug) { mutableListOf() }
                .add(
                    ArticleEntry(
                        mdArticle.nameWithoutExtension.lowercase(),
                        ArticleMetadata(title)
                    ).also { entry ->
                        if (SITE_LISTING.find(categorySlug, entry.slug) == null) {
                            throw GradleException(
                                "$mdArticle needs an entry (slug: \"${entry.slug}\") in `SITE_LISTING`."
                            )
                        }
                    }
                )
        }

        project.layout.projectDirectory.file(LISTINGS_OUTPUT_FILE).asFile.let { siteListing ->
            val indent = "   "

            siteListing.parentFile.mkdirs()
            siteListing.writeText(buildString {
                appendLine(
                    """
                    package com.varabyte.kobweb.site.model.listing

                    // DO NOT EDIT THIS FILE BY HAND! IT GETS AUTO-GENERATED BY `./gradlew $name`

                    val SITE_LISTING = buildList {
                    """.trimIndent()
                )

                SITE_LISTING.forEach { category ->
                    appendLine("${indent}add(")
                    appendLine("${indent}${indent}Category(")
                    appendLine("${indent}${indent}${indent}\"${category.slug}\",")
                    appendLine("${indent}${indent}${indent}\"${category.title}\",")
                    category.subcategories.forEach { subcategory ->
                        appendLine("${indent}${indent}${indent}Subcategory(")
                        appendLine("${indent}${indent}${indent}${indent}\"${subcategory.title}\",")
                        subcategory.articles.forEach { article ->
                            val metadata = discoveredMetadata.getValue(category.slug).first { it.slug == article.slug }.metadata
                            appendLine("${indent}${indent}${indent}${indent}Article(\"${article.slug}\", \"${metadata.title}\"),")
                        }
                        appendLine("${indent}${indent}${indent}),")
                    }
                    appendLine("${indent}${indent})")
                    appendLine("${indent})")
                }

                appendLine(
                    """
                    }

                    fun List<Category>.findArticle(categorySlug: String, articleSlug: String): Article? {
                       return this.asSequence()
                          .filter { it.slug == categorySlug }
                          .flatMap { it.subcategories.asSequence() }
                          .flatMap { it.articles.asSequence() }
                          .firstOrNull { it.slug == articleSlug }
                    }
                    """.trimIndent()
                )
            })

            println("Generated ${siteListing.absolutePath}")
        }

        SITE_LISTING.forEach { category ->
            category.subcategories.forEach { subcategory ->
                subcategory.articles.forEach { article ->
                    if (discoveredMetadata[category.slug]?.any { entry -> entry.slug == article.slug } != true) {
                        throw GradleException(
                            "`SITE_LISTING` contains entry for \"${category.slug}/${article.slug}\" but no found article satisfies it."
                        )
                    }
                }
            }
        }
    }
}.also { notifyKobwebAboutFrontendCodeGeneratingTask(it) }

