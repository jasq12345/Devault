plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "dev.devault"
version = "0.0.1-SNAPSHOT"
description = "auth-service-lib"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.nimbusds:nimbus-jose-jwt:10.9")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(project(":common-lib"))
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.14.9")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Library module: no main class, publish a plain jar instead of a Boot fat jar
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}
