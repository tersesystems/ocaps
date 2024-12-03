# Abstraction

Packing lower level capabilities into more convenient APIs.  See [definition](https://github.com/GravityNetwork/Gravity/wiki/What-are-Capabilities#abstraction).  

This is different from @ref:[abstraction](abstraction.md) in that abstraction works to perform an operation using the lower level capabilities, but that operation could have been performed by working with the lower level capabilities individually i.e. no new functionality was revealed.

Here's an example of a repository exposed with several facets, with a higher level `NameChanger` capability composed out of two lower level ones.

@@snip [RepositoryComposition.scala]($examples$/RepositoryComposition.scala) { #repository-composition }
