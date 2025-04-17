rootProject.name = "eclipse-pde-partial"

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include("eclipse-pde-partial-idea")
include("common")
//include("wishes-launcher") // deprecated
