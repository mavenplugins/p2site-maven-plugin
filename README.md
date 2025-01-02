# p2site-maven-plugin

[![MIT License](https://img.shields.io/github/license/mavenplugins/p2site-maven-plugin?label=License)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mavenplugins/p2site-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.mavenplugins/p2site-maven-plugin)
[![CI](https://github.com/mavenplugins/p2site-maven-plugin/actions/workflows/build_and_deploy.yml/badge.svg)](https://github.com/mavenplugins/p2site-maven-plugin/actions/workflows/build_and_deploy.yml)

P2 site utility plugin providing the following goals:
- **build-index-html**:
  - This goal creates template based index.html files recursively for a P2 update sites.
- **create-composite-xmls**:
  - This goal creates P2 composite xml files for upper directories containing multiple P2 update site repos.
