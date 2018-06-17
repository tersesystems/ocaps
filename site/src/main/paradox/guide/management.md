
# Managing Capabilities

Management of capabilities is based around the concepts of *revocation* and *modulation*: being able to stop a capability that has already been assigned, and being able to augment the behavior of capabilities for security purposes.

### Managing Accessiblity with Revocation

This naturally brings up the question of how to limit access to an already granted capability, for example to limit the lifespan of a capability to the lifespan of a user session, so the capability is no longer valid after logout or timeout.  This is done using *revocation*, but more importantly, this is done using a whole series of constructs based around *revocation*, the most important being the `Revocable` pattern, also known as the Caretaker.

We've shown an example of *revocation* in the bakery above, but revocation as a concept is important enough that it has a specific trait associated with it -- a `Revoker`:

```scala
package ocaps

trait Revoker {
  def revoke(): Unit
}
```

A revoker does one thing -- it revokes access to a proxied capability.  When the `revoke` method is invoked, the proxy will no longer forward calls to the capability, and will instead throw a `RevokedException`.

Of course, a `Revoker` does no good without something to revoke.

The `Revocable` handles the creation of a revoker and associated proxy.  The apply method of the `Revocable` is implemented as follows:

```scala
package ocaps

object Revocable {
  def apply[C](capability: C)(cblock: (() => C) => C): Revocable[C] = {
    val (thunk, revoker) = Revoker.pair(capability)
    val revocableCapability: C = cblock(thunk)
    Revocable(revocableCapability, revoker)
  }
}
```

The `cblock` of the Revocable is a constructor block -- it passes in a capability thunk, and returns the proxy implementation.

```scala
import ocaps._

trait Doer {
  def doTheThing(): Unit
}

object Doer {
  def revocable(doer: Doer): Revocable[Doer] = {
    Revocable(doer) { thunkedDoer =>
      new Doer {
        override def doTheThing(): Unit = thunkedDoer().doTheThing()
      }
    }
  }
}
```

and will be used as follows:

```scala
val Revocable(revocableDoer, revoker) = Doer.revocable(access.doer)
revocableDoer.doTheThing() // works fine
revoker.revoke()
revocableDoer.doTheThing() // throws exception!
```

You can also use the `revocable` macro, which will create the implementation proxy automatically:

```scala
import ocaps._
import ocaps.macros._

// takes a `Doer` trait as a type parameter and autocreates implementation
val Revocable(revocableDoer, revoker) = revocable[Doer](access.doer)
```

If you have several revokers, you can compose them together using `Revoker.compose``:

```scala
val revokerABC = Revoker.compose(revokerA, revokerB, revokerC)
revokerABC.revoke() // revoke A, B, and C all at the same time.
```

By using revokers, you can tie capabilities to the lifespan of a user session by revoking them on session close or timeout.  Any call to the capability after revocation will result in failure.  

Recovery from revocation is application specific.  If an operation fails, the component that owns the capability may request a fresh new capability to replace the revoked one, or may require reauthorization or reauthentication before reinstantiation.  Akka Actors work extremely well in this context, as does judicious use of the IO monad.

### Managing Behavior with Modulation

Modulation is an extremely powerful technique that wraps a capability in additional behavior.  The canonical example of modulation is logging:

```scala
def loggingDoer(doer: Foo.Doer, logger: Logger): Foo.Doer = {
  new Foo.Doer {
    override def doTheThing(): Int = {
      logger.info(s"doTheThing: before call")
      val result = doer.doTheThing()
      logger.info(s"doTheThing: after returns $result")
      result
    }
  }
}
```

Modulation of a capability must obey behavioral subtyping -- it is acceptable to throw an exception and fail in security related conditions, but it is not acceptable to add new functionality, because it violates the [Liskov substitution principle](https://en.wikipedia.org/wiki/Liskov_substitution_principle).  For example, the following is extremely rude:

```scala
def badlyBehavedDoer(doer: Foo.Doer, logger: Logger): Foo.Doer = {
  new Foo.Doer {
    override def doTheThing(): Int = {
      val result = doer.doTheThing()
      if (result > 0) -1 else result // NEVER DO THIS
    }
  }
}
```

Modulation can be used in a [design by contract](https://en.wikipedia.org/wiki/Design_by_contract) style preconditions and postconditions.  For example, given an `ItemRepository.Finder`, we can specify a finder which must match a particular id (often called *narrowing*):

```scala
def idPreFinder(finder: ItemRepository.Finder, validIds: Set[UUID]): ItemRepository.Finder = {
  new ItemRepository.Finder {
    def find(id: UUID): Option[Item] = {
      if (validIds.contains(id)) {
        finder.find(id)
      } else {
        throw new CapabilityException("Invalid id!")
      }
    }
  }
}
```

Or modulation can verify a post condition exists -- for example, ensuring that only items owned by a particular user can be found:

```scala
def userPostFinder(finder: ItemRepository.Finder, user: User): ItemRepository.Finder = {
  new ItemRepository.Finder {
    def find(id: UUID): Option[Item] = {
      val result = finder.find(id)
      result.foreach { item =>
         if (! item.owner.equals(user)) {
           throw new CapabilityException("Invalid id!")
         }
      }
      result
    }
  }
}
```

Using modulation with capabilities, additional security guarantees can be added transparently to operations.

To skip the boilerplate, there is a capability macro which can automate the implementation of modulation by providing `before` and `after` functions:

```scala
import ocaps.macros._

