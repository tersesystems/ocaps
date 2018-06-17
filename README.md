# ocaps

[<img src="https://img.shields.io/travis/wsargent/ocaps.svg"/>](https://travis-ci.org/wsargent/ocaps) [![Bintray](https://img.shields.io/bintray/v/ocaps/ocaps-core_2.12.svg)](http://mvnrepository.com/artifact/ocaps/ocaps-core_2.12)

*ocaps* is a library for assembling capabilities in Scala.

- *Revoker* / *Revocable* classes for revoking capabilities.
- *Brand* for sealing and unsealing capabilities
- *PermeableMembrane* for revocation as an effect.
- Macros for *composition*, *attenuation*, *revocable* and *modulating* capabilities.
- No libraries dependencies (other than `scala-reflect`)

Documentation can be found at [https://wsargent.github.io/ocaps/](https://wsargent.github.io/ocaps/)

A guide to object capabilities can be found at [http://wsargent.github.io/ocaps/guide/](http://wsargent.github.io/ocaps/guide/)

Examples to object capabilities can be found at [http://wsargent.github.io/ocaps/examples/](http://wsargent.github.io/ocaps/examples/)


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