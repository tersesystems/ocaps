
# Introducing Capabilities

Object oriented programming starts with a single object.  Functional programming starts with a single function.  The power in OOP and FP comes from being able to build and compose powerful structures out of these primitives.  OOP is all about wiring objects together.  FP is all about wiring functions together. 

Object-capability systems (OCAP) start with a single capability, and builds on it by wiring capabilities together.  

So what is a capability?

## Definition
 
The definition of capability used here is from [Permission and Authority Revisited: towards a formalization](https://ai.google/research/pubs/pub45570):

**A capability is *sufficient justification* to affect a resource.**

In Scala, a resource is *an object* and a capability is the *reference* to that object.
 
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

Likewise, we assume that you're not a hostile attacker -- if you are hostile and can execute code in the JVM, it's trivial to subvert the SecurityManager, call `setAccessible` and start monkeypatching, so effectively all fields and methods are public if you scratch hard enough.  Scala is not an ocap language, this is purely about capabilities as a software engineering practice.
