import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.wecovi"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        webstorm("2024.1.7")
        bundledPlugin("JavaScript")
        testFramework(TestFrameworkType.Platform)
    }
}

configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
}

kotlin {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val installUi by tasks.registering(Exec::class) {
    workingDir("ui")
    commandLine("pnpm", "install", "--frozen-lockfile")
    inputs.file("ui/package.json")
    inputs.file("ui/pnpm-lock.yaml")
    outputs.dir("ui/node_modules")
}

val buildUi by tasks.registering(Exec::class) {
    dependsOn(installUi)
    workingDir("ui")
    commandLine("pnpm", "build")
    inputs.dir("ui/src")
    inputs.file("ui/index.html")
    inputs.file("ui/vite.config.ts")
    outputs.dir("ui/dist")
}

tasks.processResources {
    dependsOn(buildUi)
    from("ui/dist") { into("wecovi/ui") }
}

tasks.test {
    systemProperty("idea.load.plugins.id", "JavaScript")
    systemProperty("wecovi.projectDir", projectDir.absolutePath)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
        }
    }
}
