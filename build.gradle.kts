import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

jacoco {
    toolVersion = "0.8.14"
}

val coverageModules = listOf(
    ":app", ":core:common", ":core:ui", ":core:network", ":core:database",
    ":feature:auth", ":feature:events", ":feature:cart", ":feature:payment", ":feature:ticket", ":feature:profile",
)

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    source.setFrom(coverageModules.map { project(it).file("src") })
    ignoreFailures = false
}
val generatedClasses = listOf(
    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest.class",
    "**/Manifest\$*.class", "**/AppDatabase_Impl*.class", "**/CartDao_Impl*.class",
    "**/TicketDao_Impl*.class", "**/IssuedTicketDao_Impl*.class",
    "**/ComposableSingletons\$*.class", "**/*\$serializer.class",
)

val allCoverageClasses = files()
val allCoverageSources = files()
val allCoverageData = files()
val coverageClassesByModule = mutableMapOf<String, FileCollection>()
val coverageDataByModule = mutableMapOf<String, FileTree>()

configure(subprojects.filter { it.path in coverageModules }) {
    apply(plugin = "jacoco")
    extensions.configure<JacocoPluginExtension> { toolVersion = "0.8.14" }
    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    val classes = files(
        fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            exclude(generatedClasses)
        },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
            exclude(generatedClasses)
        },
    )
    val sources = files("src/main/java", "src/main/kotlin")
    val data = fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")) {
        include("testDebugUnitTest.exec")
    }
    allCoverageClasses.from(classes)
    allCoverageSources.from(sources)
    allCoverageData.from(data)
    coverageClassesByModule[path] = classes
    coverageDataByModule[path] = data

    tasks.register<JacocoReport>("jacocoDebugReport") {
        group = "verification"
        description = "Reports debug unit-test line coverage for $path."
        dependsOn(":verifyCoverageInputs")
        classDirectories.setFrom(classes)
        sourceDirectories.setFrom(sources)
        executionData.setFrom(allCoverageData)
        reports {
            xml.required = true
            html.required = true
        }
    }
}

val verifyCoverageInputs = tasks.register("verifyCoverageInputs") {
    group = "verification"
    description = "Requires compiled production classes and unit-test execution data."
    dependsOn(coverageModules.map { "$it:testDebugUnitTest" })
    val classesToCheck = coverageClassesByModule.toMap()
    val dataToCheck = coverageDataByModule.toMap()
    val modulesWithTests = setOf(
        ":app", ":core:common", ":core:ui",
        ":feature:auth", ":feature:events", ":feature:cart", ":feature:payment", ":feature:ticket", ":feature:profile",
    )
    inputs.files(allCoverageClasses, allCoverageData)
    doLast {
        classesToCheck.forEach { (module, classes) ->
            check(classes.files.isNotEmpty()) { "Nenhuma classe de produção encontrada em $module." }
        }
        modulesWithTests.forEach { module ->
            check(dataToCheck.getValue(module).files.any { it.length() > 0 }) {
                "Nenhum dado de execução JaCoCo encontrado em $module."
            }
        }
    }
}

tasks.register<JacocoReport>("jacocoDebugReport") {
    group = "verification"
    description = "Reports aggregated debug unit-test coverage."
    dependsOn(verifyCoverageInputs)
    classDirectories.setFrom(allCoverageClasses)
    sourceDirectories.setFrom(allCoverageSources)
    executionData.setFrom(allCoverageData)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.register<JacocoCoverageVerification>("jacocoDebugCoverageVerification") {
    group = "verification"
    description = "Verifies at least 80% aggregated debug line coverage."
    dependsOn("jacocoDebugReport")
    classDirectories.setFrom(allCoverageClasses)
    sourceDirectories.setFrom(allCoverageSources)
    executionData.setFrom(allCoverageData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }

        }
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs static analysis, tests, coverage verification and debug/release builds."
    dependsOn("detekt", "jacocoDebugCoverageVerification", ":app:assembleDebug", ":app:assembleRelease")
}
