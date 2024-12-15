plugins {
	application
	id("checkstyle")
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "org.shvedchikov"
version = "0.0.1-SNAPSHOT"

application { mainClass.set("org.shvedchikov.domidzebot.DomidzeBotApplication") }

//java {
//	toolchain {
//		languageVersion = JavaLanguageVersion.of(19)
//	}
//}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.2")
	implementation("org.springframework.boot:spring-boot-starter:3.3.2")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.3.2")
	implementation("org.springframework.boot:spring-boot-starter-validation:3.2.4")
	implementation("org.postgresql:postgresql:42.7.3")
	compileOnly("org.projectlombok:lombok:1.18.34")
	annotationProcessor("org.projectlombok:lombok:1.18.34")
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
