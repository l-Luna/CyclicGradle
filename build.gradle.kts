plugins {
	`java-gradle-plugin`
	`maven-publish`
}

group = "cyclic.lang"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
	useJUnitPlatform()
}

gradlePlugin {
	// Define the plugin
	val cyclic by plugins.creating {
		id = "cyclic.lang"
		implementationClass = "cyclic.gradle.CyclicPlugin"
	}
}