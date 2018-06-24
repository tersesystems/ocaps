# Construction

Constructing a capability in Scala is all about deciding what operations on a resource should be isolated and encapsulated with access control.

## Before

We'll start by showing a class that has methods that we want to capabilities.  Here's a `Document` class that lets you change the name.

@@snip [Construction.scala]($examples$/Construction.scala) { #before }

Single Abstract Method (SAM) traits, such as `java.lang.Runnable` and `FunctionN` are natural capabilities.  

However, anonymous functions typically don't expose the domain, and don't indicate sensitivity -- one `() => String` looks much the same as another, so it's harder to track where a capability is exposed.  You can extend these to provided named traits that expose your domain functionality.

For example, we don't want just anyone to be able to change the name, so we set up a `NameChanger` capability, which extends `Function0`.

Note that these are "root level" capabilities -- they are non-revocable, "private key" object references, so you typically want to wrap these in `ocap.Revocable` and hand out the revocable capability instead.

## Construction Through Access

Here's what the code looks like after refactoring.

@@snip [Construction.scala]($examples$/Construction.scala) { #after-amplification }

You'll note that access modifiers are used heavily here, and there's an `Access` class which is involved in exposing the capability.  When two (or more) objects are required to expose some extra functionality, it's called @ref:[amplification](amplification.md).
  
This `Access` class constructor is public here, but obviously anyone who has both `Access` and a resource will be able to expose capabilities, so you should protect your access appropriately.  You can use dynamic sealing or Scala access modifiers to ensure access is tightly controlled.

### Effects

The `Access` class also makes a good place to provide [functional programming effects](https://www.youtube.com/watch?v=po3wmq4S15A) on capabilities.  Please see the @ref:[construction with effects](effects.md) example for more details.

### Subtypes

You can create capabilities on abstract parent classes, and be able to use subtypes appropriately.  Please see the @ref:[construction with subtypes](subtypes.md) example for more details.

## Construction Through Composition

There is another way to construct capabilities, which is through *composition*.

@@snip [Construction.scala]($examples$/Construction.scala) { #after-attenuation }

In this construction, having access to the resource means having access to all of its capabilities, because the resource implements the capabilities directly as traits.

I don't like this construction, because you need to keep access to the resource itself tightly constrained.   

If you want to expose a capability by itself, then you do so by exposing a single capability through @ref:[attenuation](attenuation.md).
