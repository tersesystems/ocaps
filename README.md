# ocaps

*ocaps* is a library for working with [object capabilities](https://en.wikipedia.org/wiki/Object-capability_model) in Scala.

- *Revoker* / *Revocable* classes for revoking capabilities.
- *Brand* for sealing and unsealing capabilities
- *RevocableMembrane* for revocation as an effect.
- Macros for *composition*, *attenuation*, *revocable* and *modulating* capabilities.
- No libraries dependencies (other than `scala-reflect`)

Documentation can be found at [https://tersesystems.github.io/ocaps/](https://tersesystems.github.io/ocaps/)

Examples can be found at [http://tersesystems.github.io/ocaps/examples/](http://tersesystems.github.io/ocaps/examples/)

A guide to object capabilities can be found at [http://tersesystems.github.io/ocaps/guide/](http://tersesystems.github.io/ocaps/guide/)


The `ocaps` library was presented as part of the [Security in Scala](https://na.scaladays.org/schedule/security-with-scala-refined-types-and-object-capabilities) presentation at [Scaladays NYC 2018](https://na.scaladays.org/).  [Slides](https://tersesystems.github.io/ocaps/slides/) and [video](https://slideslive.com/38908776/security-with-scala-refined-types-and-object-capabilities?subdomain=false) are available.

## Usage

There is currently no compiled package available.  RIP Bintray.

Add the following to `build.sbt`

```
// where latestVersion is defined up top
libraryDependencies += "ocaps" %% "ocaps-core" % latestVersion
```

## Releasing

To release a new version of `ocaps`:

```
sbt release
```

## Updating Website

To update the website, change `version.sbt` so it's the release version and not snapshot (there should be a way to do this automatically) and then do the following:

```
sbt
> clean 
> makeSite
> previewSite
> ghpagesPushSite
```
