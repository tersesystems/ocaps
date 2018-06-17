# README



# Capabilities

A capability is a single thing that can access protected areas of a resource and authorizes some kind of access to it through reference.  Capabilities can be implemented in object oriented programming languages, at which point they are called "object capability" systems, or "ocap" for short.

This doesn't mean much by itself.  So, say you have a class `Foo`, with a private method.

```scala
final class Foo(name: String) {
  private def privateDoTheThing(): Unit = {
    println(s"$name.doTheThing()")
  }
}
```

You want to have other users of this class be able to call the private method, but only on a case by case basis.  

Instead of making the method public or protected, you can expose another class that does have access and a reference to that private method.  We'll call this `Foo.Doer`.

```scala
object Foo {
  trait Doer {
    def doTheThing(): Unit
  }
}
```

Now, because we can use Scala's access qualifiers and the trait is inside the companion object, we can create an instance of `Foo.Doer` by closing over Foo:

```scala
final class Foo(name: String) {
  private def privateDoTheThing(): Unit = {
    println(s"$name.doTheThing()")
  }

  private[Foo] object capabilities {
    val doer: Foo.Doer = new Foo.Doer {
      override def doTheThing(): Unit = Foo.this.privateDoTheThing()
    }
  }
}
```

And now we have a capability.  We can expose the capabilities of Foo through a Powerbox that has access.

```scala
import scala.util.{Success, Try}

final class Foo(name: String) {
  private def privateDoTheThing(): Unit = {
    println(s"$name.doTheThing()")
  }

  private[Foo] object capabilities {
    val doer: Foo.Doer = new Foo.Doer {
      override def doTheThing(): Unit = Foo.this.privateDoTheThing()
    }
  }
}

object Foo {

  sealed trait Doer {
    def doTheThing(): Unit
  }

  class Powerbox {
    def askForDoer(foo: Foo): Try[Doer] = Success(foo.capabilities.doer)
  }
}
```

And then try it out:

```scala
object Main {
  def main(args: Array[String]): Unit = {
    val powerbox = new Foo.Powerbox()

    val foo = new Foo("foo")

    powerbox.askForDoer(foo).map { doer =>
      doer.doTheThing()
    }
  }
}
```

And we have a capability.

A capability is a security primitive.  It doesn't look like much by itself.  Then again, object oriented programming starts with a single object.  Functional programming starts with a single function.  The power in these systems comes from being able to build and compose powerful structures out of these primitives.
 
So, now that there's a `Foo.Doer` capability, we can pass around references to that capability.  Access to the reference gives a caller the authority to call the private method on `Foo`.  Also note capability is instance based, so access to one `Foo` instance doesn't mean you have access in general.

```scala
val foo1 = new Foo("foo1")
val doer1 = foo1.capabilities.doer
val foo2 = new Foo("foo2")
val doer2 = foo2.capabilities.doer

val classWithFoo1Authority = new SomeClassThatWantsToDoThings(doer1)
classWithFoo1Authority.doThings() // uses doer1 to access foo1 private method

val classWithFoo2Authority = new SomeClassThatWantsToDoThings(doer2)
classWithFoo2Authority.doThings() // uses doer2 to access foo2 private method

val classWithNoAuthority = new SomeClassThatWantsToDoThings()
classWithNoAuthority.doThings() // whoops
```

Note that although we've made the capability a subtype of `Foo` here, the capability can also enrich or define semantic control over a resource that wasn't there before.  Take the example of an Akka actor, which can receive any method:

```scala
val actorRef = actorSystem.actorOf(Props(FooActor.class))
val doer = new Doer {
  def doTheThing(): Unit = actorRef ! DoTheThing
}
```

By exposing the `Doer` capability instead of the `ActorRef`, callers now have type-safe access to functionality of the Actor, without being directly exposed to Akka itself.

Note that although capabilities may look like functions, and in fact could be replaced as a function, they are objects, consisting of traits and classes which may have several methods.  For example, a `Writer` capability could consist of several means of writing:

```scala
trait Writer {
  def bufferedWriter(): java.io.BufferedWriter
  def outputStream(): java.io.OutputStream
}
```

There is a conceptual difference as well.  Functions are not only anonymous, but are also public.  It's a common pattern to switch between a function and a wrapper type, for instance:

```scala
trait FooBarConverter extends Foo => Bar

object FooBarConverter {
  def apply(f: Foo => Bar): FooBarConverter = new FooBarConverter {
    override def apply(foo: Foo): Bar = f(foo)
  }
}

val converter = FooBarConverter(foo => bar)
```

