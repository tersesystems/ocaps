# Modulation

This is an example of modulation in ocaps.  

Modulation can involve any kind of intermediary processing involved in handling a capability.  Here, the example shows logging of a capability, using before and after hooks.

Modulation is not simply decorative; it can also affect the functionality of operations.  For example, modulation can "narrow" a `Finder` capability by restricting the `find` operation to a single ID, or replace the result of processing that includes sensitive details.  Modulation that interferes with processing is potentially fraught, so it's best done carefully under specific circumstances.

You can read @ref:[Managing Capabilities](../guide/management.md) in the guide for more information.

@@snip [Modulation.scala]($examples$/Modulation.scala) { #modulation }
