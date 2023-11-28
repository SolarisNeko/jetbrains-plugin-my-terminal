// 项目依赖的插件，默认会依赖kotlin，但我们这里是直接用java来开发插件的，所以这里依赖我们可以去掉
val kotlinVersion = "1.9.0"

plugins {
    val kotlinVersion = "1.9.0"
    id("java")
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.intellij") version "1.15.0"
}

// 插件的一些基本信息，按实际情况填就行，不是很重要
group = "com.neko233"
version = "1.0.1"

// 插件等依赖的下载地址，默认会去中央仓库下载，这里我们一般是会改为直接去idea官网下载或者是用其他镜像
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.neko233:skilltree-commons-core:0.2.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    // 使用 kotlin.test 进行单元测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.0")
}


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html

// 这里是很重要的配置，定义了gradle构建时依赖的idea版本，我们进行插件调试的时候，会使用这里定义的idea版本来进行测试的。
intellij {
    version.set("2022.2.5")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "com.intellij.java",
        )
    )
}

sourceSets {
    main {
        java.srcDirs("src/main/java") // 确保这里包含了 Java 源文件目录
        kotlin.srcDirs("src/main/kotlin") // 确保这里包含了 Kotlin 源文件目录

    }

    test {
        java.srcDirs("src/test/java")
        kotlin.srcDirs("src/test/kotlin")
    }
}

// 定义构建的任务，主要是改一下编译的jdk版本，插件适用的idea版本等信息
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
        options.isFailOnError = false // 设置是否在编译错误时抛出异常（可选）
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
