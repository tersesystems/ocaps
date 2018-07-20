# Responsibility Tracking (Horton)

This is an implementation of [Horton](https://www.usenix.org/legacy/event/hotsec07/tech/full_papers/miller/miller.pdf) in Scala.

Horton is used for responsibility tracking.  Using Horton, stubs and proxies are used in conjunction with dynamic sealing so that a proxy can always establish the provenance of a request as coming from a particular principal.

@@snip [Main.scala]($examples$/horton/Main.scala) { #main }

The implementation is as follows:

@@snip [package.scala]($examples$/horton/package.scala) 

@@snip [Horton.scala]($examples$/horton/Horton.scala) { #horton }