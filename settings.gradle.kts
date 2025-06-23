plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "mspa"

include("domain")
include("useCase")
include("rest")
include("infrastructure")
include("application")