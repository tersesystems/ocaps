# Amplification

Amplification is when two object references put together result in an ability to do something not possible if you don't have references to both.

Technically, the `Access` pattern qualifies:

@@snip [Construction.scala]($examples$/Construction.scala) { #access }
 
But amplification is more about bringing together independent objects.  The canonical example is a can and can opener, brought together by @ref:[dynamic sealing](dynamic_seal.md):

@@snip [Amplification.scala]($examples$/Amplification.scala) { #amplification }
