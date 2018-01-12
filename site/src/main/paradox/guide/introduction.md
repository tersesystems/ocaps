
# Introducing Capabilities

Object oriented programming starts with a single object.  Functional programming starts with a single function.  The power in these systems comes from being able to build and compose powerful structures out of these primitives.  
 
The definition of capability used here is from [Permission and Authority Revisited: towards a formalization](https://ai.google/research/pubs/pub45570):

**A capability is *sufficient justification* to affect a resource.**

This guide will explain what this means, using the Scala programming language as the implementation.

## Scala Example

In Scala, a resource is *an object* and a capability is the *reference* to that object.  Say that you have a class `Foo`:

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

In general, where OO programming is typically interested in **making things accessible** and creating graphs of things, a capability is a security tool, used for **making things inaccessible**.  A capability is a precious thing, a tightly guarded key to a locked room full of treasure.  You are only handed one, and if you lose it, then you must ask a gatekeeper to give you another one.

We'll go into detail in the next section.
