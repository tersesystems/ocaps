# Authorizing Capabilities

## Authorization and Ambient Authority

One of the issues that the capability model neatly sidesteps is the issue of [ambient authority](https://en.wikipedia.org/wiki/Ambient_authority).  

Broadly speaking, ambient authority is when a user or a security context (also known as a "Principal") is assumed to exist when making sensitive calls, and the credentials of the "current user" are applied every time.

In Java, ambient authority would typically come in the form of a thread-local user context that would be accessible from every method.  In Scala, it typically takes the form of an implicit `User` or `SecurityContext` that is always accessible.

In a capability model, authorization is used only and solely to dispense the capability.  After that point, the capability is passed around without an implicit `SecurityContext` and without ambient authority.  If the user logs out at any point, then the capability is revoked.

## Authorizating Capabilities with Gatekeeper

Handing out a capability to an object gives the object authority over the resource.  The question of whether that object should have authority or not is an authorization decision.  The question of authorization ("what authority") is usually accompanied by the question of authentication ("who should have this authority?").

In capability based systems, there are four stages before a capability is handed out.  [Alan Karp](https://youtu.be/XQWY9_BcSGI?t=8m40s) defines four stages:

1. Identification (grant privs)
2. Authentication (let a process running on behalf use priv associated with identity)
3. Authorization (grant token, etc)
4. Access Decision (in the service)

Capability-based authorization systems identify a policy that says who is authorized for an operation on a resource, and then hand out a capability for that operation.  That capability is then used directly.  This is in contrast to role based access control (RBAC), which ties in the identity directly.  Examples include [OAuth 2 Bearer Token](https://en.wikipedia.org/wiki/OAuth#OAuth_2.0) and [XACML](https://en.wikipedia.org/wiki/XACML), but it's quite simple to create your own.

This may not mean much in isolation, so let's run through an example with Scala code, implementing a Gatekeeper class for documents.

> You can see the full class in @ref:[Gatekeeper example](../examples/gatekeeper.md) page.

First, we need authentication.  Let's posit a `User` class that serves as the [principal](https://stackoverflow.com/a/5025140), and an implicit `SecurityContext` that serves as an [implicit context](http://www.lihaoyi.com/post/ImplicitDesignPatternsinScala.html#implicit-contexts). 

```scala
case class User(name: String)
class SecurityContext(val user: User)
```

We also need to extend `Document` slightly to have an `owner` field: 

```scala
final class Document private(val owner: String,
                             private[this] val path: Path) {
   ...
}
```

Next, we put together a `DocumentGatekeeper`.  This takes a `reader(doc: Document)` with the `SecurityContext` as an implicit parameter, and returns the `Document.Reader` capability only if it passes the document policy.

```scala
class DocumentGatekeeper() {

  private class DocumentPolicy {
    def canRead(user: User, doc: Document): Boolean = {
      isDocumentOwner(user, doc) || isAdmin(user)
    }
    private def isDocumentOwner(user: User, doc: Document): Boolean = {
      doc.owner.equals(user.name) || isAdmin(user)
    }
    private def isAdmin(user: User): Boolean = {
      user.name.equals("admin")
    }
  }

  private val access = Document.Access()
  private val policy = new DocumentPolicy
  
  def reader(doc: Document)(implicit ctx: SecurityContext): Try[Reader] = {
    if (policy.canRead(ctx.user, doc)) {
      Success(access.reader(doc))
    } else {
      Failure(new CapabilityException(s"Cannot authorize ${ctx.user} for writer to doc $doc"))
    }
  }
}
```

A gatekeeper is not complicated, but it's important to note it should be involved only when granting capabilities.

```scala
// Assume authentication happens up here...
val user = new User("will")
implicit val sc = new SecurityContext(will)

// Access the document...
val doc = new Document(user.name, path)

// get reader or throw exception
val reader = gatekeeper.reader(doc).get

// pass around reader to actor here...
system.actorOf(DocumentActivityActor.props(reader))

// Or create new Activity class instance with reader as constructor parameter
val userActivity = new DocumentActivity(reader)
```

From the time the capability is accessible, it is "in scope" of the application code, and may be not tied to the lifecycle of the user session.  That is, there may be a job or a process which can run with the reader well after the user has logged out.  The `SecurityContext` is only required by the gatekeeper, and is not required by any following activity.  There is no ambient authority.

The discussion around capabilities and their lifecycles, and how capabilities can be passed around and delegated to other classes is the biggest point of difference between permission based access control (which always requires an identity context) and capability based access control (which depends on an object reference).  In short, once you have assigned capabilities, you must then manage them.

## Dispensing Capabilities with Composition

When there are several capabilities that may be available from authorization, one common pattern is to return a set of capabilities to the caller, providing a range of options.  This works in situations

```scala
class Caller(capabilities: Set[AnyRef]) {
 ...
}

val capabilitySet = Set(reader, writer)
val caller = new Caller(capabilitySet)
```

Note that `Caller` does not know what capabilities may be presented to it, and so must iterate to see if it has access.  This is fine as far as it goes, but it does inherently provide collection mechanics on top of managing capabilities, and loses some type information.
 
Another option is to use *composition* to return an object that directly incorporates all the capabilities.

```scala
class Caller(capabilities: AnyRef) {
 ...
}

val capabilities = ocap.macros.compose[Reader with Writer](reader, writer)
val caller = new Caller(capabilities)
```

Now the caller can pattern match directly against the capability:

```scala
class Caller(capabilities: AnyRef) {
  private def reader: Option[Reader] = {
    capabilities match {
      case reader: Reader =>
        Some(reader)
      case _ =>
        None
    }
  }
}
```

Or the caller can extract the facets through *attenuation*:

```scala
val reader: Reader = ocaps.macros.attenuate[Reader](capabilities)
val writer: Writer = ocaps.macros.attenuate[Writer](capabilities)
```

You can see a complete @ref:[attenuation example](../examples/attenuation.md) for more details.

Composition is not the only way of arranging capabilities, of course.  Joe Duffy has a great [post](http://joeduffyblog.com/2015/11/10/objects-as-secure-capabilities/) where he discusses the practical aspects of capabilities:

> I’ll be the first to admit, there was a maturity process that developers went through, as they learned about the design patterns in an object capability system. It was common for “big bags” of capabilities to grow over time, and/or for capabilities to be requested at an inopportune time. For example, imagine a Stopwatch API. It probably needs the Clock. Do you pass the Clock to every operation that needs to access the current time, like Start and Stop? Or do you construct the Stopwatch with a Clock instance up-front, thereby encapsulating the Stopwatch’s use of the time, making it easier to pass to others (recognizing, importantly, that this essentially grants the capability to read the time to the recipient). Another example, if your abstraction requires 15 distinct capabilities to get its job done, does its constructor take a flat list of 15 objects? What an unwieldy, annoying constructor! Instead, a better approach is to logically group these capabilities into separate objects, and maybe even use contextual storage like parents and children to make fetching them easier.

As in all things, choose the solution that works best for your context.