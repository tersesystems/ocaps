

# Overview

Creating a capability in Scala is all about deciding what operations on a resource should be isolated and encapsulated with access control.

## Before

We'll start by showing a class that has methods that we want to capabilities.  Here's a `Document` class that lets you change the name.

@@fiddle [Simple.scala](../../scala/Simple.scala) { #before extraParams=theme=light&layout=v75 cssStyle=width:100%;height:300px }

Single Abstract Method (SAM) traits, such as `java.lang.Runnable` and `FunctionN` are natural capabilities.  

However, anonymous functions typically don't expose the domain, and don't indicate sensitivity -- one `() => String` looks much the same as another, so it's harder to track where a capability is exposed.  You can extend these to provided named traits that expose your domain functionality.

For example, we don't want just anyone to be able to change the name, so we set up a `NameChanger` capability, which extends `Function0`.

## After Amplification

Here's what the code looks like after refactoring.

@@fiddle [Simple.scala](../../scala/Simple.scala) { #after-amplification extraParams=theme=light&layout=v75 cssStyle=width:100%;height:300px }

You'll note that access modifiers are used heavily here, and there's an `Access` class which is involved in exposing the capability.  When two (or more) objects are required to expose some extra functionality, it's called *amplification*.
  
This `Access` class constructor is public here, but obviously anyone who has both `Access` and a resource will be able to expose capabilities, so you should protect your access appropriately.  You can use dynamic sealing or Scala access modifiers to ensure access is tightly controlled.

The `Access` class also makes a good place to provide [functional programming effects](https://www.youtube.com/watch?v=po3wmq4S15A) on capabilities.  Please see the [Effects](effects.md) page for more details.

Note also that these are "root level" capabilities that are handed out.  These capabilities are non-revocable, "private key" object references, so you typically want to wrap these in `ocap.Revocable` and hand out the revocable capability instead.

## After Attenuation

There is another way to construct capabilities, which is through *composition*.

@@fiddle [Simple.scala](../../scala/Simple.scala) { #after-attenuation extraParams=theme=light&layout=v75 cssStyle=width:100%;height:300px }

In this construction, having access to the resource means having access to all of its capabilities, because the resource implements the capabilities directly as traits.

If you want to expose a capability by itself, then you do so by exposing a single capability through *attenuation*.  You cannot use downcasting, because it's not safe.

This is a bit abstract, so it's worth giving an example.

```scala
class Force {
  private object capabilities {}
}

object Force {
  sealed trait JediCapability {
    def cleanRoom(): CleanRoomResults = ???
  }
    
  sealed trait SithCapability {
    def forcePush(): ForcePushResults = ???
    def choke(): ChokeResults = ???
  }
    
  trait ForceCapability extends JediCapability with SithCapability
    
  trait Jedi {
    def useTheForceWisely(jediPowers: JediCapability): Unit
  }
}
```

Okay, let's try it out.

```scala
val theForce = new Force.ForceCapability
val luke = new Jedi {
  def useTheForceWisely(jediPowers: JediCapability) = {
    jediPowers.cleanRoom()
  }
}
luke.useTheForceWisely(theForce)
```

Looks good!  What could go wrong?

```scala
val kylo = new Jedi {
  override def useTheForceWisely(jediPowers: JediCapability) {
    val sithPowers = jediPowers.asInstanceOf[SithCapability]
    sithPowers.forcePush() // MUA HA HA
  }
}
```

Whoops.

I don't like this construction, because you need to keep access to the resource itself tightly constrained.   
