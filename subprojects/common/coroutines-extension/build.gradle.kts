plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    }

dependencies {
    api(libs.coroutinesCore)

    implementation(libs.kotlinStdlib)
}
