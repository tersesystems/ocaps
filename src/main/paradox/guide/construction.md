# Constructing Capabilities

The introduction gave a basic explanation of capabilities, but did not demonstrate why access to the capability is so important.

In esssence, a capability is a programming level expression of the saying "You can only eat your cake if you have it."  

```scala
trait Cake {
  def eat(): Unit
}
val cake = new Cake() {
  def eat() = ()
}
cake.eat()
```

You may know that a resource (an instance of `Cake`) exists, but you still need an object reference `cake` in order to affect the `Cake` instance by calling the `eat()` method on it.  This is a fairly trivial example, because if you can instantiate a cake yourself, you will have no problem referencing it.

Things start getting more interesting when you add more objects, and you don't control or instantiate resources directly.  For example, you may not be able to make cakes yourself, so you go to a bakery.

```scala
trait Bakery {
  def buy(): Cake 
}

class Person(val bakery: Bakery) {
  def buyAndEatCake() = eatCake(bakery.buy())
  def eatCake(cake: Cake) = cake.eat()    
}
val bakery = new Bakery() {
  def buy(): Cake = new Cake() { 
    def eat() = ()
  }
}
val you = new Person(bakery)
you.buyAndEatCake()
```

Here, you're still interested in affecting a resource `Cake` by, well, eating it, but you don't have direct access to the cake.  Instead, `you`, an instance of a `Person`, have a reference to a `Bakery`, and that `Bakery` may give you a cake. 

Note that because `bakery` is an object reference, it is also a capability.  You can't buy things from a bakery unless you know how to get to one.
 
The bakery may give you a cake.  But it may throw an exception, because they don't have cakes or you don't have enough money, or because they're closed.  In addition, you don't control the behavior of the `eat` method.  You have a reference, but that reference is a contract and it's the object that chooses how to execute it -- when you call `cake.eat()`, that may also throw an exception or do something behind the scenes.  

And this is where the power of capabilities comes in.  As it stands, you can eat a cake as often as you like.  The bakery may want you to only eat a cake once.  Because the bakery controls access to the resource, it can enforce a capability policy around the eating of cake.

```scala
val enforcingBakery = new Bakery() {
  def buy(): Cake = {
    val latch = new java.util.concurrent.atomic.AtomicInteger(1)
    val realCake = new Cake() {
      def eat() = ()
    }
    new Cake() {
      def eat() = {
        if (latch.getAndDecrement == 1) {
          realCake.eat()
        } else {
          throw new RuntimeException("You have already eaten this cake!")
        }
      }
    }
  }
}

val sneakyYou = new Person(enforcingBakery)
val oneTimeCake = sneakyYou.bakery.buy()
sneakyYou.eatCake(oneTimeCake)
sneakyYou.eatCake(oneTimeCake)
```

You may get an object reference -- a capability -- to a `Cake`.  But here, the actual cake is only accessible through a delegate (also called a "forwarder") which proxies the call.  You have no direct access to the resource (`realCake`), and you can affect the resource (call `realCake.eat()`)  only if the proxy allows it.  The proxy may refuse to proxy the call.  This is called *revocation*, and it is a key part of capability-based systems.

So if the only way you can get a cake is through a bakery, then you have to play by the bakery's rules.

## Capabilities as Traits

Because capabilities are references, referring to a `Cake` class is perfectly legal.  However, given the nature of capabilities as a security primitive, implementing a capability as a class limits functionality, because it is trickier to implement behavior that usually requires forwarding proxies, such as *revocation* and *composition*.  

In most cases, using a pure trait with no behavior is the safest choice.  For example, rather than having a reference to a `Cake` class, implementing an `Eatable` trait that contains a method `eat()` would be more convenient as a capability, and then `Cake` can implement that interface:

```scala
trait Eatable {
  def eat()
}

class Cake extends Eatable {
  ...
}
```

Now, we know that we can `eat` the cake, because it is `Eatable`.

