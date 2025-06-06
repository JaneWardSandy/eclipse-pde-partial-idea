# Eclipse PDE Partial IDEA Plugin

[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/jetbrains/plugin/v/16761-eclipse-pde-partial.svg)](https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial)
[![View at JetBrains](https://img.shields.io/jetbrains/plugin/d/16761-eclipse-pde-partial.svg)](https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial)
[![Release](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/actions/workflows/release.yml/badge.svg)](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/actions/workflows/release.yml)
[![Dependabot Updates](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/actions/workflows/dependabot/dependabot-updates)

<!-- Plugin description -->
Eclipse PDE partial foundation, and support for eclipse RCP(OSGI) platform and its application.

- Open **Preferences... | Languages & Frameworks | Eclipse Target**.
- Add the OSGI bundles that you want to work with (Eclipse root, Teamcenter root, plugins directory, p2, etc.).
- Add the Eclipse PDE Partial facet (or by add framework) to any module that should be an Eclipse Plugins.

To run eclipse application on local machine, create a new Eclipse Application Partial run configuration.

**Breaking Changed:** Versions after v1.6.8 no longer support remote debugging and Wishes-launcher
<!-- Plugin description end -->

## Download

The plugin is available in
the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial), so you can download
it
directly from your IDE.

## Issues or questions?

Please report any [issues](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues)

## Features

### Eclipse Target Application

Support coding with eclipse framework by using Java or Kotlin, or any other language supported by JVM.

### Dependency Detect

Import or using class, methods will inspection with required-bundle and import-package

### Launch Plugins

Support launch application and debug, hot swap class and break code

Launch application by local eclipse launcher, auto generate runtime file and bundle lists

### plugin.xml resolve

Tag hint, attribute auto-completion, extension-point valid

## Running from source

Clone the project and open it in IntelliJ IDEA, Selection the Gradle task `intellij > runIde` to start a sandbox
instance of IntelliJ IDEA with the plugin installed

## Comparison

| Feature                | [PDE Partial]((https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial)) | [Osmorc](https://plugins.jetbrains.com/plugin/1816-osgi) |
|------------------------|---------------------------------------------------------------------------------|----------------------------------------------------------|
| Framework support      | equinox Only                                                                    | equinox, felix, concierge, knopflerfish                  |
| Project import support | x                                                                               | by bnd                                                   |
| Bundle source          | eclipse target                                                                  | bundle directory                                         |
| Manifest lint          | partial support                                                                 | partial support                                          |
| Manifest auto gen      | x                                                                               | by bnd                                                   |
| Manifest manually edit | support with hint, reference navigate                                           | support                                                  |
| Code inspection        | support                                                                         | support                                                  |
| Package accessibility  | java, kotlin, import package, required bundle, re-export required bundle        | java, import package only                                |
| Quick fix              | import package, required bundle, required bundle with version                   | import package                                           |
| Bundle startup manager | startup level only                                                              | startup level, auto startup                              |
| BND                    | x                                                                               | by bnd                                                   |
| Maven                  | x                                                                               | by bnd                                                   |
| Library                | auto create                                                                     | x                                                        |
| Facet                  | for binary package                                                              | for manifest file edit mode                              |
| Artifact               | auto create for package                                                         | x                                                        |
| OSGI launche           | x                                                                               | support                                                  |
| OSGI console           | x                                                                               | support                                                  |
| Launch RCP             | local run/debug                                                                 | x                                                        |

## Screenshots

![LocalRunConfiguration](https://plugins.jetbrains.com/files/16761/screenshot_c27a5613-963b-458d-ac19-47dd0823aa98)

![LocalRunWithKotlin](https://plugins.jetbrains.com/files/16761/screenshot_2e8a5389-cfbf-4ad9-bd8b-f34099455c7c)

![Target](https://plugins.jetbrains.com/files/16761/screenshot_66e04a1a-13a0-4368-a566-934e54535d80)

![StartLevel](https://plugins.jetbrains.com/files/16761/screenshot_7b53b865-e7ae-4cea-947d-b26ed68a58bb)

![CheckBundles](https://plugins.jetbrains.com/files/16761/screenshot_53e358f5-c53f-474c-bd4d-ea0c6b278a07)

![FilterBundles](https://plugins.jetbrains.com/files/16761/screenshot_f5b9737e-f335-4729-94e7-22d6ad3f25a4)

![CodeInspection](https://plugins.jetbrains.com/files/16761/screenshot_5ced688f-a4ae-4cce-a2d3-9d06928f5c3f)

![QuickFixCode](https://plugins.jetbrains.com/files/16761/screenshot_0f27f96f-5756-4a91-9dcf-f51a8a7b2d22)

![ManifestMFInspection](https://plugins.jetbrains.com/files/16761/screenshot_04ea23b1-7a2b-4d9a-81eb-1ffdc176d8cd)

![AutoDependencies](https://plugins.jetbrains.com/files/16761/screenshot_0eec9cb4-b085-4dc0-985f-03fe3f60561b)

![AutoLibraries](https://plugins.jetbrains.com/files/16761/screenshot_affb493a-eee9-4c73-8f71-dbfa6017eb6c)

![PluginXMLEPCompletion](https://plugins.jetbrains.com/files/16761/screenshot_532f0c69-d65d-425d-a9a9-e990b284e89c)

![PluginXMLCompletion](https://plugins.jetbrains.com/files/16761/screenshot_6f8c358a-c0a6-424a-8a7f-acf5dcf08154)

![PluginXMLIdCompletion](https://plugins.jetbrains.com/files/16761/screenshot_70882df8-07e4-410e-92ac-2c492883f54e)

## Thanks

Development powered
by [JetBrains.](https://www.jetbrains.com/community/opensource/?utm_campaign=opensource&utm_content=approved&utm_medium=email&utm_source=newsletter&utm_term=jblogo#support&from=eclipse-pde-partial-idea)

[![JetBrains Logo (Main) logo](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg)](https://www.jetbrains.com/?from=eclipse-pde-partial-idea)

Whichever technologies you use, there's a JetBrains tool to match.
