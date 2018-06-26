# Construction Through Access

The most flexible way to set up capabilities from resources is to set up a resource with private methods that are then exposed through an `Access` class in a companion object which has sibling privileges to the capabilities.

Single Abstract Method (SAM) traits, such as `java.lang.Runnable` and `FunctionN` are natural capabilities.  

However, anonymous functions typically don't expose the domain, and don't indicate sensitivity -- one `() => String` looks much the same as another, so it's harder to track where a capability is exposed.  You can extend these to provided named traits that expose your domain functionality.

For example, we don't want just anyone to be able to change the name, so we set up a `NameChanger` capability, which extends `Function0`.

You'll note that access modifiers are used heavily here, and there's an `Access` class which is involved in exposing the capability.  When two (or more) objects are required to expose some extra functionality, it's called @ref:[amplification](../amplification.md), but in this case it's done only to expose the capability itself.
  
This `Access` class constructor is public here, but obviously anyone who has both `Access` and a resource will be able to expose capabilities, so you should protect your access appropriately.  You can use dynamic sealing or Scala access modifiers to ensure access is tightly controlled.

Note that these are "root level" capabilities -- they are non-revocable, "private key" object references, so you typically want to wrap these in `ocap.Revocable` and hand out the revocable capability instead.

You should also be careful that your capability does not "leak": i.e. if you create a capability on a File, then exposing a `java.io.File` object can lead to manipulation of the filesystem.

## Before

@@snip [Construction.scala]($examples$/Construction.scala) { #before }

## After

@@snip [Construction.scala]($examples$/Construction.scala) { #after-amplification }