def loggingDoer(doer: Foo.Doer, logger: Logger): Foo.Doer = {
  val before: String => Unit = methodName => {
    logger.info(s"$methodName: before call")
  }
  val after: (String, Any) => Unit = (methodName, result) =>
    logger.info(s"$methodName: after returns $result")
  modulate[Foo.Doer](doer, before, after)
}
```

### Managing Lifecycle with Expiration

Expiration combines *modulation* of a capability with *revocation*.  Using modulation, a capability can make use of internal or external state to decide whether it should revoke access through an internal revoker.

Expiration can be used to create a limited use capability, which is revoked after a certain number of calls.

```scala
def countBasedExpiration(doer: Foo.Doer, count: Int): Foo.Doer = {
  val latch = new java.util.concurrent.atomic.AtomicInteger(1)
  val Revocable(revocableDoer, revoker) = revocable[Foo.Doer](doer)
  val before: (String, Any) => Unit = (_, _) =>
   if (latch.getAndDecrement() == 0) {
     revoker.revoke()
   }
  modulate[Foo.Doer](revocableDoer, before, after)
} 
```

Count based expiration is especially useful when delegating a capability to an external worker which may execute at a much later date.  If you recall the bakery that allows you to eat your cake once and only once, this is a generalization of the technique.

A capability can also limit access by only allowing access based on a timer:

```scala
def timerBasedExpiration(doer: Foo.Doer, duration: FiniteDuration): Foo.Doer = {
  val deadline = duration.fromNow
  val Caretaker(revokerDoer, revoker) = caretaker[Foo.Doer](doer)
  val before: String => Unit = _ => if (deadline.isOverdue()) {
                                      revoker.revoke()
                                    }
  val after: (String, Any) => Unit = (_, _) => ()
  }
  modulate[Foo.Doer](revokerDoer, before, after)
}
```
 
Timer based expiration is helpful in resisting [Time of Check/Time of Use](https://en.wikipedia.org/wiki/Time_of_check_to_time_of_use) attacks and providing "sudo" mode for limited admin access. 
   
Expiration can also depend on external behavior, such as a supervisor that may look for suspicious activity.

```scala
def supervisorBasedExpiration(doer: Foo.Doer, supervisor: Supervisor): Foo.Doer = {
  val Revocable(revokerDoer, revoker) = revocable[Foo.Doer](doer)
  val before: String => Unit = _methodName => {
    if (! supervisor.accept(doer, methodName)) {
      revoker.revoke()
    }
  }
  val after: (String, Any) => Unit = (_, _) => ()
  modulate[Foo.Doer](revokerDoer, before, after)
}
```
