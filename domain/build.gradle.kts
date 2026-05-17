import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
    }
}

android {
    namespace = "ltd.evilcorp.domain"
    compileSdk = libs.versions.sdk.target.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Чтение SDK / NDK / CMake
// ─────────────────────────────────────────────────────────────────────────────

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

val sdkDir: String = localProperties.getProperty("sdk.dir")
    ?: System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: ""

val ndkDir: String = run {
    val explicit = localProperties.getProperty("ndk.dir")
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_ROOT")
        ?: ""
    if (explicit.isNotEmpty()) return@run explicit

    if (sdkDir.isEmpty()) return@run ""
    val ndkBase = file("$sdkDir/ndk")
    if (!ndkBase.exists()) return@run ""

    val expected = file("${ndkBase.absolutePath}/${libs.versions.ndk.get()}")
    if (expected.exists()) expected.absolutePath
    else ndkBase.listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?.absolutePath ?: ""
}

val cmakeExecutable: String = run {
    val cmakeBase = file("$sdkDir/cmake")
    cmakeBase.listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?.let { file("${it.absolutePath}/bin/cmake").absolutePath }
        ?: "cmake"
}

// ─────────────────────────────────────────────────────────────────────────────
// Вспомогательные функции (вне таска — доступны везде)
// ─────────────────────────────────────────────────────────────────────────────

fun findMsysBash(): File? {
    val msys2Root = System.getenv("MSYS2_ROOT")

    val candidates = listOfNotNull(
        "C:/msys64/usr/bin/bash.exe",
        "C:/msys64/ucrt64/bin/bash.exe",
        "C:/msys64/mingw64/bin/bash.exe",
        "C:/msys2/usr/bin/bash.exe",
        "C:/msys2/ucrt64/bin/bash.exe",
        msys2Root?.let { "$it/usr/bin/bash.exe" },
        msys2Root?.let { "$it/ucrt64/bin/bash.exe" }
    ).map { file(it) }

    candidates.firstOrNull { it.exists() }?.let { return it }

    // Fallback: ищем bash в системном PATH
    return System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.map { file("$it/bash.exe") }
        ?.firstOrNull { it.exists() }
}

fun installMsys2ViaWinget() {
    logger.lifecycle("[aTox] MSYS2 не найден — устанавливаем через winget...")
    val process = ProcessBuilder(
        "cmd.exe", "/c",
        "winget install -e --id MSYS2.MSYS2 --silent --accept-package-agreements --accept-source-agreements"
    ).inheritIO().start()

    if (process.waitFor() != 0) {
        throw GradleException(
            """
            [aTox Build Error] winget не смог установить MSYS2.
            Установите вручную: winget install MSYS2.MSYS2
            Или скачайте с https://www.msys2.org/
            После установки добавьте в PATH:
              C:\msys64\usr\bin
              C:\msys64\ucrt64\bin
            """.trimIndent()
        )
    }

    logger.lifecycle("[aTox] MSYS2 установлен. Ждём записи файлов на диск...")
    Thread.sleep(6000)
}

fun ensurePackages(bash: File) {
    val required = listOf("make", "curl", "tar", "patch", "ca-certificates")

    val missing = required.filter { pkg ->
        val probe = ProcessBuilder(bash.absolutePath, "-c", "pacman -Q $pkg")
            .redirectErrorStream(true)
            .start()
        probe.inputStream.copyTo(System.out)  // PrintStream напрямую, без .outputStream()
        probe.waitFor() != 0
    }

    if (missing.isEmpty()) {
        logger.lifecycle("[aTox] Все пакеты MSYS2 уже установлены — пропускаем.")
        return
    }

    logger.lifecycle("[aTox] Устанавливаем отсутствующие пакеты: ${missing.joinToString(", ")}")

    val install = ProcessBuilder(
        bash.absolutePath, "-c",
        "pacman -Sy --noconfirm ${missing.joinToString(" ")}"
    ).inheritIO().start()

    if (install.waitFor() != 0) {
        throw GradleException(
            """
            [aTox Build Error] pacman не смог установить: ${missing.joinToString(", ")}
            Выполните вручную в терминале MSYS2:
              pacman -Sy ${missing.joinToString(" ")}
            """.trimIndent()
        )
    }
    logger.lifecycle("[aTox] Пакеты успешно установлены.")
}

// ─────────────────────────────────────────────────────────────────────────────
// Таск сборки — теперь DefaultTask + ProcessBuilder, без Exec
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("buildNativeDependencies") {
    group = "build"
    description = "Сборка нативных зависимостей Tox (libsodium, opus, vpx, toxcore) для aarch64"

    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    val parallelism = Runtime.getRuntime().availableProcessors()

    // Пропускаем если все .a уже собраны
    onlyIf {
        val installLib = file("${rootDir}/_install/aarch64-linux-android/lib")
        val needed = listOf("libtoxcore.a", "libsodium.a", "libopus.a", "libvpx.a")
        val alreadyBuilt = needed.all { file("${installLib.absolutePath}/$it").exists() }
        if (alreadyBuilt) {
            logger.lifecycle("[aTox] Все prebuilt-библиотеки найдены в _install/ — пропускаем сборку.")
        }
        !alreadyBuilt
    }

    doLast {
        // Общее окружение для ProcessBuilder
        fun buildEnv() = mapOf(
            "ANDROID_NDK_HOME" to ndkDir.replace("\\", "/"),
            "NDK_HOME"         to ndkDir.replace("\\", "/"), // ← добавить эту строку
            "CMAKE"            to cmakeExecutable.replace("\\", "/"),
            "LIBSODIUM_VERSION" to libs.versions.libsodium.get(),
            "OPUS_VERSION"      to libs.versions.opus.get(),
            "LIBVPX_VERSION"    to libs.versions.libvpx.get(),
            "TOXCORE_VERSION"   to libs.versions.toxcore.get()
        )

        fun runProcess(command: List<String>, env: Map<String, String>) {
            val pb = ProcessBuilder(command)
                .directory(rootDir)
                .redirectErrorStream(true) // stderr → stdout
            pb.environment().putAll(env)

            val process = pb.start()

            // Читаем вывод построчно и пишем в Gradle-лог
            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    logger.lifecycle("[aTox] $line")
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException(
                    "[aTox Build Error] Процесс завершился с кодом $exitCode.\nКоманда: ${command.joinToString(" ")}"
                )
            }
        }

        if (isWindows) {
            // ── Шаг 1: найти bash ────────────────────────────────────────
            var bash = findMsysBash()

            // ── Шаг 2: bash не найден — ставим MSYS2 через winget ───────
            if (bash == null) {
                installMsys2ViaWinget()
                bash = findMsysBash()
            }

            // ── Шаг 3: после установки всё ещё нет — стоп ───────────────
            if (bash == null || !bash.exists()) {
                throw GradleException(
                    """
                    [aTox Build Error] bash.exe не найден даже после установки MSYS2.
                    Убедитесь, что в PATH добавлены:
                      C:\msys64\usr\bin
                      C:\msys64\ucrt64\bin
                    """.trimIndent()
                )
            }

            logger.lifecycle("[aTox] bash найден: ${bash.absolutePath}")

            // ── Шаг 4: проверяем и доустанавливаем пакеты ───────────────
            ensurePackages(bash)

            // ── Шаг 5: диагностический лог окружения ────────────────────
            logger.lifecycle(
                """
                [aTox] Окружение сборки:
                  bash  = ${bash.absolutePath}
                  NDK   = $ndkDir
                  CMake = $cmakeExecutable
                  SDK   = $sdkDir
                """.trimIndent()
            )

            // ── Шаг 6: запускаем make через bash ────────────────────────
            // PATH уже содержит C:\msys64\usr\bin и C:\msys64\ucrt64\bin,
            // поэтому --login не нужен
            runProcess(
                listOf(
                    bash.absolutePath,
                    "-c",
                    "make -f scripts/build-aarch64-linux-android -j$parallelism release"
                ),
                buildEnv()
            )

        } else {
            // ── Linux / macOS ────────────────────────────────────────────
            val script = file("${rootDir}/scripts/build-aarch64-linux-android")
            script.setExecutable(true)

            runProcess(
                listOf(script.absolutePath, "-j$parallelism", "release"),
                buildEnv()
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn("buildNativeDependencies")
}

// ─────────────────────────────────────────────────────────────────────────────
// Зависимости модуля
// ─────────────────────────────────────────────────────────────────────────────

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.kotlinx.coroutines.test) {
        exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-debug")
    }

    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
}