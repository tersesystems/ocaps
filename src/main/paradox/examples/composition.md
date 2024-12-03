# Composition

This is an example of composition using ocaps.

Composition is fairly simple.  You have two capabilities implemented as traits, `A` and `B`.  Using composition, you create a proxy which has a [compound type](https://docs.scala-lang.org/tour/compound-types.html) of `A with B`.

You can read more in @ref:[Constructing Capabilities](../guide/construction.md) section of the guide.

@@snip [Composition.scala]($examples$/Composition.scala) { #composition }

