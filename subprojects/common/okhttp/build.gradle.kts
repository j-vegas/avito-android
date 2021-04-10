plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    }

dependencies {
    api(libs.okhttp)

    implementation(project(":common:logger"))
    implementation(libs.kotlinStdlib)
    implementation(libs.okhttpLogging)

    testImplementation(project(":common:truth-extensions"))
    testImplementation(project(":common:test-okhttp"))
    testImplementation(project(":common:result"))
    testImplementation(testFixtures(project(":common:logger")))
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofitConverterGson)
}

kotlin {
    explicitApi()
}
