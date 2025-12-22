<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# eclipse-pde-partial-idea-demo Changelog

## [Unreleased]

## [1.7.1] - 2025-12-19

### Chore

- Update supported IntellIj version to 253

## [1.7.0] - 2025-08-12

### Breaking Changed

- Remove remote run configuration
- Remote run with Wishes-launcher not allowed anymore

### Chore

- Re-structure the project
- Remove dead project
- Remove legacy code

### Added

- Allowed to specify the MANIFEST.MF file path relative to module [#148](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/discussions/148)

## [1.6.8] - 2025-05-01

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Add support for JUnit-Plugin test
  - Basic support for running JUnit5 plugin test
  - A basic remote test runner client implementation that will output a test result to console
  - For PDE plugin test application (org.eclipse.pde.junit.runtime.coretestapplication), auto add -testLoaderClass, -loaderpluginname and -port program parameters
  - User has to use -testpluginname {test bundle name} program parameter to specify the plugin test bundle
  - User has to use one of -test, -classname. -classnames, -packagenamefile or -testnamefile program parameter to specify the test(s) to run
  - How to specify the test(s) to run:
    - -test {test class name}:{test method name}
    - -classname {test class name}
    - -classnames {space separated test class names, ends with -}
    - -packagenamefile {absolute or relative path to a text file with one test package name per line}
    - -testnamefile {absolute or relative path to a text file with one test class name per line}

## [1.6.7]

### Fix

- 2024.3 Compatible
- 2025.1 Compatible

### Feature

- Write dependency entries to project .iml file in sorted order
- Write bundles.info in sorted order
- Discover bundle start level from product configuration (.product file)
- Make product selection optional in PDE run configuration
- Support target module selections for PDE run configuration

## [1.6.6]

### Feature

- Support additional classpath for PDE run configuration

## [1.6.5-1]

### Fix

- 2023.3 Compatible

## [1.6.5]

### Try Fix

- ConcurrentModificationException after update
  branch [#119](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/119)

### Fix

- 2024.1 Compatible

## [1.6.4]

### Fix

- getModuleDir in removed module will occur
  NPE [#112](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/112) [#117](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/117)

## [1.6.3]

### Fix

- 2023.2 specify it using 'displayName' or 'key' attribute to avoid necessity to load the configurable class when
  Settings dialog is opened [#111](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/111)

## [1.6.2]

### Feature

- Allowed to configure runtime directory [#110](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/110)

## [1.6.1]

### Fixed

- Stacktrace on startup [#102](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/102)
- RuntimeException: Cannot find an entity at (...)
  PackagingElement.getThisEntity [#106](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/106)

## [1.6.0-1]

## [1.6.0]

### Feature

- Compatibility verification
- Facet option for artifact

## [1.5.2]

### Fixed

- Error "Access is allowed from event dispatch thread
  only" [#101](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/101)

## [1.5.1]

### Fixed

- Write access is allowed inside write-action
  only [#91](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/91)

## [1.5.0]

### Breaking Changes

- The wishes launcher is no longer maintained because jlib has not found a suitable configuration in kotlin 1.7. There
  will be no major adjustments to the wishes launcher in the future, unless it is also considered to be re-shined in the
  v2.0 version.

### Fixed

- 2022.2.2 with plugins will hang the loading components
  dialog [#87](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/87)

## [1.4.2]

### Feature

- P2 Repository's feature not resolve if it was jar file not
  directory [#80](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/80)

## [1.4.1]

### Feature

- Provide a category/feature-based way of importing target platform
  artifacts [#80](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/80)

## [1.4.0]

### Feature

- Inter module dependency detection [#71](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/71)
- Inter module dependency not working for
  Fragment [#72](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/72)
- Fragment support [#74](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/74)
  - Support Eclipse-ExtensibleAPI in MANIFEST.MF, it should be able to show alert when add additional API for the host.
  - Fragment plugins can use the host plugin's class and dependencies, not asking import package or required bundle if
    it has been added in the host, and in order entry they will be re-order to near index.
  - Support Fragment-Host header in MANIFEST.MF, it should be able to highlight keyword, grammar check, BSN check and
    link and auto-completion, attribute check, and host state check
  - Support fragment.xml hints and lints
- MANIFEST.MF typing empty version with "" occurs
  error [#75](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/75)

## [1.3.6]

### Fixed

- After the first setting error, the RMI link will report an error every
  time [#61](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/61)
- Need some config to change the default output folder from out and out/production to target and
  target/classes [#67](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/67)
- Eclipse executable shouldn't be necessary [#68](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/68)

## [1.3.5]

### Fixed

- NullPointerException when Execute the RunConfiguration in both Debug/Run
  Mode [#58](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/58)
- Reload Target on module import/creation [#59](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/59)
- Eclipse target launcher not available on
  Linux [#60](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/60)

## [1.3.4]

### Fixed

- NullPointerException on
  ExtensionPointDefinition [#56](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/56)
- Class inspection should check super type too

## [1.3.3]

### Fixed

- include schema in same directory not found. _Schema Not Found: Schema not existed for
  org.eclipse.core.expressions.definitions at location expressionLanguage.exsd_
- RMI connection time out with 3sec

## [1.3.2]

### Feature

- Add clean option for run configuration to clean runtime directory at startup
- Provide a dialog to check dependencies, and auto resolve the bundle that can be
  resolved [#48](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/48)
- Eclipse target from eclipse that using oomph, but can not read plugins from .p2
  repo [#54](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/54)
- Support Eclipse target bundles provider extension point

### Fixed

- IDE error occurred if the jar file is deleted from target
  location [#50](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/50)

## [1.3.1]

### Feature

- Bundle not found even when available in the Eclipse
  target [#48](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/48)
- Allow use select multi version of bundle in target definition, and they can import into library too
- manifest.mf now can parse version/version range correctly
- Enhance quick search using speed search component

## [1.3.0]

### Feature

- Dismantling kotlin dependencies [#39](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/39)
- Search box in Eclipse target's Content tab [#45](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/45)

### Fixed

- PluginException when starting IntelliJ [#42](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/42)
- Inspection should check all export bundle, not fast
  fall [#43](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/43)
- Invalid extension points/tags in plugin.xml [#44](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/44)
- Wrong application name in run/debug
  configuration [#46](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/46)

## [1.2.3-1]

### Fixed

- Apply run configuration when application or product not specify occur
  NPE [#40](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/40)
- Config.ini not found occur value of getProperty as
  null [#41](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/41)

## [1.2.3]

### Feature

- auto check bundle version when add/edit location

### Fixed

- com.intellij.diagnostic.PluginException: encoded string too
  long [#36](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/36)
- Partial-CompileOnly dependencies are not
  indexed [#38](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/38)

## [1.2.2-1]

### Fixed

- NoClassDefFoundError: AddModifierFixKt [#35](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/35)

## [1.2.2]

### Feature

- Additional completion for plugin.xml and manifest.mf

### Fixed

- Indexing process should not rely on non-indexed file
  data. [#30](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/30)
- Schema choice occurs limit wrong [#31](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/31)
- identify org.eclipse.ui.perspectives/perspective/@id not
  found [#32](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/32)
- Multitask run when opened more projects [#33](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/33)

## [1.2.1]

### Feature

- plugin.xml and EPs, manifest.mf add side-cache with
  indexes [#25](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/25)

### Fixed

- DomExtender enhancement [#24](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/24)

## [1.2.0]

### Feature

- Migrate libraries register from ModuleHelper to
  EP [#16](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/16)
- Required bundle and re-exported bundle was ordered
- Mark schema `.exsd` as xml, and valid by schema file
- Parse `plugin.xml` file, and support hint,
  completion [#22](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/22)

### Fixed

- Provide product, application combo [#2](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/2)
- Support jars.extra.classpath [#13](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/13)
- Wrong imported JAR [#19](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/19)

### Known issues

- `plugin.xml` not support some rule now, like `extendClass`, child number limit,
  ets. [#24](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/24)
- `plugin.xml` resolve dynamic by `Cached-value`, and `identify` attribute was reference by `plugin.xml` resolve, PSI
  modified persist into virtual file was delay, so `identify` list won't util PSI
  persist. [#25](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/25)

## [1.1.3]

### Fixed

- Not exited library should be removed
- Select latest version of a library if multiple are
  present [#14](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/14)

### Feature

- Wishes launcher showed IP address in ProgressView

## [1.1.2]

### Fixed

- Cannot find entity for library with ID
  LibraryId [#11](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/11)
- FileTooBigException on Target definition [#12](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/12)
- Class in function parameter should be imported
  too [#15](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/15)

### Performance

- Speed-up re-build project library

## [1.1.1]

### Fixed

- Lib inner jar not support by IDEA [#8](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/8)
- Accessibility: Bundle-ClassPath not the highest
  priority [#9](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/9)
- Inner class not resolve as inner, wrong accessibility inspection
- Write access required write action

## [1.1.0]

### Breaking Changes

- New bundle management, split library by single bundle
- Will add many library start with "Partial: "

### Fixed

- Now it can resolve jar in bundle-class-path correct.
- Meta-Inf file will create on facet added
- Bundle dependency resolve wrong [#1](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/1)

### New

- Some manifest completion

## [1.0.1]

### Fixed

- Module in same project was required in another module, but not add into dependency tree.
- Module in same project was required in another module, but reexport not resolve. Module reexport bundle not pass to
  another module [#3](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/3)
- Manifest bundle reference in same project, linked to wrong module
- Kotlin's property ext access inspection not
  work [#4](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/4)
- Kotlin project alert kotlin bundle not
  required [#5](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/5)

## [1.0.0]

- Initial project, migrating

[Unreleased]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.7.1...HEAD
[1.7.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.8...v1.7.0
[1.6.8]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.7...v1.6.8
[1.6.7]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.6...v1.6.7
[1.6.6]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.5-1...v1.6.6
[1.6.5]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.4...v1.6.5
[1.6.5-1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.5...v1.6.5-1
[1.6.4]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.3...v1.6.4
[1.6.3]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.2...v1.6.3
[1.6.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.1...v1.6.2
[1.6.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.0-1...v1.6.1
[1.6.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.5.2...v1.6.0
[1.6.0-1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.6.0...v1.6.0-1
[1.5.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.5.1...v1.5.2
[1.5.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.4.2...v1.5.0
[1.4.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.4.1...v1.4.2
[1.4.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.6...v1.4.0
[1.3.6]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.5...v1.3.6
[1.3.5]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.4...v1.3.5
[1.3.4]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.3...v1.3.4
[1.3.3]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.2...v1.3.3
[1.3.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.3-1...v1.3.0
[1.2.3]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.2-1...v1.2.3
[1.2.3-1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.3...v1.2.3-1
[1.2.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.1...v1.2.2
[1.2.2-1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.2...v1.2.2-1
[1.2.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.1.3...v1.2.0
[1.1.3]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/JaneWardSandy/eclipse-pde-partial-idea/commits/v1.0.0
