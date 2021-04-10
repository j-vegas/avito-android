plugins {
    id("convention.kotlin-android-library")
    id("convention.publish-android-library")
    }

dependencies {
    api(project(":android-test:test-instrumentation-runner"))
    api(project(":common:junit-utils"))
    api(project(":android-test:test-report"))
    api(libs.sentry) {
        because("InHouseInstrumentationTestRunner.sentry")
    }

    implementation(project(":common:sentry"))
    implementation(project(":common:elastic-logger"))
    implementation(project(":common:http-client"))
    implementation(project(":common:okhttp"))
    implementation(project(":common:statsd"))
    implementation(project(":common:report-viewer"))
    implementation(project(":common:logger"))
    implementation(project(":common:junit-utils"))
    implementation(project(":common:test-okhttp"))
    implementation(project(":common:test-annotations"))
    implementation(project(":common:file-storage"))
    implementation(project(":common:time"))
    implementation(project(":android-test:android-log"))
    implementation(project(":android-test:ui-testing-core"))
    implementation(project(":android-test:ui-testing-maps"))
    implementation(libs.kotlinStdlib)
    implementation(libs.androidXTestRunner)
    implementation(libs.truth)
    implementation(libs.mockitoKotlin)
    implementation(libs.okhttpLogging)
    implementation(libs.okhttpMockWebServer)
    implementation(libs.gson)

    testImplementation(libs.kotlinPoet)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(project(":common:truth-extensions"))
}
