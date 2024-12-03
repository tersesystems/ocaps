# Membrane

This is an example of a "permeable membrane" pattern in ocaps.

A permeable membrane wraps an object graph using a dependently typed monad to provide a policy effect.  It differs from a "hard membrane" in that wrapping must be done manually and involves co-operation from participants, rather than being provided automatically.

You can read @ref:[Confining Capabilities](../guide/confinement.md) in the guide for more information.

@@snip [Membrane.scala]($examples$/Membrane.scala) { #membrane }