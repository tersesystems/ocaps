# Gatekeeper

This is an example of a gatekeeper in ocaps.  

A gatekeeper looks at the identity (aka security principal) and releases a set of capabilities in response.  Note that the capability is not dependent on the security principal after authorization, i.e. there is no ambient authority.

Gatekeepers typically do not hand out raw capabilities.  Instead, they will typically hand out capabilities combined with @ref:[expiration](expiration.md) and @ref:[revocation](revocation.md) to implement [valet key](https://docs.microsoft.com/en-us/azure/architecture/patterns/valet-key) patterns.

Gatekeeper is often confused with the Powerbox pattern, i.e. [here](http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns).  My working belief is that Powerbox is a UI construct, such as a "file finder" dialog, but the working name isn't important compared to the construct.

Please see the @ref:[authorization](../guide/authorization.md) for more details on how authorization of capabilities is implemented.

@@snip [Gatekeeper.scala]($examples$/Gatekeeper.scala) { #gatekeeper }
