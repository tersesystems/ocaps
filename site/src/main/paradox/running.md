# Running

The ocaps artifacts are available in [https://bintray.com/wsargent/maven/ocaps-core](https://bintray.com/wsargent/maven/ocaps-core).

Source code is available in [https://github.com/wsargent/ocaps](https://github.com/wsargent/ocaps).  Issues can be added at [https://github.com/wsargent/ocaps/issues](https://github.com/wsargent/ocaps/issues).

## Dependencies

To add `ocaps` to your project, add the following resolver to `build.sbt`:

```scala
resolvers += Resolver.bintrayRepo("wsargent","maven")
```

And add the given library dependency in `build.sbt`:

@@dependency[sbt,Maven,Gradle] {
  group="ocaps"
  artifact="ocaps_2.12"
  version=$project.version$
}

## Imports

To import the `ocaps` library classes, please add the following to your Scala files:

```
import ocaps._
import ocaps.macros._
```