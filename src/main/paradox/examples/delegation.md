# Delegation

This is an example of delegation in ocaps.

Delegation is probably the easiest thing in capabiities to understand, as you're passing a reference to another object, either by passing it as a parameter, or through direct assignment.

Note that assigning a capability to a singleton object or to a thread local is not delegation, as you are exposing the capability globally rather than to a specific target.

Delegation is often combined with @ref:[revocation](revocation.md), so that the delegated access to the capability can be revoked as necessary.

You can read @ref:[Managing Capabilities](../guide/management.md) in the guide for more information.

@@snip [Delegation.scala]($examples$/Delegation.scala) { #delegation }
