plugins {
    kotlin("plugin.spring")
    id("org.openapi.generator") version "7.8.0"
}
apply(plugin = "kotlin")
apply(plugin = "kotlin-spring")
apply(plugin = "kotlin-jpa")

dependencies {
    implementation(project(":domain"))
    implementation(project(":useCase"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-dependencies:3.1.2")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.1.2")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

openApiGenerate {
    generatorName = "kotlin-spring"
    inputSpec = "$rootDir/rest/specs/api-docs.yaml"
    outputDir = "${layout.buildDirectory.get().asFile}/generated"
    apiPackage = "ru.vlasov.api"
    invokerPackage = "ru.vlasov.invoker"
    modelPackage = "ru.vlasov.model"

    configOptions.set(
        mapOf(
            "dateLibrary" to "java17",
            "interfaceOnly" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            "serializableModel" to "true",
            "useBeanValidation" to "true",
            "performBeanValidation" to "true",
            "useJakartaEe" to "true",
            "useSpringBoot3" to "true",
            "useSpringAnnotation" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true"
        )
    )
}

sourceSets {
    main {
        java.srcDir("${layout.buildDirectory.get().asFile}/generated/src/main/kotlin")
    }
}

tasks {
    compileKotlin {
        dependsOn(openApiGenerate)
    }
}
