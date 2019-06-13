import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
  id("org.jetbrains.kotlin.jvm").version("1.3.31")
  id("com.github.johnrengelman.shadow") version "5.0.0"
  // Apply the application plugin to add support for building a CLI application.
  application
}

repositories {
  // Use jcenter for resolving your dependencies.
  // You can declare any Maven/Ivy/file repository here.
  jcenter()
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    suppressWarnings = true
  }
}


val vertx_version = "3.7.0"
val envoyproxyVersion = "0.1.15"

dependencies {

  // Use the Kotlin JDK 8 standard library.
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  implementation("io.vertx:vertx-core:$vertx_version")

//  implementation("io.envoyproxy.controlplane:java-control-plane:$envoyproxyVersion")
  implementation("io.envoyproxy.controlplane:api:$envoyproxyVersion")
  implementation("io.envoyproxy.controlplane:server:$envoyproxyVersion")
  implementation("io.envoyproxy.controlplane:cache:$envoyproxyVersion")


  // was having connection errors with lower version. make sure to match correct tc-native version.
  // need for grpc tls (2.0.20.Final is the supported version according to docs: https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty)
  api("io.grpc:grpc-netty:1.19.0")
  runtime("io.netty:netty-tcnative-boringssl-static:2.0.20.Final") {
    because("need specific version of boring ssl for grpc netty. see comments in build file.")
  }

  testImplementation("io.vertx:vertx-junit5:$vertx_version")

  // Use the Kotlin test library.
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  // Use the Kotlin JUnit integration.
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks {
  test {
    useJUnitPlatform()
  }
}

application {
  // Define the main class for the application.
  mainClassName = "io.vertx.core.Launcher"
}

tasks.withType<ShadowJar> {
  manifest {
    attributes["Main-Verticle"] = "io.bhowell2.envoyxdsserver.Server"
  }
  mergeServiceFiles {
    include("META-INF/services/io.vertx.core.spi.VerticleFactory")
  }
}
