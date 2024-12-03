# Construction Through Access

The most flexible way to set up capabilities from resources is to set up a resource with private methods that are then exposed through an `Access` class in a companion object which has sibling privileges to the capabilities.
  
You can read more in @ref:[Constructing Capabilities](../../guide/construction.md#construction-through-access) section of the guide.

## Before

@@snip [Construction.scala]($examples$/Construction.scala) { #before }

## After

@@snip [Construction.scala]($examples$/Construction.scala) { #after-amplification }
