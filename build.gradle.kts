val kotlinVersion = "2.1.21"
val springBootVersion = "3.4.3"
val springAiVersion = "1.0.0"
val jacksonDataformatXmlVersion = "2.15.2"
val postgresqlVersion = "42.3.8"
val liquibaseCoreVersion = "4.25.0"
val kotestVersion = "5.9.1"

plugins {
    val kotlinVersion = "2.1.21"
    val springVersion = "3.4.3"

    kotlin("jvm") version kotlinVersion
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion apply false
    id("org.springframework.boot") version springVersion apply false
    id("org.openapi.generator") version "7.8.0" apply false
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    application
    `java-library`
}

allprojects {
    group = "ru.mspa"
    version = "0.0.1-SNAPSHOT"

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.plugin.allopen")
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion") {
                bomProperty("kotlin.version", kotlinVersion)
            }
            mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        }
        dependencies {
            dependency("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")

            // kotlin
            dependency("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            dependency("io.github.microutils:kotlin-logging-jvm:3.0.5")

            dependency("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
            dependency("org.springframework.boot:spring-boot-starter-security:${springBootVersion}")
            dependency("org.springframework.boot:spring-boot-starter-websocket:${springBootVersion}")

            dependency("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

            dependency("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")

            dependency("jakarta.inject:jakarta.inject-api:2.0.1")
            dependency("jakarta.annotation:jakarta.annotation-api:3.0.0")
            dependency("jakarta.validation:jakarta.validation-api:3.1.0")
            dependency("jakarta.transaction:jakarta.transaction-api:2.0.1")
            dependency("jakarta.ejb:jakarta.ejb-api:4.0.1")

            dependency("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")

            dependency("org.postgresql:postgresql:${postgresqlVersion}")
            dependency("org.liquibase:liquibase-core:${liquibaseCoreVersion}")
            dependency("io.hypersistence:hypersistence-utils-hibernate-63:3.8.2")

            dependency("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
        }
    }

    dependencies {
        implementation("io.github.microutils:kotlin-logging-jvm")

        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    tasks.test {
        useJUnitPlatform()
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            javaParameters.set(true)
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}
