# Abstraction

Packing lower level capabilities into more convenient APIs.  See [definition](https://github.com/GravityNetwork/Gravity/wiki/What-are-Capabilities#abstraction).

Here's an example of a repository exposed with several facets, with a higher level `NameChanger` capability composed out of two lower level ones.

@@snip [RepositoryComposition.scala]($examples$/RepositoryComposition.scala) { #repository-composition }
