
# Introducing Capabilities

Object oriented programming starts with a single object.  Functional programming starts with a single function.  The power in OOP and FP comes from being able to build and compose powerful structures out of these primitives.  OOP is all about wiring objects together.  FP is all about wiring functions together. 

In the same way, [object-capability systems](https://en.wikipedia.org/wiki/Capability-based_security), also known as OCAP, start with a single capability, and builds on it by wiring capabilities together.  Just as an object is the primitive of OOP and a function is the primitive of FP, a capability is the primitive of OCAP.

However, there is a difference between FP, OOP and OCAP.  While FP and OOP are concerned about program design, OCAP (and capabilities in general) is concerned about security.  Like an [access control list](https://en.wikipedia.org/wiki/Access_control_list), a capability is a security primitive.

## Why Use Object Capabilities?
 
Object Capabilities make a natural building block for designing secure systems for several reasons.

From an engineering perspective:

* Object capabilities follow the programming domain precisely and build on encapsulation and information hiding principles that are already well-known and well understood by the programming community.
* Capabilities allow for flexible delegation without widening access control. Workers have only the access they need to do their jobs, and nothing more.
* Auditing, logging, security notifications and accountability can be built directly into capabilities without changes to application code.
* Object capabilities are "provably correct" through static analysis and flow control.

From a security perspective:
 
* Capabilities are a better solution to the [confused deputy problem](https://en.wikipedia.org/wiki/Confused_deputy_problem) and [Time of Check/Time of Use problem](https://en.wikipedia.org/wiki/Time_of_check_to_time_of_use).
* Capabilities allow for on-the-spot revocation at any level with the appropriate design, for immediate lockdown of an account or resource.
* Built-in time based or execution based expiration of capabilities, after which the capability is revoked.  Allows for "sudo" and "1-time" access.
* Capabilities allow for a far richer domain of security control and can manage access decisions involving multiple principals, as opposed to [access control lists](http://waterken.sourceforge.net/aclsdont/current.pdf).

## Definition

So what is a capability?

We use the following definitions for an object capability:
 
**A capability is a security primitive that confers authority by reference.**

We use the [following definition of authority](https://ai.google/research/pubs/pub45570):

**An authority is *sufficient justification* to affect a resource.**

In Scala, a resource is *an object* and a capability is a *reference* to an object that can affect that resource.  There is also the concept of *permission*, which is a direct reference to the resource -- however, as we'll see, permission doesn't always imply authority or vice versa.

In fact, capability may be a reference to the resource itself, or it may be a very indirect chain of forwarded calls to an inner class which is not the resource, which can change the resource's internals.  As long as the end result is the same and the resource is affected, it doesn't matter.
 
There is an important difference between capabilities and object oriented programming.  Whereas OOP is typically interested in **making things accessible** and creating graphs of things, a capability is a security tool, used for **making things inaccessible**.  A capability is a precious thing, a tightly guarded key to a locked room full of treasure.  You are only handed one, and if you lose it, then you must ask a @ref:[gatekeeper](../examples/gatekeeper.md) to give you another one.

## Scala Example

Say that you have a class `Foo`:

```scala
case class Foo(name: String)
```

A class by itself doesn't mean anything -- we have to create an instance of the class aka an object:

```scala
def createFoo: Unit = Foo("will") // create the object!
```

In the above sample, we've left something out. We've created a `Foo` instance -- but because the method returns `Unit`, there is no reference to the object.  We can't get the name.  In order to be useful, we have to return a reference to the `Foo` instance:

```scala
def createFoo: Foo = Foo("will")
```

If you have a reference to an object, you can, at a minimum, access all of the public methods and fields on that object.  

```scala
val foo = createFoo
// now we can affect the object referred to as `foo` by accessing name
val name = foo.name
```

@@@mermaid
```
graph LR
You -->|foo| Foo(Foo object)
```
@@@

If you call a method, or access a field, or modify a field, then you are *affecting the resource*. Therefore, your `foo` capability is sufficient justification to affect the resource that is the `Foo` instance.

## Differences between Capabilities and OO

It may seem like the only thing we've done here is come up with different names for well known concepts, but that's not actually the case.  Capabilities come from a different space altogether, where "unforgeable references" meant long strings or signed and encrypted messages.  Implementing capabilities in an OO language -- *object capabilities* or *ocap* for short -- is a very clean mapping, because references to objects can't be forged or faked, and so much of the ground level work has already been done.  

But what distinguishes a capability from a plain old object reference?  There are a few qualifications:
 
1. The resource is valuable, and typically long lived.  It contains methods and data that should not be touched lightly.  Controlling access to the resource is important.

2. The capability is the **only way** to access the resource.  There is no back door (DI framework, static holder) reference that "gets around" the capability model.  

3. The capability is opaque.  You have an object reference to a trait, but you can't downcast that, introspect it, change access modifiers etc.  The trait may run through any number of forwarders (the 'capability' word for filters / proxies / delegates) before executing the call on the resource, and there is no way to circumvent that.

## Caveats and Assumptions

The assumption in this document is that you are playing a straight game, where references are handled only through creation (you created the reference), parentage (you are a subclass and you have access to references scoped by your parent class), or through scoping (you were handed references through parameters in the class, or followed an existing reference).  

Basically, you're not setting or accessing capabilities through global state through singleton objects, static fields, or thread locals.  You can use Akka and only send capabilities through messages between actors for a more accurate capability model, but it's not required.

Likewise, we assume that you're not a hostile attacker -- if you are hostile and can execute code in the JVM, it's trivial to subvert the SecurityManager, call `setAccessible` and start [monkeypatching](https://tersesystems.com/blog/2014/03/02/monkeypatching-java-classes/), so effectively all fields and methods are public if you scratch hard enough.  Scala is not an ocap language, so this is purely about capabilities as a software engineering practice.

## History

The original paper describing capabilities is [Programming Semantics for Multiprogrammed Computations](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.16.9948) by Dennis and Van Horn in 1963, but in an operating systems context.  

In 2003, Miller, Yee and Shapiro wrote [Capability Myths Demolished](http://srl.cs.jhu.edu/pubs/SRL2003-02.pdf), which goes through [the incorrect assumptions about capabilities](https://blog.acolyer.org/2016/02/16/capability-myths-demolished/), and [Paradigm Regained: Abstraction Mechanisms for Access Control](http://www.erights.org/talks/asian03/paradigm-revised.pdf).

Miller followed this up with his 2006 PhD thesis, [Robust Composition: Towards a Unified Approach to Access Control and Concurrency Control](http://erights.org/talks/thesis/markm-thesis.pdf).  Miller's PhD thesis is regarded as the founding document of the [Object Capability Model](https://en.wikipedia.org/wiki/Object-capability_model).

Other useful papers include [Protection in Programming Languages](http://www.erights.org/history/morris73.pdf), which covers dynamic sealing, and [Trustworthy Proxies: Virtualizing Objects with Invariants](https://ai.google/research/pubs/pub40736) which expands on the Membrane pattern.