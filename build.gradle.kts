plugins {
    java
    kotlin("jvm") version "1.4.32"
    application
    antlr
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "obdd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation("org.antlr:antlr4:4.8")
    antlr("org.antlr:antlr4:4.8")
}

sourceSets.getByName("main").java {
    srcDir("build/generated-src/main/java")
}

application {
    mainClassName = "obdd.MainKt"
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages")
    outputDirectory = file("build/generated-src/main/java/obdd/gen")
}

tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}