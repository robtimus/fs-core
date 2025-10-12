# fs-core
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/fs-core)](https://search.maven.org/artifact/com.github.robtimus/fs-core)
[![Build Status](https://github.com/robtimus/fs-core/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/fs-core/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Afs-core&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Afs-core)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Afs-core&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Afs-core)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/fs-core/badge.svg)](https://snyk.io/test/github/robtimus/fs-core)

The `fs-core` library provides utility classes and interfaces that can assist in implementing NIO.2 file systems.

Please refer to the [Javadoc](https://robtimus.github.io/fs-core/apidocs/index.html) for more information.

## Limitations

Classes [SimpleAbstractPath](https://robtimus.github.io/fs-core/apidocs/com.github.robtimus.filesystems/com/github/robtimus/filesystems/SimpleAbstractPath.html), which can be used as a base class of [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html) implementations, and [PathMatcherSupport](https://robtimus.github.io/fs-core/apidocs/com.github.robtimus.filesystems/com/github/robtimus/filesystems/PathMatcherSupport.html), which can be used to help implement [PathMatcher](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/PathMatcher.html) support in file systems, always use `/` as separator, and do not allow `/` inside file or directory names.
