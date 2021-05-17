package com.avito.test.gradle

import com.avito.test.gradle.plugin.plugins
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PluginsSpecTest {


    fun `formatting - plugin with only id`() {
        val plugin = plugins {
            id("plugin-id")
        }

        assertThat(plugin.getScriptRepresentation()).isEqualTo(
            """|plugins {
               |    id "plugin-id"
               |}""".trimMargin()
        )
    }


    fun `formatting - plugin with version`() {
        val plugin = plugins {
            id("plugin-id").version("1.0")
        }

        assertThat(plugin.getScriptRepresentation()).isEqualTo(
            """|plugins {
               |    id "plugin-id" version "1.0"
               |}""".trimMargin()
        )
    }


    fun `formatting - disabled plugin`() {
        val plugin = plugins {
            id("plugin-id").apply(false)
        }

        assertThat(plugin.getScriptRepresentation()).isEqualTo(
            """|plugins {
               |    id "plugin-id" apply false
               |}""".trimMargin()
        )
    }


    fun `formatting - disabled plugin with version`() {
        val plugin = plugins {
            id("plugin-id").version("1.0").apply(false)
        }

        assertThat(plugin.getScriptRepresentation()).isEqualTo(
            """|plugins {
               |    id "plugin-id" version "1.0" apply false
               |}""".trimMargin()
        )
    }


    fun `formatting - multiple specs`() {
        val firstSpec = plugins {
            id("one")
        }
        val secondSpec = plugins {
            id("two")
        }
        val plugins = firstSpec.plus(secondSpec)

        assertThat(plugins.getScriptRepresentation()).isEqualTo(
            """|plugins {
               |    id "one"
               |    id "two"
               |}""".trimMargin()
        )
    }
}
