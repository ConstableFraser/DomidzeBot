plugins {
	application
	id("checkstyle")
	id("jacoco")
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	id("io.freefair.lombok") version "8.6"
}

group = "org.shvedchikov"
version = "0.0.1-SNAPSHOT"

application { mainClass.set("org.shvedchikov.domidzebot.DomidzeBotApplication") }

repositories {
	mavenCentral()
}

jacoco {
	toolVersion = "0.8.12"
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter:3.3.2")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-validation:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-security:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.3.3")
	testImplementation("org.springframework.security:spring-security-test:6.3.3")
	implementation("org.postgresql:postgresql:42.7.7")
	implementation("org.mapstruct:mapstruct:1.6.2")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.1")
	implementation("org.openapitools:jackson-databind-nullable:0.2.6")
	implementation("org.telegram:telegrambots:6.9.7.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.3")
	implementation("org.jsoup:jsoup:1.19.1")
	implementation("org.instancio:instancio-junit:5.0.2")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.7")
	implementation("net.datafaker:datafaker:2.3.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JacocoReport> {
	sourceDirectories.setFrom(files("src/main/java"))
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}
