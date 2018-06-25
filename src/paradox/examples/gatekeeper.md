# Gatekeeper

This is an example of a gatekeeper in ocaps.  

A gatekeeper looks at the identity (aka security principal) and releases a set of capabilities in response.  Note that the capability is not dependent on the security principal after authorization, i.e. there is no ambient authority.

Please see the @ref:[authorization](../guide/authorization.md) for more details on how authorization of capabilities is implemented.

@@snip [Gatekeeper.scala]($examples$/Gatekeeper.scala) { #gatekeeper }
