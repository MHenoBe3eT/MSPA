import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}
apply(plugin = "kotlin-jpa")

java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    implementation(project(":domain"))
    implementation(project(":useCase"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("io.minio:minio:8.5.12")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}