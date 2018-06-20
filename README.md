# ocaps

[<img src="https://img.shields.io/travis/wsargent/ocaps.svg"/>](https://travis-ci.org/wsargent/ocaps) [![Bintray](https://api.bintray.com/packages/wsargent/maven/ocaps-core/images/download.svg)](https://bintray.com/wsargent/maven/ocaps-core/_latestVersion)

*ocaps* is a library for assembling capabilities in Scala.

- *Revoker* / *Revocable* classes for revoking capabilities.
- *Brand* for sealing and unsealing capabilities
- *PermeableMembrane* for revocation as an effect.
- Macros for *composition*, *attenuation*, *revocable* and *modulating* capabilities.
- No libraries dependencies (other than `scala-reflect`)

Documentation can be found at [https://wsargent.github.io/ocaps/](https://wsargent.github.io/ocaps/)

Examples can be found at [http://wsargent.github.io/ocaps/examples/](http://wsargent.github.io/ocaps/examples/)

A guide to object capabilities can be found at [http://wsargent.github.io/ocaps/guide/](http://wsargent.github.io/ocaps/guide/)


## Usage

```
resolvers += Resolver.bintrayRepo("wsargent","maven")

// where latestVersion is defined up top
libraryDependencies += "ocaps" %% "ocaps-core" % latestVersion
```

## Releasing

```
sbt release
```