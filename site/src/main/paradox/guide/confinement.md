# Confining Capabilities

Managing capabilities in the context of an architecture can be complex. 

Design patterns of capability patterns can be hard to manage.  One way to package capabilities is to use *composition*, but it may also make sense to organize capabilities in a tree structure connected to resources.

> [I]f your abstraction requires 15 distinct capabilities to get its job done, does its constructor take a flat list of 15 objects? What an unwieldy, annoying constructor! Instead, a better approach is to logically group these capabilities into separate objects, and maybe even use contextual storage like parents and children to make fetching them easier. -- [Objects as Secure Capabilities, Joe Duffy](http://joeduffyblog.com/2015/11/10/objects-as-secure-capabilities/)

One issue is that managing capabilities can mean limiting scope and access to an object reference, but many architectures assume a context in which all object references are publically accessible -- for example, events may be sent across an event bus, or through a streaming model.  And because data structures inherently involve object references, there's the question of how to keep capabilities safe and isolated.

We touched on this a little bit in the `Access` class which uses access modifiers and a companion object to construct capabilities which cannot be accessed directly.  Unfortunately, in Scala, [access modifiers and qualifiers only apply to enclosing scope](http://www.jesperdj.com/2016/01/08/scala-access-modifiers-and-qualifiers-in-detail/), so there is no way to qualify private access to things outside that scope.  However, there is a way to protect capabilities to produce this effect -- through the use of `Brand`.

## Encapsulating Capabilities with Brands

A Brand is a means of "[providing data abstraction in the absence of static typing](https://people.mpi-sws.org/~dreyer/papers/ocpl/paper.pdf)."  It consists of a sealing function, which provides a "boxed" reference to the capability **which cannot be opened**, and an unsealing function, which takes the box, and returns the contained capability. The best way of describing it is that a Brand provides both a canner and the associated can opener.

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
    println(s"food = ${canOfSpam.food.toString}") // DOES NOT WORK
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

Because dynamic sealing is

Signing

Encryption

Private Channel

Fuck yes, this is awesome.

https://blog.acolyer.org/2016/10/19/protection-in-programming-languages/




I think there is a use case I'm not accounting for -- in an Akka actor system hierarchy, you could have multiple actors that were all of the same type, but implement dynamic sealing to provide the same kind of "dynamic private" access privacy when passing messages -- so the parent may have the unsealer but the children do not.  Is this more in line with how sealers are used in practice?

Okay, so https://people.mpi-sws.org/~dreyer/papers/ocpl/paper.pdf says 

> We now consider one of the oldest and most influential OCPs: dynamic sealing, also called the sealer-unsealer pattern. Originally proposed by Morris [1973], dynamic sealing makes it possible to support data abstraction in the absence of static typing. In this section, we show how OCPL supports compositional reasoning about dynamic sealing. In particular, we show how to implement dynamic sealing in HLA and how to give a compositional specification for this implementation, from which we derive useful specifications for interesting abstractions built on top of it and prove robust safety for representative clients. (We consider an alternative implementation of the OCP, satisfying a slightly weaker specification, in Appendix A. We also show how ideal specifications for cryptographic signing and encryption primitives can be derived from the specification of dynamic sealing in Appendix B.) 

> The functionality of dynamic sealing. Morris [1973] introduced dynamic sealing to enforce data abstraction while interoperating with untrusted, potentially ill-typed code. He stipulated a function makeseal for generating pairs of functions (seal, unseal), such that (i) for every value v, seal v returns a value v ′ serving as an opaque, low-integrity proxy for v; and (ii) for every value v ′ , unseal v ′ returns v, if v ′ was produced by seal v, and otherwise gets stuck. The key point is that this seal-unseal pair supports data abstraction: the client of these functions can freely pass sealed values to untrusted code since they are low-integrity, while at the same time imposing whatever internal invariant it wants on the underlying values that they represent.

Paper on sealers (which does not actually have that word in it, only the operations Seal and Unseal) http://www.erights.org/history/morris73.pdf

https://ai.google/research/pubs/pub45568

http://www.cis.upenn.edu/~bcpierce/papers/infohide3.pdf

https://blog.acolyer.org/2016/10/19/protection-in-programming-languages/

## Confining Operations with Membranes

A membrane is an extension of a `Revocable` that transitively imposes revocability on all
references exchanged via the membrane.

* [What is a Membrane?](http://blog.ezyang.com/2013/03/what-is-a-membran/)
* [Trustworthy Proxies: Virtualizing Objects with Invariants](https://research.google.com/pubs/pub40736.html)
* https://web.archive.org/web/20160408162552/http://www.eros-os.org/pipermail/e-lang/2003-January/008434.html

Membranes are supposed to stop messages passing from one place to another without being wrapped.

The generalized way to implement a membrane in Java and Scala is to use an `InvocationHandler` as a dynamic proxy.

* http://www.baeldung.com/java-dynamic-proxies
* https://www.concretepage.com/java/dynamic-proxy-with-proxy-and-invocationhandler-in-java

There are two ways to implement membranes in a type safe way.

The first is to have a membrane class which will wrap everything, and have the membrane act as an effect.  This does require classes to "opt in" to the membrane.

The second is to make all capabilities take an implicit membrane context.

* [Capability confinement by membranes](https://www.info.ucl.ac.be/~pvr/rr2005-03.pdf)

TODO

// http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns#Membranes
// http://blog.ezyang.com/2013/03/what-is-a-membran/
