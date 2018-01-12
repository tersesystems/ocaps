@@@section { data-background="#15a9ce" }

### Capabilities in Scala

##### Will Sargent

@@@

@@@section

Agenda:

1. What are Capabilities?
1. Using Capabilities
1. Combining Capabilities
1. Capabilities and Functional Programming

@@@

@@@section

# 1. What are Capabilities?

@@@

@@@section

* A capability is a security primitive that grants authority by reference.

@@@

@@@section

## Defining Authority

* <b>Resource</b>: an object of interest.
* <b>Effect</b>: ability to query/modify state and/or computation on a resource.
* <b>Authority</b>: sufficient cause for an effect on a resource.
* An object has authority over a resource if it can effect it.

@@@@notes

Permission and Authority Revisited towards a formalisation, Sophia Drossopoulou
Principal of Least Authority says "be careful when dispensing power"

@@@@

@@@

@@@section

## Defining Capability

* <b>Capability</b>: a security primitive that grants authority by reference.
* Put another way, a capability is a reference to an object that has authority over a resource.
* "security primitive" because capabilities are typically stuck together in interesting ways.

@@@

@@@section

## Example 

* We have a `User` object, which has a private `name` field.
* We want to change the name.

@@@

@@@section

### Definition

@@snip[Before.scala]($root$/src/main/scala/simple/Before.scala){#definition}

@@@

@@@section

### Trivial Case

* In the trivial case, the object reference to `user` is a capability on its public API.
* If you have reference to a `val user: User`...
* ...then you have authority to call `user.changeName()`
* ...but what if you only want `changeName` itself?

@@@

@@@section

### Defining Capability

* `User` makes `changeName` private, preventing external effect.
* `User` defines inner type `NameChanger` with authority over `name`.
* Access to `NameChanger` instance means you are <b>capable</b> of changing name.
* We therefore call `NameChanger` a <b>capability</b>.

@@@

@@@section

### Example

@@snip[After.scala]($root$/src/main/scala/simple/After.scala){#definition}

@@@

@@@section

### Access

@@snip[After.scala]($root$/src/main/scala/simple/After.scala){#access}

@@@

@@@section

### Usage

@@snip[After.scala]($root$/src/main/scala/simple/After.scala){#usage}

@@@

@@@section

### Why is this useful?

* Isolation: `NameChanger` != `AgeChanger`
* Type Safe: capabilities are types
* Runtime Control: object references are dynamic
* Capability Operations!

@@@

@@@section

## Capability Operations

* Revocation ("a = null")
* Amplification ("ab = a + b")
* Attenuation ("a = ab - b")
* Modulation ("a = { pre(); a; post(); }")

@@@

@@@section

### Revocation

* Access to a capability can be through a delegate with conditional access
* <b>Caretaker</b>: contains delegate and `Revoker`
* <b>Revoker</b>: `revoker.revoke()` destroys access

@@@

@@@section

#### Caretaker

@@snip[Main.scala]($root$/src/main/scala/revocation/Main.scala){#caretaker}

@@@

@@@section

#### Manual

```
val rawDoer = policy.askForDoer(foo)
val caretaker = Foo.Doer.caretaker(rawDoer)
val revocableDoer = caretaker.capability
revocableDoer.doTheThing()
caretaker.revoker.revoke()
```

@@@

@@@section

#### Macro

```scala
final class DocumentPolicy private {
  def reader(doc: Document): Reader = doc.capabilities.reader
  def writer(doc: Document): Writer = doc.capabilities.writer
  def deleter(doc: Document): Deleter = doc.capabilities.deleter
}
val Caretaker(docReader, revoker) = caretaker[Reader](policy.reader(doc))
```

@@@

@@@section

### Amplification

* You can stick two capabilities together by creating a new anonymous class implementing capability traits and delegating through all of them.
* This can be elided by using the `amplify` Scala macro.

@@@

@@@section

@@snip[Main.scala]($root$/src/main/scala/amplification/Main.scala){#usage}

@@@

@@@section

### Attenuation

* Attenuation is the reverse of amplification, by creating a delegate with less authority over the domain object.
* Note that you cannot "downcast" an object as it still has the same underlying class
* Use `attenuate` Scala macro here :-)

@@@

@@@section

#### Unsafe Casting

@@snip[Main.scala]($root$/src/main/scala/attenuation/Main.scala){#unsafe}

@@@

@@@section

#### Safe Attenuation

@@snip[Main.scala]($root$/src/main/scala/attenuation/Main.scala){#safe}

@@@

@@@section

### Modulation

* Modulation is standard proxying with additional wrapper behavior
* Has the greatest amount of flexibility, but requires some thought
* We'll show the manual and macro based versions

@@@

@@@section

#### Manual

@@snip[Main.scala]($root$/src/main/scala/modulation/Main.scala){#manual}

@@@

@@@section

#### Counter

@@snip[Main.scala]($root$/src/main/scala/modulation/Main.scala){#countable}

@@@

@@@section

#### Timer

@@snip[Main.scala]($root$/src/main/scala/modulation/Main.scala){#timer}

@@@

@@@section

#### Logging

@@snip[Main.scala]($root$/src/main/scala/modulation/Main.scala){#logging}

@@@

@@@section

## Capabilities and Functional Programming

* Capability != FP
* A capability looks like a function, but does not have to be!
* Any capability could throw `RevokedException` or similar
* A capability can has "lifecycle", functions typically don't

@@@

@@@section

### Anonymous Capabilities via Lifting

```scala
class User(private val name: String) {
  private def changeName(name: String): Unit = ...
  private object capabilities {
    val nameChanger: (String => Unit) = User.this.changeName _
  }
}
```

@@@

@@@section

### Issues with Anonymous Capabilities

* You can seal a trait.  You can't seal a `(String => Unit)`
* No strong typing with compiler
* No semantic information to `String => Unit`

@@@

@@@section

### May Have Multiple Methods

```scala
trait Reader {
  def bufferedReader[T](charset: Charset)(block: BufferedReader => T): Try[T]
  def inputStream[T](options: Seq[OpenOption])(block: InputStream => T): Try[T]
}
```

@@@

@@@section

### Effects

* Note the `Try[T]` in the previous reader: this is a blocking IO operation
* What if we want to return an asynchronous `Future[T]` or an `Option[T]`?
* "Effect" is a vague term: effects != side-effects
* [Capabilities and Effects](http://homepages.ecs.vuw.ac.nz/~alex/files/CraigPotaninGrovesAldrichOCAP2017.pdf)

@@@@notes

 * Using a capability means that sometimes you're using a capability that
 * is proxied through a caretaker, and can be revoked.  When it is revoked,
 * any call out to the capability will die with a RevokedException.
 *
 * If you are writing in an FP style, you want to avoid using try/catch blocks,
 * and instead would rather return an [[Either]] or a [[Try]] instead.  But you
 * don't want to constrain the underlying capability to a particular Monad.
 *
 * The easiest way to do this is to use a "tagless final" algebra to wrap the
 * capability methods in a to-be-determined "F[_]", so that instead of returning
 * "Foo", you return "F[Foo]" and can decide later what you want F to be.  You
 * can define F to be a Monad, and call "flatMap" to compose capabilities without
 * breaking this abstraction.
 
@@@@

@@@

@@@section

### Tagless Final with Cats Effect

* Use type parameter `F[_]` on class to return `F[Foo]`
* Wrap in Cats Effect `IO` monad to defer execution and ensure stack safety

@@@

@@@section

#### Repository Definition

@@snip[Main.scala]($root$/src/main/scala/tryinator/Tryinator.scala.txt){#repository}

@@@

@@@section

#### Functional Composition with Monad

@@snip[Main.scala]($root$/src/main/scala/tryinator/Tryinator.scala.txt){#monad}

@@@

@@@section

@@@section

## Questions!

@@@