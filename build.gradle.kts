import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    kotlin("jvm") version "1.9.0"
    id("org.openapi.generator") version "6.5.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("groovy")
}

group = "com.vymalo.keycloak.nkobani.users"
version = "0.3.0"

val gsonVersion = "2.10.1"
val amqpVersion = "5.17.0"
val okhttp3Version = "4.10.0"
val feignVersion = "12.4"
val okioVersion = "3.2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    api("org.apache.commons", "commons-lang3", "3.12.0")

    implementation(kotlin("stdlib"))

    implementation("org.keycloak", "keycloak-services", "22.0.1")
    implementation("org.keycloak", "keycloak-server-spi", "22.0.1")
    implementation("org.keycloak", "keycloak-server-spi-private", "22.0.1")
    implementation("org.keycloak", "keycloak-model-legacy", "22.0.1")

    api("io.github.openfeign", "feign-core", feignVersion)
    api("io.github.openfeign", "feign-okhttp", feignVersion)
    api("io.github.openfeign", "feign-slf4j", feignVersion)
    api("io.github.openfeign", "feign-jackson", feignVersion)
    api("io.github.openfeign", "feign-gson", feignVersion)
    api("io.github.openfeign.form", "feign-form", "3.8.0")
    api("com.github.scribejava", "scribejava-core", "8.3.1")

    api("org.openapitools", "jackson-databind-nullable", "0.2.6")
    api("com.squareup.okhttp3", "okhttp", okhttp3Version)
    api("com.google.code.gson", "gson", gsonVersion)
    api("org.slf4j", "slf4j-log4j12", "1.7.36")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

val generatedSourcesDir = "$buildDir/generated/openapi"
val openapiPackageName = "$group.openapi.client"

openApiGenerate {
    generatorName.set("java")

    inputSpec.set("$rootDir/openapi/users.open-api.json")
    outputDir.set(generatedSourcesDir)
    library.set("feign")

    packageName.set(openapiPackageName)
    apiPackage.set("$openapiPackageName.handler")
    invokerPackage.set("$openapiPackageName.invoker")
    modelPackage.set("$openapiPackageName.model")

    httpUserAgent.set("Keycloak/Kotlin")

    typeMappings.set(mapOf("OffsetDateTime" to "Date"))
    importMappings.set(mapOf("java.time.OffsetDateTime" to "java.util.Date"))

    configOptions.set(
        mutableMapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "gson"
        )
    )
}

sourceSets {
    getByName("main") {
        kotlin {
            srcDir("$generatedSourcesDir/src/main/kotlin")
        }
        java {
            srcDir("$generatedSourcesDir/src/main/java")
        }
    }
}

tasks {
    val openApiGenerate by getting

    val compileKotlin by getting {
        dependsOn(openApiGenerate)
    }

    val shadowJar by existing(ShadowJar::class) {
        dependencies {
            include(dependency("io.github.openfeign:.*"))
            include(dependency("io.github.openfeign.form:.*"))
            include(dependency("com.squareup.okhttp3:.*"))
            include(dependency("com.squareup.okio:okio-jvm:.*"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
            include(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            include(dependency("com.google.code.gson:gson:.*"))
        }
        dependsOn(build)
    }
}
