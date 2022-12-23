# fs-core

The `fs-core` library provides utility classes and interfaces that can assist in implementing NIO.2 file systems.

Please refer to the [Javadoc](https://robtimus.github.io/fs-core/apidocs/index.html) for more information.

## Limitations

Classes [SimpleAbstractPath](https://robtimus.github.io/fs-core/apidocs/com/github/robtimus/filesystems/SimpleAbstractPath.html), which can be used as a base class of [Path](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html) implementations, and [PathMatcherSupport](https://robtimus.github.io/fs-core/apidocs/com/github/robtimus/filesystems/PathMatcherSupport.html), which can be used to help implement [PathMatcher](https://docs.oracle.com/javase/8/docs/api/java/nio/file/PathMatcher.html) support in file systems, always use `/` as separator, and do not allow `/` inside file or directory names.
