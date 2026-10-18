package com.livingrelation.markdownnotes.core.markdown

import java.io.File

object FixtureNotes {
    private val fixturesRoot: File by lazy {
        System.getProperty("markdown.fixtures.root")?.let { File(it) }
            ?: File(checkNotNull(System.getProperty("user.dir"))).resolve("../fixtures/notes")
    }

    fun load(name: String): String = fixturesRoot.resolve(name).readText()

    fun allMarkdownFiles(): List<String> =
        fixturesRoot
            .listFiles { file -> file.isFile && file.extension == "md" }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
}
