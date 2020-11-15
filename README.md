# openapi-spring-generator

OpenAPI Generator for Java Spring framework

[![Build Actions Status](https://github.com/slamdev/openapi-spring-generator/workflows/build/badge.svg)](https://github.com/slamdev/openapi-spring-generator/actions)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/slamdev/openapi-spring-generator/com.github.slamdev.openapi-spring-generator.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradle%20plugin)](https://plugins.gradle.org/plugin/com.github.slamdev.openapi-spring-generator)

## Motivation

In a microservice world you need to provide some communication layer between your services. If you are choosing REST API
the workflow is usually the following: in service **A**(producer) you create REST controllers and some DTOs to describe 
you API. In service **B**(consumer) you create the same DTOs and some code to call the **A**'s endpoints. If 
requirements are changed you need to make modifications in both services. If there are multiple consumers there is a big
chance that you forgot to modify one of them.

The AIM of this plugin is to solve this issue and to generate some boilerplate for you communication layer.

## Usage

First step is to add the plugin to your project as described [here](https://plugins.gradle.org/plugin/com.github.slamdev.openapi-spring-generator).

Next step is to write specification of you API in [Swagger/OpenAPI](http://swagger.io/) yaml format. [Swagger editor](http://editor.swagger.io/)
is really helpful for this task.

## Make a release

```shell script
TAG=x.x.x git tag -a ${TAG} -m "make ${TAG} release" && git push --tags
```
