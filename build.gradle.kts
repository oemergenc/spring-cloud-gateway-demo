import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("com.diffplug.spotless") version "7.0.0"
}

val springBootVersion = "3.4.3"
val grpcBomVersion = "1.70.0"
val springCloudDependenciesVersion = "2024.0.0"
val springCloudGcpDependenciesVersion = "6.1.0"
val testContainersVersion = "1.20.4"

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

configure<SpotlessExtension> {
    java {
        googleJavaFormat()
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-starter-parent:$springBootVersion"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudDependenciesVersion"))

    // Server
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Logging
    implementation("io.micrometer:context-propagation")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.projectreactor:reactor-core-micrometer:1.2.3")


    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.mock-server:mockserver-spring-test-listener:5.15.0")
    testImplementation("org.wiremock:wiremock-standalone:3.0.1")
    testImplementation("net.javacrumbs.json-unit:json-unit-fluent:2.38.0")


}

tasks.withType<Test> {
    useJUnitPlatform()
}