@@@note

Avoid default methods and default parameters on traits. They add behavior, and interfere with `ocaps` macros.

@@@

Note that `Eatable` is a Single Abstract Method (SAM) trait.  Capabilities are not required to only implement a single method, but it can make things more convenient.  An example of a capability that could implement several methods would be a `FileReader` which could return either a byte-oriented stream, or a character oriented stream:

```scala
trait Reader {
  def bufferedReader[T](block: BufferedReader => T): T

  def inputStream[T](block: InputStream => T): T
}
```

Note that only a `BufferedReader` or an `InputStream` are returned here.  This is because exposing a `java.io.File` object can lead to manipulation of the filesystem, causing the capability to "leak" information and expose additional privileges that were not explicitly granted.  
 
@@@ warning 
 
Be careful that your capability does not leak extra functionality!

@@@

### Traits vs Anonymous Functions

There's an argument that anonymous functions such as `FunctionN` are natural capabilities.

Anonymous functions are good for many things, but they have the following drawbacks compared to explicit traits:

* Anonymous functions are anonymous.  One `() => ()` looks much the same as another, so it's harder to track where a capability is exposed through IDE.
* Anonymous functions are opaque.  When you see an `Eatable`, you know what it does.  A `() => ()` method could do anything.
* Anonymous functions are not refactorable.  If you have an `Eatable` trait, you can add more methods to it, subtype, change the name where appropriate.
  
A similar argument applies to `java.lang.Runnable` and other SAM traits that are part of the Java platform: they are not part of the domain, and they are intended for different purposes.  It's always possible to turn a SAM trait into an anonymous function when necessary, but the reverse is not possible.  

If a drop in is required, then extending an anonymous function type is acceptable, i.e.

```scala
trait Doer extends (String => Unit)
```

Although personally I still don't like this, because subclassing means that you can still downcast to `Function1` and lose the trait information.
  
## Construction Through Composition

Let's dig a little deeper and discuss capabilities in the context of a [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) based repository, and show how resources and capabilities interact, and how to expose capabilities.

> It's worth noticing that because a capability is an object reference, a resource which has an exposed capability is always referencable and so will not be eligible for garbage collection. In certain cases, we have many capabilities to resources which are no longer valid, such as cached items, or may have open filehandles.  You should use or `ocaps.WeakResource` to wrap access to a resource in a [weak reference](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html).

When you think of a repository, you usually think of something like the following:

```scala
import scala.concurrent._
case class Item(id: UUID, name: String)
trait ItemRepository {
  def create(item: Item): Future[Item]
  def update(item: Item): Future[Item]
  def delete(id: UUID): Future[Unit]
  def find(id: UUID): Future[Option[Item]]
}
```

However, recall the first rule: if you have a cake, you can eat it.  If you have access to an `ItemRepository`, you can call all of the methods available on `ItemRepository`.  If a class just wants to query for an item, it doesn't need the ability to create, update or delete that item. 

```scala
case class Item(id: UUID, name: String)
object ItemRepository {
  trait Finder {
    def find(id: UUID): Future[Option[Item]]
  }
  trait Updater {
    def update(item: Item): Future[Item]
  }
  trait Deleter {
    def delete(id: UUID): Future[Unit]
  }
  trait Creator {
    def create(item: Item): Future[Item]
  }
}
trait ItemRepository 
  extends ItemRepository.Finder 
  with ItemRepository.Updater
  with ItemRepository.Creator
  with ItemRepository.Deleter
```

Here, we've broken down the individual methods of the `ItemRepository` into a series of traits.  Rather than exposing the `ItemRepository` itself, we can expose the traits individually, and build up the repository as a *composition* of capabilities.  

