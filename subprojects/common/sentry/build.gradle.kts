plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    }

dependencies {
    api(libs.sentry)

    implementation(libs.kotlinStdlib)
}
