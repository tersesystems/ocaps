# Expiration

This is an example of expiration in ocaps.  

Expiration combines @ref:[modulation](modulation.md) of a capability with @ref:[revocation](revocation.md).  Using modulation, a capability can make use of internal or external state to decide whether it should revoke access through an internal revoker.

You can read @ref:[Managing Capabilities](../guide/management.md) in the guide for more information.

@@snip [Modulation.scala]($examples$/Modulation.scala) { #modulation }