```scala
val item = Item(UUID.randomUUID(), "item")
val repo = new ItemRepository() {
  def create(item: Item): Future[Item] = Future.successful(item)
  def update(item: Item): Future[Item] = Future.successful(item)
  def delete(id: UUID): Future[Unit] = Future.successful(())
  def find(id: UUID): Future[Option[Item]] = Future.successful(item)
}

val finder = new ItemRepository.Finder() {
  def find(id: UUID): Future[Option[Item]] = repo.finder(id)
}
```

When we have a pattern like this, where capabilities have a shared underlying resource, we say that they are facets of the resource.  In this case, we have a `finder` facet of the `ItemRepository` resource. 

You would think that since `ItemRepository` implements the `Finder` trait itself, that you could just pass it around directly.  The problem there is that an object reference is still an object reference **no matter what type you give it**.  As such, you can get access to the other facets by downcasting to `ItemRepository`:

```scala
val onlyAFinder: ItemRepository.Finder = repo
val recoveredRepo: ItemRepository = onlyAFinder.asInstanceOf[ItemRepository]
recoveredRepo.delete(ID) // oh dear, we only wanted to give you a finder.
```

So instead, we create a new `Finder` instance, and proxy through to `repo.finder`.  This is called *attenuation*.   You can use an ocaps macro to write this code for you automatically:

```scala
val attenuatedFinder: ItemRepository.Finder = ocaps.macros.attenuate[ItemRepository.Finder](repo)
```

Facets do not have to be created through attenuation, and in some cases it's undesirable because it fixes traits onto the resource.  We'll show another way facets can be created later in this section.
 
Now we have the facet, we can use it in other operations:

```scala
class NameChanger(finder: ItemRepository.Finder) {
  def findName(id: String): Future[String] = finder.find(id).map { maybeItem =>
    maybeItem.map(_.name)
  }
}
```

## Effects in Capabilities with Tagless Final

