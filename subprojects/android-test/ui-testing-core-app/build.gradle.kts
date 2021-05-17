import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.avito.instrumentation.reservation.request.Device.CloudEmulator
import com.avito.kotlin.dsl.getOptionalStringProperty
import com.avito.utils.gradle.KubernetesCredentials
import com.avito.utils.gradle.KubernetesCredentials.Service
import com.avito.utils.gradle.kubernetesCredentials

plugins {
    id("convention.kotlin-android-app")
    id("com.avito.android.instrumentation-tests")
    id("convention.libraries")
}

android {

    defaultConfig {
        versionName = "1.0"
        versionCode = 1
        testInstrumentationRunner = "com.avito.android.test.app.core.TestAppRunner"

        // TODO: describe in docs that they are updated in IDE configuration only after sync!
        testInstrumentationRunnerArguments(
            mapOf(
                "planSlug" to "AndroidTestApp"
            )
        )
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    packagingOptions {
        pickFirst("META-INF/okhttp.kotlin_module")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.playServicesMaps)
    implementation(libs.recyclerView)

    implementation(project(":android-lib:proxy-toast"))

    androidTestImplementation(project(":test-runner:test-inhouse-runner"))
    androidTestImplementation(project(":test-runner:test-report"))
    androidTestImplementation(project(":test-runner:test-annotations"))
    androidTestImplementation(project(":common:report-viewer")) {
        because("Priority/Behavior test annotations still there")
    }

    androidTestImplementation(project(":android-test:ui-testing-core"))
    androidTestImplementation(project(":android-test:toast-rule"))
    androidTestImplementation(project(":common:truth-extensions"))

    androidTestUtil(libs.testOrchestrator)
}

val avitoRegistry = getOptionalStringProperty("avito.registry")

instrumentation {
    experimental {
        useInMemoryReport.set(true)
    }

    sentryDsn = getOptionalStringProperty("avito.instrumentaion.sentry.dsn") ?: "http://stub-project@stub-host/0"

    logcatTags = setOf(
        "UITestRunner:*",
        "ActivityManager:*",
        "ReportTestListener:*",
        "StorageJsonTransport:*",
        "TestReport:*",
        "VideoCaptureListener:*",
        "TestRunner:*",
        "SystemDialogsManager:*",
        "AndroidJUnitRunner:*",
        "ito.android.de:*", // по этому тэгу система пишет логи об использовании hidden/restricted api https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces
        "*:E"
    )

    instrumentationParams = mapOf(
        "videoRecording" to "all",
        "jobSlug" to "FunctionalTests"
    )

    filters.register("ci") {
        fromSource.excludeFlaky = true
    }

    val credentials = project.kubernetesCredentials
    if (credentials is Service || credentials is KubernetesCredentials.Config) {

        afterEvaluate {
            tasks.named("check").dependsOn(tasks.named("instrumentationUi"))
        }

        val emulator22 = CloudEmulator(
            name = "api22",
            api = 22,
            model = "Android_SDK_built_for_x86",
            image = emulatorImage(22, "740eb9a948"),
            cpuCoresRequest = "1",
            cpuCoresLimit = "1.3",
            memoryLimit = "4Gi"
        )

        configurationsContainer.register("ui") {
            reportSkippedTests = true
            filter = "ci"

            targetsContainer.register("api22") {
                deviceName = "API22"

                scheduling {
                    quota {
                        retryCount = 1
                        minimumSuccessCount = 1
                    }

                    testsCountBasedReservation {
                        device = emulator22
                        maximum = 5
                        minimum = 2
                        testsPerEmulator = 1
                    }
                }
            }
        }
    }
}

fun emulatorImage(api: Int, label: String): String {
    return if (avitoRegistry != null) {
        "$avitoRegistry/android/emulator-$api:$label"
    } else {
        "avitotech/android-emulator-$api:$label"
    }
}
