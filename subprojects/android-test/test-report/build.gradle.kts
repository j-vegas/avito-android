plugins {
    id("convention.kotlin-android-library")
    id("convention.publish-android-library")
    }

dependencies {
    implementation(project(":common:okhttp"))
    implementation(project(":common:http-client"))
    implementation(project(":common:time"))
    implementation(project(":common:file-storage"))
    implementation(project(":common:report-viewer"))
    implementation(project(":common:logger"))
    implementation(project(":common:elastic-logger"))
    implementation(project(":common:sentry"))
    implementation(project(":common:result"))
    implementation(project(":common:test-annotations"))
    implementation(project(":common:throwable-utils"))
    implementation(project(":android-test:android-log"))
    implementation(project(":android-test:ui-testing-core"))
    implementation(project(":android-test:resource-manager-exceptions"))
    implementation(project(":android-test:websocket-reporter"))
    implementation(libs.kotlinStdlib)
    implementation(libs.okio)
    implementation(libs.kotson)
    implementation(libs.okhttp)
    implementation(libs.radiography)

    testImplementation(libs.jsonPathAssert)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.okhttpMock)
    testImplementation(project(":common:junit-utils"))
    testImplementation(project(":common:resources"))
    testImplementation(project(":common:truth-extensions"))
    testImplementation(testFixtures(project(":common:logger")))
    testImplementation(testFixtures(project(":common:time")))
    testImplementation(testFixtures(project(":common:http-client")))
}
