plugins {
    kotlin("jvm") version "1.9.23" // O la versión que tengas definida
    application
}

group = "com.ctrlcafe.scrapp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/** La consola imprime acentos; sin esto Windows los muestra como "sesi?n". */
val argsConsola = listOf("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

application {
    mainClass.set("com.ctrlcafe.scrapp.MainKt")
    applicationDefaultJvmArgs = argsConsola
}

tasks.named<JavaExec>("run") {
    // Gradle no conecta la entrada estándar a JavaExec por defecto: sin esta línea
    // readLine() devuelve null desde el arranque y el menú se repite sin fin.
    standardInput = System.`in`
    jvmArgs(argsConsola)
}

tasks.test {
    useJUnitPlatform()
}
