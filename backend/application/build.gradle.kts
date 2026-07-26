dependencies {
    implementation(project(":domain"))

    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-tx")
    compileOnly("jakarta.validation:jakarta.validation-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
