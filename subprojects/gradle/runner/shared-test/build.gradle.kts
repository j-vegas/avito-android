plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    }

publish {
    artifactId.set("runner-shared-test")
}

dependencies {
    api(project(":gradle:runner:stub"))
    api(libs.coroutinesTest)

    compileOnly(gradleApi())

    implementation(libs.coroutinesCore)
    implementation(libs.kotson)
    implementation(libs.kotlinStdlib)
    implementation(project(":common:report-viewer"))
    implementation(project(":gradle:runner:service"))
    implementation(project(":gradle:runner:shared"))
    implementation(project(":gradle:test-project"))
}
