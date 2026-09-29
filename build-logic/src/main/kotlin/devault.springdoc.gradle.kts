// Adds OpenAPI documentation (springdoc) to a Spring Boot service.
// Shared OpenAPI setup (bearer JWT scheme, API info, hidden @AuthenticationPrincipal)
// comes from common-lib auto-configuration, which activates when springdoc is on the classpath.
plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
    implementation(project(":common-lib"))
}