This does **not** qualify as a capability, because there is no privileged access to the function that can only be referenced through the converter.

## Why this works

Because Scala qualifiers are awesome.

http://jesperdj.com/2016/01/08/scala-access-modifiers-and-qualifiers-in-detail/


## How to assign capabilities

Generally, you want to do an authorization check as close as possible to the execution of the capability, use the capability in context, and then revoke it.

* Only ask for a capability when you know you're going to use it.
* Keep it short.
* Keep it focused.
  
There is a category of attack called Time of Check/Time of Use (TOCTOU), where the longer the period between a check made for the purposes of an operation and the actual operation itself the more opportunity there is for an attacker to "sneak in" under the hood.

There's another attack called the "Confused Deputy" problem, where the more expansive the context of the authority, the more likely the authority is likely to be misused or delegated.

[Capability Myths Demolished](http://srl.cs.jhu.edu/pubs/SRL2003-02.pdf)

* Designation inseperable from authority
* Deputies cannot be confused by authority-less designators
* There are no ambient authorities
* Authorities arrive in contexts of requests
* Subjects can locally identify authorities

## The Downcast Attack

If you use structural types or AOP style mixins, then you may end up with downcasting to a specific type, which could expose your data.

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

What you need to do instead is never to subtype a capability if you can help it.  Instead, close over capabilities to make them safe, and if you have fields make them all private.

```scala
class Force {
   val jediPowers = new JediCapability {
      def cleanRoom() = Force.this.cleanRoom()
   }

  val sithPowers = new SithCapability {
    def forcePush() = Force.this.forcePush()
    def choke() = Force.this.choke()
  }
}
```

### The Reflection Attack

There's another means by which callers can get a reference from a class that they should not be allowed to have: through reflection.

You can open up any field in the JVM using `setAccessible`, and you can monkeypatch any field or method with your own implementation using [`Unsafe.putObject`](http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/).  This can lead to truly horrifying results, i.e.

```java
import java.lang.reflect.*;

public class EverythingIsTrue {
   static void setFinalStatic(Field field, Object newValue) throws Exception {
      field.setAccessible(true);

      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      field.set(null, newValue);
   }
   public static void main(String args[]) throws Exception {      
      setFinalStatic(Boolean.class.getField("FALSE"), true);

      System.out.format("Everything is %s", false); // "Everything is true"
   }
}
```

Needless to say, this disrupts the capability model, as any private reference can be made public, traversed through reflection, and monkeypatched.  

In theory, you can set a security manager on the JVM, but that also comes with caveats.  A full discussion is out of scope here, but Java 9 goes a long way to limiting `setAccessible` in conjunction with the module system

## Stages in Capabilities

## Custom policies on capabilities

We can define custom policies on capabilities, like a OneTimeUse[A], or a GoodForFiveMinutes[A] thing.  We should be able to do this *regardless of the capability*.    

* [Declarative Policies for Capability Control](https://people.seas.harvard.edu/~chong/pubs/csf14_capflow.pdf)
* The need for capability policies https://types.cs.washington.edu/ftfjp2013/preprints/a6-Drossopoulou.pdf
* How to break the bank: Semantics of Capability Policies http://www.doc.ic.ac.uk/~scd/iFM_Caps.pdf
* Towards Capability Policy Specification and Verification https://www.researchgate.net/profile/Sophia_Drossopoulou/publication/261214232_Towards_Capability_Policy_Specification_and_Verification/links/00b7d5339827125ac2000000.pdf

* Restricted delegation in Haskell can be used with refined types to limit to members of a type
* Dependent types to restrict delegation options to sprcific people / resources

Access control policies restrict which components may use a given capability. 

Integrity policies restrict which components may influence the use of a capability.

We demonstrate that standard language-based techniques can soundly enforce these policies (contracts and a security type system respectively).

TODO Is it possible to implement HORTON through monads and additional info on capabilities?

## Capabilities and Effects

In other words, it's not just Membrane. 

"You get to flatmap this ONCE."  Give a demo.  Love that side-effect.

[Designing with Capabilities](https://www.youtube.com/watch?v=fi1FsDW1QeY)  

Existing systems conflate authentication and access decision pieces (presenting authentication when making request)

[Rob Norris](https://youtu.be/po3wmq4S15A?t=26m8s): An effect is whatever distinguishes `F[A]` from `A`.

## Capabilities as gatekeepers to resources

## Capabilities vs CircuitBreakers

### Revocation and Caretakers

One common pattern to use with capabilities is to revoke access to the protected resource.  This is done through the `revoker` trait.

```scala
trait revoker {
  def revoke(): Unit
}
```

Providing a capability with a `revoker` is done through a `Revocable` pattern, which exposes the caretaker with the capability.

```scala
trait Revocable[C] {
  def capability: C
  def revoker: revoker
}
```

The caretaker provides a capability which closes over the previous one, but which can be revoked at any time, causing an exception to be thrown.  Using `Try` is a good practice to incorporate, since capabilities are fungible:


The `Caretaker` has a `provider` function which will call out to the underlying `doer` on every method invocation.

```scala
val revocable: Revocable[Foo.Doer] = Foo.Doer.caretaker(doer)
val revokerDoer: Foo.Doer = caretaker.get
val revoker = caretaker.revoker

val success = revokerDoer.doTheThing() // returns Success!
revoker.revoke() // shut off access through the provider
val failure = revokerDoer.doTheThing() // returns Failure(RevokedException)
```

The caller has a reference to the revoked capability still, but it's useless after revocation, because every call will return `Failure` with `RevokedException`. A caller who wants access will have to recover by asking for another capability.

Revocation is extremely flexible.  It's possible to revoke capabilities based on the number of times the capability is invoked.  Or after a fixed amount of time, or based on rule based suspicion.  

```scala
class JediMaster(jediPowers: JediCapability) {
  
  def cleanYourRoom(jedi: Jedi): Unit = {
    try {
      val Caretaker(revokerJediPowers, revoker) = Jedi.caretaker(jediPowers)
      // it shouldn't take more than 5 minutes to clean a room.
      after(5 minutes, () => revoker.revoke()) 
      user.useTheForceWisely(revokerJediPowers) 
    } finally {
      revoker.revoke()
    }
  }
}

val yoda = new JediMaster(force.jediPowers)
yoda.cleanYourRoom(luke)
```

Now, the program is safer on several levels.  Yoda only has Jedi Powers, can delegate only the powers he has, and can revoke delegation both on completion or on suspicion.

### Loggers 
 
```scala
object Foo {
  object Doer {
    // Note there's no external reference to the external FooCapability here
    def logger(provider: => Foo.Doer): Foo.Doer = new Foo.Doer {
      override def doTheThing(): Unit = {
        println("about to do the thing")
        provider.doTheThing()
      }
    }
  }
}

val foo = new Foo("foo")
val doer = foo.capabilities.doer
val loggingDoer = Foo.Doer.logger(doer)
```


### Attenuation

reducing a capability's scope of authority

Example: An OCap may have access to a computer's file system.

 Using attenuation, we can return an intermediary capability of just a particular file or subdirectory.
Read-only access
useful for packaging access to existing, non-capablity oriented world into capabilities
mediate access to network communications
limit connections to particular domains
allow applications to be securely distributed across datacenters without enabling them to talk to
arbitrary hosts on the internet – the sort of thing that would normally be regulated by firewall
configuration, but without the operational overhead or administrative inconvenience

### Abstraction

Packing lower level capabilities into more convenient APIs
example: package read-only file cap into an input steram object
example: Unix `passwd` command is an example of abstracting lower level details of file access and data formats

### Combination

Uses two or more capabilities together to create a new capability to some
specific joint functionality, or create something truly new
Example: In a Cap OS for mobile smartphones, having a combined capability composed of
 the authority to capture images with camera,
 the authority to obtain position with GPS,
  the authority to read system clock.
  
### Sealer/Unsealer

### Powerbox

A powerbox is the opposite of a sandbox.  Rather than isolating the system, it selectively grants short-lived authority by providing capabilities.

* http://wiki.c2.com/?PowerBox
* http://www.combex.com/papers/darpa-report/html/ (from DarpaBrowser)
* http://www.skyhunter.com/marcs/ewalnut.html (Powerbox Capability Manager section)
* https://docs.sandstorm.io/en/latest/developing/powerbox/
* https://github.com/sandstorm-io/sandstorm/blob/master/src/sandstorm/powerbox.capnp

So `bob.foo(carol)` means that `bob` has the reference to carol now.  In a membrane, we're not only wrapping
access to carol, but also to bob???

```
bob.foo(carol) => membrane(bob).foo(membrane(carol))
```

This doesn't make much sense, so let's use the jedi analogy again:

```
kylo.usePowersWisely(jediPowers)
```

Even if Kylo attempts to hand his Jedi powers off to everyone else, we can revoke them.

Every single parameter has to be wrapped in in something that is revoker.
So Int => Membrane[Int] etc.
This may be not workable in Scala, and I don't doubt you could bypass it.

## Affine Types

Affine Types are "use-once" types that  You can simulate affine types in Scala through isolated actors, but revoking a capability after first use will also do.

http://materials.dagstuhl.de/files/17/17051/17051.PhilippHaller.Slides1.pdf 
 

 
"object capability discipline" -- we can restrict types to match what we expect (is this part of membrane?) 
 
## Loader Isolation

Loader isolation figures into the original [Object Capability thesis](http://www.erights.org/talks/thesis/markm-thesis.pdf), but is at the level of the JVM rather than Scala.  The Java ClassLoader is discussed extensively in section 10.  The [Wikipedia article](https://en.wikipedia.org/wiki/Object-capability_model) says "A loader obeys _loader isolation_ if the new object's only initial references are from the explicitly provided state, with no implicit grants by the loader itself. The Java ClassLoader violates loader isolation, making confinement of unexamined loaded code impossible." but does not provide citation.  

That's technically true, but isolation in Java is still possible, even if it may not be complete.

This is important in Java, because if everything uses the same classloader, a new instance can be brought up through reflection.  Additionally, if a security loader is not enabled on the system, then private variables can be made accessible on the system and swapped out.  For an example, see [Monkeypatching Java Classes](https://tersesystems.com/blog/2014/03/02/monkeypatching-java-classes/).  You can enable the security manager, but it's very easy to disable unless you are [very careful about the sandbox](https://tersesystems.com/blog/2015/12/29/sandbox-experiment/).  And when you combine all of the above with the many gadgets available through [Java Deserialization](https://tersesystems.com/blog/2015/11/08/closing-the-open-door-of-java-object-serialization/), it's pretty clear that a dedicated attacker can circumvent the JVM.

Instantiating a new instance through reflection does not automatically put it in the chain of references, but it does allow a bypass of the Scala access modifier logic.  However, if the classloader doesn't have those classes at all, they can't be instantiated.

You break apart your public API classes and your data transfer objects from your implementation code.  Then you create another classloader, and register the SPI from there, enabling public access through the API without access to the underlying implementation.  In Java 9, you can use the module system, but that's still incredibly new and untried.

### Custom class loader

You load in projects through URLClassLoader, and use SBT BuildInfo.

```scala
lazy val core = project enablePlugins BuildInfoPlugin settings (
  buildInfoKeys := Seq(BuildInfoKey.map(exportedProducts in (`third-party`, Runtime)) {
    case (_, classFiles) ⇒ ("thirdParty", classFiles.map(_.data.toURI.toURL)) 
  })
```

```scala
def createInstance(): foo.bar.API = {
  val loader = new java.net.URLClassLoader(buildinfo.BuildInfo.thirdParty.toArray, parent)
  loader.loadClass("foo.bar.Impl").asSubclass(classOf[foo.bar.API]).newInstance()
}
```
  
https://stackoverflow.com/questions/29578808/using-a-custom-class-loader-for-a-module-dependency-in-sbt

### OSGI

If you're going down this route, you probably want an OSGI container like Apache Karaf, which can run bundles with strict container isolation.  OSGI lets you put the API into one bundle, and the implementation into another bundle.  Then use the OSGI service registry to construct the instance, by adding a ServiceFactory implementation and an instance of the API interface.  

It is really hard to find anything that uses both OSGI and sbt.  Phil Andrews has two projects which work, but almost everything else is set up to build an OSGI package externally and does not make use of the end product.

* https://github.com/PhilAndrew/JumpMicro
* https://github.com/PhilAndrew/sbt-osgi-felix-akka-blueprint-camel

Realistically speaking, it's probably OSGI in Java, or you set up a Manifest.MF file directly.  The simplest possible OSGI module is by [Michael Rice](https://michaelrice.com/2015/04/the-simplest-osgi-karaf-hello-world-demo-i-could-come-up-with/) with https://github.com/mrice/osgi-demo which can be used as a model.
 
// https://gist.github.com/mbedward/6e3dbb232bafec0792ba
// https://github.com/adamw/quicklens/blob/master/quicklens/src/main/scala/com/softwaremill/quicklens/QuicklensMacros.scala
// https://github.com/wix/accord/blob/master/core/src/main/scala/com/wix/accord/transform/ValidationTransform.scala
// http://www.strongtyped.io/blog/2014/05/23/case-class-related-macros/
// http://blog.scottlogic.com/2013/06/07/scala-macros-part-3.html
// http://blog.echo.sh/2013/11/04/exploring-scala-macros-map-to-case-class-conversion.html
// https://github.com/echojc/scala-macro-template
// https://blog.scalac.io/2016/05/26/simple-types-in-play.html
// https://stackoverflow.com/questions/25188440/applying-type-constructors-to-generated-type-parameters-with-scala-macros




# Other works
 
## Capability Patterns Papers

* [What are Capabilities](https://github.com/GravityNetwork/Gravity/wiki/What-are-Capabilities)
* [Analysing Object-Capability Patterns With Murϕ](https://cseweb.ucsd.edu/~dstefan/pubs/stefan:2011:ocap.pdf)
* http://erights.org/talks/thesis/index.html
* [Analysing the Security Properties
 of Object-Capability Patterns](http://www.cs.ox.ac.uk/files/3080/thesis-FINAL-15-07-10.pdf)
* [Patterns of Safe Collaboration](http://www.evoluware.eu/fsp_thesis.pdf)
* [Language and Framework Support for Reviewably-Secure Software Systems](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2012/EECS-2012-244.pdf)
* [Towards First Class References as a
 Security Infrastructure in
 Dynamically-Typed Languages](https://tel.archives-ouvertes.fr/file/index/docid/808419/filename/main.pdf)
* [Capability Myths Demolished](http://srl.cs.jhu.edu/pubs/SRL2003-02.pdf)
* [Robust Composition:
   Towards a Unified Approach to Access Control and Concurrency Control](http://www.erights.org/talks/thesis/markm-thesis.pdf)
* [LaCasa PDF](http://www.csc.kth.se/~phaller/doc/haller16-oopsla.pdf)
* [Emily](http://www.hpl.hp.com/techreports/2006/HPL-2006-116.pdf)
* [The Structure of Authority: Why Security Is not a Separable Concern](http://www.erights.org/talks/no-sep/secnotsep.pdf)
* https://www.cs.ox.ac.uk/files/2690/AOCS.pdf
* https://classes.soe.ucsc.edu/cmps203/Winter12/static/markm-ocap.pdf
* https://types.cs.washington.edu/ftfjp2013/preprints/a6-Drossopoulou.pdf
* http://www.doc.ic.ac.uk/~scd/iFM_Caps.pdf
* http://www.erights.org/talks/asian03/paradigm-revised.pdf
* http://www.erights.org/elib/capability/deadman.html

Lightweight static capaiblities

basically treating types as capabilities.  This is "strongly" typed programming so the refined type library is already ahead.  There's nothing new here.

https://github.com/changlinli/types_presentation_slides/blob/v0.2.0/slides.md

* http://okmij.org/ftp/papers/lightweight-static-capabilities.pdf
* https://tfp2016.org/papers/TFP_2016_paper_22.pdf 
* http://lambda-the-ultimate.org/node/1635
* https://www.youtube.com/watch?v=Ankp-DtKFmI

## Blog Posts

* http://joeduffyblog.com/2015/11/10/objects-as-secure-capabilities/
* https://fsharpforfunandprofit.com/posts/capability-based-security/
* https://fsharpforfunandprofit.com/posts/capability-based-security-2/
* https://fsharpforfunandprofit.com/posts/capability-based-security-3/
* [Capability Patterns](http://www.skyhunter.com/marcs/ewalnut.html#SEC45)
* http://www.cap-lore.com/CapTheory/
* http://erights.org/elib/capability/index.html
* http://wiki.c2.com/?CapabilityOrientedProgramming
* https://github.com/dckc/awesome-ocap
* https://github.com/GravityNetwork/Gravity/wiki/Reading-List

## Capability Patterns

* [What are Capabilites?](http://habitatchronicles.com/2017/05/what-are-capabilities/) (MOST USEFUL INTRO)
* [Capability Patterns](http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns) (SO MUCH)
* https://github.com/hierophantos/capable
* http://wiki.c2.com/?FacetPattern
* http://wiki.c2.com/?CaretakerPattern
* http://wiki.c2.com/?TwoKindsOfCapabilities
* http://wiki.c2.com/?BuildSecurityAbstractionsIntoCapabilities

* https://blog.acolyer.org/2016/02/16/capability-myths-demolished/
