plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
}

dependencies {
    implementation(gradleApi())
    implementation(projects.gradle.gradleExtensions)
}
