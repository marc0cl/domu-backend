plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.domu"
version = "0.1.0"

repositories {
    mavenCentral()
}

val javaVersion = JavaLanguageVersion.of(21)

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

dependencies {
    implementation(libs.javalin)
    implementation(libs.slf4j)
    implementation(libs.log4jApi)
    implementation(libs.log4jCore)
    implementation(libs.log4jSlf4j)
    implementation(libs.hikaricp)
    implementation(libs.mysqlConnector)
    implementation(libs.javaJwt)
    implementation(libs.jbcrypt)
    implementation(libs.jacksonJsr310)
    implementation(libs.jakartaValidation)
    implementation(libs.jakartaPersistence)
    implementation(libs.guice)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junitJupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.javalinTesttools)
}

application {
    mainClass.set("com.domu.backend.Main")
    applicationDefaultJvmArgs = listOf(
        "-Duser.timezone=UTC",
        "-Dfile.encoding=UTF-8"
    )
}

tasks.test {
    useJUnitPlatform()
    systemProperty("javalin.port", "0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.asInt())
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("shadowJar") {
    dependsOn("test")
}