Rather than throwing exceptions as a side effect, capabilities can make use of functional programming techniques. For example, rather than commit to a `monix.Task[_]` or a `Try[_]`, a capability may be expressed in [tagless final](https://www.beyondthelines.net/programming/introduction-to-tagless-final/) style with a type parameter `F` that is a standin for the thing you want:   

```scala
object ItemRepository {
  trait Finder[F[_]] {
    def find(id: UUID): F[Option[Item]]
  }
}
```

We refer to this as an effect, where effect means "whatever distinguishes `F[A]` from `A`."

The `capabilities` companion is defined using `cat.Id`, which essentially means "no effect here":

```scala
import cats._
class ItemRepository {
  import ItemRepository._
  private val items = Seq(Item(UUID.randomUUID(), "item name"))
  private object capabilities {
    val finder: Finder[Id] = new Finder[Id]() {
      override def find(id: UUID): Id[Option[Item]] = items.find(_.id == id)
    }
  }
}
```

And then the effect -- in this case `Try` -- can be wrapped around the original capability instance.

```scala
val idFinder = access.finder(itemRepository)
val tryFinder: Finder[Try] = new Finder[Try] {
  override def find(id: UUID): Try[Option[Item]] = Try(idFinder.find(id))
}
```

When using `Try`, the `find` method will return either `Success(maybeItem)` or `Failure(throwable)` as a result, rather than throwing an exception up the stack.

## Construction Through Access

Creating a resource which exposes all its public methods is very convenient in situations where direct access to the resource is tightly restricted, and all external access is regulated through facets.  This is not always the case: for example, in an Inversion-of-Control container like Spring or Guice, all resources are accessible, i.e.

```scala
val repoThroughGuice = injector.instanceOf(classOf[ItemRepository])
repoThroughGuice.delete("1") // anyone can delete
```

In this situation, it may be desirable in some cases to have a resource with public methods, and a "protected" set of capabilities that are not accessible:

```scala
trait ItemRepository {
  def name: String  // okay for anyone to access this!
  
  private[???] def deleter: Deleter // only accessible to authorized objects, if we knew what ??? was
}
```

There is a way to implement this!  In Scala, only the public methods on an object are exposed -- private methods cannot be referenced externally.  We can use Scala's [access modifiers and qualifiers](http://jesperdj.com/2016/01/08/scala-access-modifiers-and-qualifiers-in-detail/) to selectively expose capabilities.  We'll demonstrate in this section.

So, say you have a class `Document`, with a private `Path` that contains the text of the document.

```scala
import java.nio.file._
final class Document(private[this] val path: Path) {
    ...
}
```

We want to restrict access so that we can read from the document without exposing the path.

First, we create a companion object `Document` and add a `NameChanger` trait:

```scala
object Document {
  trait Reader {
    def bufferedReader[T](block: BufferedReader => T): T
  }
}
```

Next, we create a private singleton object called `capabilities` and add a `reader` implementation:

```scala
final class Document(private[this] val path: Path) {
  private object capabilities {
    val reader: Document.Reader = new Document.Reader {
      override def bufferedReader[T](block: BufferedReader => T): T = {
        val reader = Files.newBufferedReader(Document.this.path, StandardCharsets.UTF_8)
        try {
          block(reader)
        } finally {
          reader.close()
        }
      }
    }
  }
}
```

Finally, we add an `Access` class that can expose the `Reader` for a particular `Document` instance:

```scala
object Document {
  final class Access private {
    def reader(doc: Document): Reader = doc.capabilities.reader
  }
  object Access {
    private val instance = new Access()
    def apply(): Access = instance
  }
}
```

In order to have a reference to a `reader`, you must have access to both a `Document.Access` instance and a `Document`:

```scala
val path = Path.get("document.txt")
val document = new Document(path)
val access = Document.Access()
val reader = access.reader(document)
val text = reader.bufferedReader(_.readLine())
```

When you put two capabilities together to expose functionality that is more than the individual pieces, it is called *amplification*.  Other useful examples of *amplification* are when a "can" and a "can opener" are put together, or a "private key" and "document encrypted with public key."

Creating an `Access` class can also be useful because it can mediate between a resource and the capabilities on the resource, without directly involving the resource itself.  For example, referring back to the effects section above, a `TryAccess` class may provide a `Try` effect on all the capabilities of the `Document`.

```scala
final class TryAccess private {
  def reader(doc: Document): Reader[Try] = Try(doc.capabilities.reader)
}
```

Or you can go for a full-on type class approach:

```scala
object Document {
  trait NameChanger[F[_]] {
    def changeName(name: String): F[Unit]
  }

  trait WithEffect[C[_[_]], F[_]] {
    def apply(capability: C[Id]): C[F]
  }

  // The default "no effect" type Id[A] = A
  implicit val idEffect: NameChanger WithEffect Id = new WithEffect[NameChanger, Id] {
    override def apply(capability: NameChanger[Id]): NameChanger[Id] = identity(capability)
  }

  // Apply a "Try" effect to the capability
  implicit val tryEffect: NameChanger WithEffect Try = new WithEffect[NameChanger, Try] {
    override def apply(capability: NameChanger[Id]): NameChanger[Try] = new NameChanger[Try] {
      override def changeName(name: String): Try[Unit] =  Try(capability.changeName(name))
    }
  }

  class Access {
    def nameChanger[F[_]](doc: Document)(implicit ev: NameChanger WithEffect F): NameChanger[F] = {
      val effect = implicitly[NameChanger WithEffect F]
      effect(doc.capabilities.nameChanger)
    }
  }
}

val idNameChanger = access.nameChanger[Id](document)
```

This `Access` class constructor is public here, but obviously anyone who has both `Access` and a resource will be able to expose capabilities, so you should protect your access appropriately.  You can use dynamic sealing or Scala access modifiers to ensure access is tightly controlled.

Note that these are "root level" capabilities -- they are non-revocable, "private key" object references, so you typically want to wrap these in `ocap.Revocable` and hand out the revocable capability (aka caretaker) instead.
