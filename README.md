# openapi-spring-generator

OpenAPI Generator for Java Spring framework

[![Build Actions Status](https://github.com/slamdev/openapi-spring-generator/workflows/build/badge.svg)](https://github.com/slamdev/openapi-spring-generator/actions)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/slamdev/openapi-spring-generator/com.github.slamdev.openapi-spring-generator.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradle%20plugin)](https://plugins.gradle.org/plugin/com.github.slamdev.openapi-spring-generator)

## Motivation

In a microservice world you need to provide a communication layer between your services. Doesnt matter you go with sync
(HTTP) or async (PubSub) way, you need to maintain a contract between two sides.

This plugin generates code for DTOs as well as interfaces\services for HTTP client\server and PubSub consumer\producer
based on the OpenApi v2\v3 spec file.

Whenever the spec file is changed, generated code is changed as well, thus allowing you to catch and contract 
incompatibilities in the runtime.

## Usage

First step is to add the plugin to your project as described [here](https://plugins.gradle.org/plugin/com.github.slamdev.openapi-spring-generator).

Next step is to write specification of you API in [Swagger/OpenAPI](http://swagger.io/) yaml format. [Swagger editor](http://editor.swagger.io/)
is really helpful for this task.

## Make a release

```shell script
TAG=x.x.x && git tag -a ${TAG} -m "make ${TAG} release" && git push --tags
```
