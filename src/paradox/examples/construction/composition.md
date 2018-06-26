# Construction Through Composition

In this construction, having access to the resource means having access to all of its capabilities, because the resource implements the capabilities directly as traits.

If you want to expose a capability by itself, then you do so by exposing a single capability through @ref:[attenuation](../attenuation.md).  When a single capability is exposed from a resource containing many capabilities, it is often called a *facet*.

I don't like this construction, because you need to keep access to the resource itself tightly constrained -- access to the resource means access to all of its capabilities and so it must be @ref:[dynamically sealed](../dynamic_seal.md) if you are in a situation where it must be available through a DI framework or thread local.

## Before

@@snip [Construction.scala]($examples$/Construction.scala) { #before }

## After

@@snip [Construction.scala]($examples$/Construction.scala) { #after-attenuation }


