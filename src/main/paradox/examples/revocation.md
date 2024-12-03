# Revocation

This is an example of revocation in ocaps.  

Revocation involves two steps -- first, the original capability is [thunked](https://en.wikipedia.org/wiki/Thunk) so that access is delayed, and then a proxy encapsulating that thunk is exposed, along with a `Revoker` which controls the thunked access, through a `Revocable` (aka caretaker).

Because revocation provides access mediated over time, it is sometimes [described](http://soft.vub.ac.be/events/mobicrant_talks/talk2_OO_security.pdf) as "temporal @ref:[attenuation](attenuation.md)."

You can read @ref:[Managing Capabilities](../guide/management.md) in the guide for more information.

@@snip [Revocation.scala]($examples$/Revocation.scala) { #revocation }
