plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-gradle-plugin")
    id("convention.gradle-testing")
    }

dependencies {
    compileOnly(gradleApi())

    implementation(project(":gradle:gradle-logger"))
    implementation(project(":common:math"))
    implementation(libs.kotlinStdlib)

    gradleTestImplementation(project(":common:truth-extensions"))
    gradleTestImplementation(testFixtures(project(":common:logger")))
    gradleTestImplementation(project(":gradle:test-project"))
}

kotlin {
    explicitApi()
}

gradlePlugin {
    plugins {
        create("moduleDependenciesGraph") {
            id = "com.avito.android.module-dependencies-graph"
            implementationClass = "com.avito.module.dependencies.ModuleDependenciesGraphPlugin"
            displayName = "Module dependencies graph"
            description = "Build module dependencies graph"
        }
    }
}
