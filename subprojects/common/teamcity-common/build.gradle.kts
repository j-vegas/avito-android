plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    }

dependencies {
    api(libs.teamcityClient)

    implementation(libs.kotlinStdlib)
}
