# Dynamic Sealing

This is an example of dynamic sealing in ocaps.

A sealer/unsealer pair is exposed through a brand.  A sealer can produce a `Box[Foo]`, and the box will prevent access to the boxed value.  Unsealing the box with the unsealer will make it visible again.

Because a box can only be unsealed by objects that have the unsealer, this allows for passing values in the blind.  Combined in the right manner, dynamic sealing can be used to implement @ref:[responsibility tracking](horton.md) in capabilities.

Capability patterns often involve dynamic sealing in some form.  Please see [Capability Patterns](http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns) for more information.

@@snip [DynamicSeal.scala]($examples$/DynamicSeal.scala) { #dynamic-seal }
