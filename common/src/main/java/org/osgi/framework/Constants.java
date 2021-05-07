/*
 * Copyright (c) OSGi Alliance (2000, 2020). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework;

public interface Constants {
    String SYSTEM_BUNDLE_LOCATION = "System Bundle";
    String SYSTEM_BUNDLE_SYMBOLICNAME = "system.bundle";
    long SYSTEM_BUNDLE_ID = 0L;
    String BUNDLE_CATEGORY = "Bundle-Category";
    String BUNDLE_CLASSPATH = "Bundle-ClassPath";
    String BUNDLE_COPYRIGHT = "Bundle-Copyright";
    String BUNDLE_DESCRIPTION = "Bundle-Description";
    String BUNDLE_NAME = "Bundle-Name";
    String BUNDLE_NATIVECODE = "Bundle-NativeCode";
    String EXPORT_PACKAGE = "Export-Package";
    String EXPORT_SERVICE = "Export-Service";
    String IMPORT_PACKAGE = "Import-Package";
    String DYNAMICIMPORT_PACKAGE = "DynamicImport-Package";
    String IMPORT_SERVICE = "Import-Service";
    String BUNDLE_VENDOR = "Bundle-Vendor";
    String BUNDLE_VERSION = "Bundle-Version";
    String BUNDLE_DOCURL = "Bundle-DocURL";
    String BUNDLE_CONTACTADDRESS = "Bundle-ContactAddress";
    String BUNDLE_ACTIVATOR = "Bundle-Activator";
    String EXTENSION_BUNDLE_ACTIVATOR = "ExtensionBundle-Activator";
    String BUNDLE_UPDATELOCATION = "Bundle-UpdateLocation";
    String PACKAGE_SPECIFICATION_VERSION = "specification-version";
    String BUNDLE_NATIVECODE_PROCESSOR = "processor";
    String BUNDLE_NATIVECODE_OSNAME = "osname";
    String BUNDLE_NATIVECODE_OSVERSION = "osversion";
    String BUNDLE_NATIVECODE_LANGUAGE = "language";
    String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";
    String SINGLETON_DIRECTIVE = "singleton";
    String FRAGMENT_ATTACHMENT_DIRECTIVE = "fragment-attachment";
    String FRAGMENT_ATTACHMENT_ALWAYS = "always";
    String FRAGMENT_ATTACHMENT_RESOLVETIME = "resolve-time";
    String FRAGMENT_ATTACHMENT_NEVER = "never";
    String BUNDLE_LOCALIZATION = "Bundle-Localization";
    String BUNDLE_LOCALIZATION_DEFAULT_BASENAME = "OSGI-INF/l10n/bundle";
    String REQUIRE_BUNDLE = "Require-Bundle";
    String BUNDLE_VERSION_ATTRIBUTE = "bundle-version";
    String FRAGMENT_HOST = "Fragment-Host";
    String SELECTION_FILTER_ATTRIBUTE = "selection-filter";
    String BUNDLE_MANIFESTVERSION = "Bundle-ManifestVersion";
    String VERSION_ATTRIBUTE = "version";
    String BUNDLE_SYMBOLICNAME_ATTRIBUTE = "bundle-symbolic-name";
    String RESOLUTION_DIRECTIVE = "resolution";
    String RESOLUTION_MANDATORY = "mandatory";
    String RESOLUTION_OPTIONAL = "optional";
    String USES_DIRECTIVE = "uses";
    String INCLUDE_DIRECTIVE = "include";
    String EXCLUDE_DIRECTIVE = "exclude";
    String MANDATORY_DIRECTIVE = "mandatory";
    String VISIBILITY_DIRECTIVE = "visibility";
    String VISIBILITY_PRIVATE = "private";
    String VISIBILITY_REEXPORT = "reexport";
    String EXTENSION_DIRECTIVE = "extension";
    String EXTENSION_FRAMEWORK = "framework";
    String EXTENSION_BOOTCLASSPATH = "bootclasspath";
    String BUNDLE_ACTIVATIONPOLICY = "Bundle-ActivationPolicy";
    String ACTIVATION_LAZY = "lazy";
    String FRAMEWORK_VERSION = "org.osgi.framework.version";
    String FRAMEWORK_VENDOR = "org.osgi.framework.vendor";
    String FRAMEWORK_LANGUAGE = "org.osgi.framework.language";
    String FRAMEWORK_OS_NAME = "org.osgi.framework.os.name";
    String FRAMEWORK_OS_VERSION = "org.osgi.framework.os.version";
    String FRAMEWORK_PROCESSOR = "org.osgi.framework.processor";
    String FRAMEWORK_EXECUTIONENVIRONMENT = "org.osgi.framework.executionenvironment";
    String FRAMEWORK_BOOTDELEGATION = "org.osgi.framework.bootdelegation";
    String FRAMEWORK_SYSTEMPACKAGES = "org.osgi.framework.system.packages";
    String FRAMEWORK_SYSTEMPACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
    String SUPPORTS_FRAMEWORK_EXTENSION = "org.osgi.supports.framework.extension";
    String SUPPORTS_BOOTCLASSPATH_EXTENSION = "org.osgi.supports.bootclasspath.extension";
    String SUPPORTS_FRAMEWORK_FRAGMENT = "org.osgi.supports.framework.fragment";
    String SUPPORTS_FRAMEWORK_REQUIREBUNDLE = "org.osgi.supports.framework.requirebundle";
    String FRAMEWORK_SECURITY = "org.osgi.framework.security";
    String FRAMEWORK_SECURITY_OSGI = "osgi";
    String FRAMEWORK_STORAGE = "org.osgi.framework.storage";
    String FRAMEWORK_STORAGE_CLEAN = "org.osgi.framework.storage.clean";
    String FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT = "onFirstInit";
    String FRAMEWORK_LIBRARY_EXTENSIONS = "org.osgi.framework.library.extensions";
    String FRAMEWORK_EXECPERMISSION = "org.osgi.framework.command.execpermission";
    String FRAMEWORK_COMMAND_ABSPATH = "abspath";
    String FRAMEWORK_TRUST_REPOSITORIES = "org.osgi.framework.trust.repositories";
    String FRAMEWORK_WINDOWSYSTEM = "org.osgi.framework.windowsystem";
    String FRAMEWORK_BEGINNING_STARTLEVEL = "org.osgi.framework.startlevel.beginning";
    String FRAMEWORK_BUNDLE_PARENT = "org.osgi.framework.bundle.parent";
    String FRAMEWORK_BUNDLE_PARENT_BOOT = "boot";
    String FRAMEWORK_BUNDLE_PARENT_EXT = "ext";
    String FRAMEWORK_BUNDLE_PARENT_APP = "app";
    String FRAMEWORK_BUNDLE_PARENT_FRAMEWORK = "framework";

    /*
     * Service properties.
     */ String OBJECTCLASS = "objectClass";
    String SERVICE_ID = "service.id";
    String SERVICE_PID = "service.pid";
    String SERVICE_RANKING = "service.ranking";
    String SERVICE_VENDOR = "service.vendor";
    String SERVICE_DESCRIPTION = "service.description";
    String SERVICE_BUNDLEID = "service.bundleid";
    String SERVICE_SCOPE = "service.scope";
    String SCOPE_SINGLETON = "singleton";
    String SCOPE_BUNDLE = "bundle";
    String SCOPE_PROTOTYPE = "prototype";
    String FRAMEWORK_UUID = "org.osgi.framework.uuid";
    String REMOTE_CONFIGS_SUPPORTED = "remote.configs.supported";
    String REMOTE_INTENTS_SUPPORTED = "remote.intents.supported";
    String SERVICE_EXPORTED_CONFIGS = "service.exported.configs";
    String SERVICE_EXPORTED_INTENTS = "service.exported.intents";
    String SERVICE_EXPORTED_INTENTS_EXTRA = "service.exported.intents.extra";
    String SERVICE_EXPORTED_INTERFACES = "service.exported.interfaces";
    String SERVICE_IMPORTED = "service.imported";
    String SERVICE_IMPORTED_CONFIGS = "service.imported.configs";
    String SERVICE_INTENTS = "service.intents";
    String PROVIDE_CAPABILITY = "Provide-Capability";
    String REQUIRE_CAPABILITY = "Require-Capability";
    String EFFECTIVE_DIRECTIVE = "effective";
    String EFFECTIVE_RESOLVE = "resolve";
    String EFFECTIVE_ACTIVE = "active";
    String FILTER_DIRECTIVE = "filter";
    String FRAMEWORK_SYSTEMCAPABILITIES = "org.osgi.framework.system.capabilities";
    String FRAMEWORK_SYSTEMCAPABILITIES_EXTRA = "org.osgi.framework.system.capabilities.extra";
    String FRAMEWORK_BSNVERSION = "org.osgi.framework.bsnversion";
    String FRAMEWORK_BSNVERSION_MULTIPLE = "multiple";
    String FRAMEWORK_BSNVERSION_SINGLE = "single";
    String FRAMEWORK_BSNVERSION_MANAGED = "managed";
    String BUNDLE_ICON = "Bundle-Icon";
    String BUNDLE_LICENSE = "Bundle-License";
    String BUNDLE_DEVELOPERS = "Bundle-Developers";
    String BUNDLE_SCM = "Bundle-SCM";
    String SERVICE_CHANGECOUNT = "service.changecount";
    String INTENT_BASIC = "osgi.basic";
    String INTENT_ASYNC = "osgi.async";
    String INTENT_CONFIDENTIAL = "osgi.confidential";
    String INTENT_PRIVATE = "osgi.private";
}
