# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- Format restrictions - see https://common-changelog.org and https://keepachangelog.com/ for details -->
<!-- Each Release must start with a line for the release version of exactly this format: ## [version] -->
<!-- The subsequent comment lines start with a space - not to irritate the release scripts parser!
 ## [major.minor.micro]
 <empty line> - optional sub sections may follow like:
 ### Added:
 - This feature was added
 <empty line>
 ### Changed:
 - This feature was changed
 <empty line>
 ### Removed:
 - This feature was removed
 <empty line>
 ### Fixed:
 - This issue was fixed
 <empty line>
 <empty line> - next line is the starting of the previous release
 ## [major.minor.micro]
 <empty line>
 <...>
 !!! In addition the compare URL links are to be maintained at the end of this CHANGELOG.md as follows.
     These links provide direct access to the GitHub compare vs. the previous release.
     The particular link of a released version will be copied to the release notes of a release accordingly.
     At the end of this file appropriate compare links have to be maintained for each release version in format:
 
  +-current release version
  |
  |                   +-URL to this repo             previous release version tag-+       +-current release version tag
  |                   |                                                           |       |
 [major.minor.micro]: https://github.com/mavenplugins/p2site-maven-plugin/compare/vM.N.u..vM.N.u
-->
<!--
## [Unreleased]

### 🚨 Removed
- TBD

### 💥 Breaking
- TBD

### 📢 Deprecated
- TBD

### 🚀 New Features
- TBD

### 🐛 Fixes
- TBD

### ✨ Improvements
- TBD

### 🔧 Internal Changes
- TBD

### 🚦 Tests
- TBD

### 📦 Updates
- TBD

### 🔒 Security
- TBD

### 📝 Documentation Updates
- TBD
-->

## [Unreleased]
<!-- !!! Align version in badge URLs as well !!! -->
[![1.0.8 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/p2site-maven-plugin?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=1.0.8)](https://central.sonatype.com/artifact/io.github.mavenplugins/p2site-maven-plugin/1.0.8)

### Summary
- TBD

### 📦 Updates
- TBD


## [1.0.7]
<!-- !!! Align version in badge URLs as well !!! -->
[![1.0.7 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/p2site-maven-plugin?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=1.0.7)](https://central.sonatype.com/artifact/io.github.mavenplugins/p2site-maven-plugin/1.0.7)

### Summary
- Enhance `index.html` to display size and last modified time stamp of files listed

### ✨ Improvements
- Enhance `index.html` to display size and last modified time stamp of files listed

### 🐛 Fixes
- Get `p2.index` listed by `index.html` in case it has been created by the plugin run
- Use P2 Update Site template for `index.html` in case composite files and/or `p2.index` files are being created

## [1.0.6]
<!-- !!! Align version in badge URLs as well !!! -->
[![1.0.6 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/p2site-maven-plugin?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=1.0.6)](https://central.sonatype.com/artifact/io.github.mavenplugins/p2site-maven-plugin/1.0.6)

### Summary
- Make this plugin run as standalone plugin
- Feature enhancements - see below

### 🚀 New Features
- Run as standalone Maven Plugin without a project POM
- Add commandline properties for the appropriate config item:
  - `p2site.baseDir`
  - `p2site.compositeXmlsForFoldersWithUpdateSitePropertiesOnly`
  - `p2site.createCompositeXmls`
  - `p2site.dryRun`
  - `p2site.enforceBaseDirUpdateSitePropertiesFile`
- Add new config / property `p2site.timeStamp` to create composite files with a predefined timestamp
- Add new optional properties for updateSite.properties:
  - `update.site.no_composite_files` - `boolean`: If true no composite files will be created for this directory
  - `update.site.h2` - `string`: Will replace the `<h2>` line in templates. If defined, this will overrule the default `<h2>` line containing `update.site.title` in templates.
  - `update.site.buildinfo` - `string`: If set, this will overrule the default `Build: ${update.site.version}` line in templates.
- `updateSite.properties` can contain variable placeholders in property values of format `${<property name>}`


## [1.0.5]
<!-- !!! Align version in badge URLs as well !!! -->
[![1.0.5 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/p2site-maven-plugin?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=1.0.5)](https://central.sonatype.com/artifact/io.github.mavenplugins/p2site-maven-plugin/1.0.5)

### Summary
- Initial release maintained within GitHub mavenplugins organization
- Moved and updated from `de.mhoffrogge.maven:p2site-maven-plugin:1.0.5` at [p2site-maven-plugin on GitLab](https://gitlab.com/mhopen/maven-plugins/-/tree/master/p2site-maven-plugin?ref_type=heads)

### 📦 Updates
- Maven groupId changed to `io.github.mavenplugins`


<!--
## []

### NeverReleased
- This is just a dummy placeholder to make the parser of GHCICD/release-notes-from-changelog@v1 happy!
-->

[Unreleased]: https://github.com/mavenplugins/p2site-maven-plugin/compare/v1.0.7..HEAD
[1.0.7]: https://github.com/mavenplugins/p2site-maven-plugin/compare/v1.0.6..v1.0.7
[1.0.6]: https://github.com/mavenplugins/p2site-maven-plugin/compare/v1.0.5..v1.0.6
[1.0.5]: https://github.com/mavenplugins/p2site-maven-plugin/releases/tag/v1.0.5
