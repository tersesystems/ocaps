# Expiration

Expiration combines *modulation* of a capability with *revocation*.  Using modulation, a capability can make use of internal or external state to decide whether it should revoke access through an internal revoker.

You can read @ref:[Managing Capabilities](../guide/management.md) in the guide for more information.

@@snip [Modulation.scala]($examples$/Modulation.scala) { #modulation }
