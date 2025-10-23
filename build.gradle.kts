plugins {
    application
}

group = "com.domu"
version = "0.1.0"

repositories {
    mavenCentral()
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

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.junitJupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.domu.backend.Application"
}

tasks.test {
    useJUnitPlatform()
}
