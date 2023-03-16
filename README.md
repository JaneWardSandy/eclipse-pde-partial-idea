# Eclipse PDE Partial IDEA Plugin

[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/jetbrains/plugin/v/16761-eclipse-pde-partial.svg)](https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial)
[![View at JetBrains](https://img.shields.io/jetbrains/plugin/d/16761-eclipse-pde-partial.svg)](https://plugins.jetbrains.com/plugin/16761-eclipse-pde-partial)

**PLEASE NOTE**: The plugin is currently planning to refactor the v2.0 version, and the v1.x version will only be updated with major bugs for the time being, and the new features will be temporarily put into the new version and planned to be implemented.

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

#### On Local Machine

Launch application by local eclipse launcher, auto generate runtime file and bundle lists

#### On Remote machine

By using Wishes-Launcher, allow using RMI to connect remote machine and debug by JDWP

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
| Launche RCP            | local run/debug, remote debug                                                   | x                                                        |

## Screenshots

![](https://plugins.jetbrains.com/files/16761/screenshot_c27a5613-963b-458d-ac19-47dd0823aa98)

![](https://plugins.jetbrains.com/files/16761/screenshot_2e8a5389-cfbf-4ad9-bd8b-f34099455c7c)

![](https://plugins.jetbrains.com/files/16761/screenshot_fa12809c-6e92-432e-8bb6-64f77ca75a05)

![](https://plugins.jetbrains.com/files/16761/screenshot_8e905ded-1a8f-41dd-8a0d-a2c9f403e0bf)

![](https://plugins.jetbrains.com/files/16761/screenshot_66e04a1a-13a0-4368-a566-934e54535d80)

![](https://plugins.jetbrains.com/files/16761/screenshot_7b53b865-e7ae-4cea-947d-b26ed68a58bb)

![](https://plugins.jetbrains.com/files/16761/screenshot_53e358f5-c53f-474c-bd4d-ea0c6b278a07)

![](https://plugins.jetbrains.com/files/16761/screenshot_f5b9737e-f335-4729-94e7-22d6ad3f25a4)

![](https://plugins.jetbrains.com/files/16761/screenshot_5ced688f-a4ae-4cce-a2d3-9d06928f5c3f)

![](https://plugins.jetbrains.com/files/16761/screenshot_0f27f96f-5756-4a91-9dcf-f51a8a7b2d22)

![](https://plugins.jetbrains.com/files/16761/screenshot_04ea23b1-7a2b-4d9a-81eb-1ffdc176d8cd)

![](https://plugins.jetbrains.com/files/16761/screenshot_0eec9cb4-b085-4dc0-985f-03fe3f60561b)

![](https://plugins.jetbrains.com/files/16761/screenshot_affb493a-eee9-4c73-8f71-dbfa6017eb6c)

![](https://plugins.jetbrains.com/files/16761/screenshot_532f0c69-d65d-425d-a9a9-e990b284e89c)

![](https://plugins.jetbrains.com/files/16761/screenshot_6f8c358a-c0a6-424a-8a7f-acf5dcf08154)

![](https://plugins.jetbrains.com/files/16761/screenshot_70882df8-07e4-410e-92ac-2c492883f54e)

## Thanks

Development powered by [JetBrains.](https://www.jetbrains.com/community/opensource/?utm_campaign=opensource&utm_content=approved&utm_medium=email&utm_source=newsletter&utm_term=jblogo#support&from=eclipse-pde-partial-idea)

[![JetBrains Logo (Main) logo](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg)](https://www.jetbrains.com/?from=eclipse-pde-partial-idea)

Whichever technologies you use, there's a JetBrains tool to match.
