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
    implementation("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63")
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}