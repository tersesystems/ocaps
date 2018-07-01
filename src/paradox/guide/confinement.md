# Confining Capabilities

Managing capabilities in the context of an architecture can be complex. 

One issue is that managing capabilities can mean limiting scope and access to an object reference, but many architectures assume a context in which all object references are publically accessible -- for example, events may be sent across an event bus, or through a streaming model.  And because data structures inherently involve object references, there's the question of how to keep capabilities safe and isolated.

We touched on this a little bit in the `Access` class which uses access modifiers and a companion object to construct capabilities which cannot be accessed directly.  Unfortunately, in Scala, [access modifiers and qualifiers only apply to enclosing scope](http://www.jesperdj.com/2016/01/08/scala-access-modifiers-and-qualifiers-in-detail/), so there is no way to qualify private access to things outside that scope.  However, there is a way to protect capabilities to produce this effect -- through the use of `Brand`.

## Encapsulating Capabilities with Dynamic Sealing

Dynamic sealing is a means of "[providing data abstraction in the absence of static typing](https://people.mpi-sws.org/~dreyer/papers/ocpl/paper.pdf)."  It consists of a sealing function, which provides a "boxed" reference to the capability **which cannot be opened**, and an unsealing function, which takes the box, and returns the contained capability. The best way of describing it is that dynamic sealing provides both a canner and the associated can opener.  

In `ocaps`, this is done by using an `ocaps.Brand`, which contains the sealer and unsealer.

```scala
import ocaps.Brand

object Main {
  case class Food(name: String)
  case class Can(food: Brand.Box[Food])
  class CanOpener(unsealer: Brand.Unsealer[Food]) {
    def open(can: Can): Food = {
      unsealer(can.food).get // throws exception if we did not seal this
    }
  }

  def main(args: Array[String]): Unit = {
    val (sealer, unsealer) = Brand.create[Food]("canned food").tuple
    val canOfSpam: Can = Can(sealer(Food("spam")))

    val cannedFood: Brand.Box[Food] = canOfSpam.food
    println(s"food = ${cannedFood.toString}") // DOES NOT WORK
    val canOpener = new CanOpener(unsealer)

    // We need both Can and CanOpener to unseal.
    def openCan(can: Can, canOpener: CanOpener) = {
      val food: Food = canOpener.open(can)
      println(s"food = $food") // WORKS
    }

    openCan(canOfSpam, canOpener)
  }
}
```

Because both an unsealer and the sealed item are necessary to unseal, the act of unsealing is inherently a case of *amplification*, where putting two capabilities together lets you do things that neither one could do individually.

Sealers can be used for many things.  It's helpful to think of them in the context where you would use public key encryption, for example

* *Signing* -- a box can be passed around and an unsealer on a resource can be made public, attesting to its origin.
* *Encryption* -- a public sealer can "encrypt" information by boxing it, and sending it to an actor with the sealer -- in effect, the sealer is the public key.
* *Assurance* -- boxed information can be passed out and round tripped to the original source, ensuring that the information has not been modified in transit.
* *Private Channel* -- two actors can pass information to each other using sealed boxes, ensuring that sensitive information is not exposed even if the message is intercepted or `LoggingReceive` is enabled.

An example of signing and assurance is in representing responsibility for operations, as shown in [HORTON](http://www.erights.org/elib/capability/horton/).

One possible application of dynamic sealing is that all `Access` objects can be sealed and safely bound in a dependency injection framework, and a revocable unsealer capability can be passed around to enable access from a central gatekeeper.  I still need to implement this, but I think it's fairly straightforward.

```scala
// import scalaguice so we don't use new TypeLiteral[Box[Foo.Access]] {}
class Module extends ScalaModule {
  def configure() = {
    bind[Box[Foo.Access]]
     .toProvider(fooAccessProvider)
     .in(Scopes.SINGLETON)
  }
}
```

This can also be used to pass around "root level" objects such as database connections and JSSE key managers that you may want to restrict access to generally.  Anyone using the database connection will have to unseal it, and you can provide the unsealer capability to use revocation or logging to flush it out.

Please see the @ref:[Dynamic Sealing example](../examples/dynamic_seal.md) for a demonstration.

Dynamic sealing has an independent lineage from capabilities programming.  The original paper, [Protection in Programming Languages](http://www.erights.org/history/morris73.pdf) was written in 1973,  However, the morning paper [summary on the paper](https://blog.acolyer.org/2016/10/19/protection-in-programming-languages/) is actually clearer and better written than the paper itself.

[Robust and Compositional Verification of Object
 Capability Patterns](https://people.mpi-sws.org/~dreyer/papers/ocpl/paper.pdf) has a section on dynamic sealing.
 
The discussion on the use of dynamic sealing in communication channels is from [Modules, Abstract Types, and Distributed Versioning](https://www.cl.cam.ac.uk/~pes20/versions-popl.pdf).

## Confining Operations with Membranes

A membrane is an extension of a `Revocable` that transitively imposes revocability on all references exchanged via the membrane.  

Membranes are supposed to stop messages passing from one place to another without being wrapped.  They are most commonly used for ["uncooperative revocation"](https://web.archive.org/web/20160408162552/http://www.eros-os.org/pipermail/e-lang/2003-January/008434.html), although any effect can be applied with a membrane, not just revocation.

Membranes are useful in a situation in which you have to run some foreign code in a sandbox, and you absolutely do not trust it.  Tellingly, the papers above implement membranes using Javascript, where any website can tell code to be run locally in the browser.  Mozilla uses capabilities heavily internally, and membranes ensure an airgap between the browser's internal code and the code available to the site Javascript.

The JVM is not a great platform for implementing a completely safe membrane in the face of an attacker who can run executable bytecode.  The JVM SecurityManager is [easily subverted](https://tersesystems.com/blog/2015/12/29/sandbox-experiment/), and [Java Serialization attacks](https://tersesystems.com/blog/2015/11/08/closing-the-open-door-of-java-object-serialization/) mean that the boundary is very hard to enforce.  After RCE has been achieved, [monkeypatching Java classes](https://tersesystems.com/blog/2014/03/02/monkeypatching-java-classes/) is easily accomplished through `setAccessible`.

However, if the assumption is made that attackers cannot run their own code, the best way to implement a membrane in Java would be to implement a generic method interceptor in [Byte Buddy](http://bytebuddy.net/#/) and attach behavior outside of the context of "normal" Java code.

However, because membranes can implement any effect, and because membranes have a strong conceptual affinity with some FP concepts, it's actually very easy to implement "co-operative revocation" using a dependently typed effect.  This is best called a "permeable membrane", because it is an opt-in system that only works if the effect is propagated.

Please see the @ref:[Membrane example](../examples/membrane.md) for details how Membrane is used in `ocaps`.

The clearest layman explanation of membranes is [What is a Membrane?](http://blog.ezyang.com/2013/03/what-is-a-membran/) by Edward Z. Yang.  

[Trustworthy Proxies: Virtualizing Objects with Invariants](https://research.google.com/pubs/pub40736.html) is an implementation of membranes in Javascript, although somehow the abstract completely avoids that word.