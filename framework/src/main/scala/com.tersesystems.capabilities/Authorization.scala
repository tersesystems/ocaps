package com.tersesystems.capabilities

import scala.language.higherKinds
import scala.util._

trait Authority[-DomainSecuredObject] {

  /**
   * The type of capability returned.  You can either type alias this:
   *
   * {{{
   * case object Readable extends Authority[Document] {
   *   override type Capability[DSO] = ReadableCapability
   *   override type Args = Charset
   *
   *   override protected def newCapability[DSO <: Document](so: DSO, args: Args*): Try[Capability[DSO]] = {
   *     Success(new Capability(so, args.headOption.getOrElse(StandardCharsets.UTF_8)))
   *   }
   *
   *   class ReadableCapability private[Readable](document: Document, charset: Charset) { ... }
   * }
   * }}}
   *
   * or use it in the Capability itself:
   *
   * {{{
   * case object Eatable extends Authority[Fruit] {
   *   override type Capability[DSO <: Fruit] = EatableCapability[DSO] // Note DSO is passed through
   *   override type Args = Unit
   *   override protected def newCapability[DSO <: Fruit](fruit: DSO, args: Args*): Try[Capability[DSO]] = {
   *     Success(new Capability(fruit))
   *   }
   *
   *   class EatableCapability[DSO <: Fruit] private[Eatable] (f: DSO) {
   *     def eat(): DSO = ...
   *   }
   * }
   * }}}
   *
   * @tparam DSO domain specific type
   */
  type Capability[DSO <: DomainSecuredObject]

  // Capability argument type (use a type alias here)
  type Args

  /**
   * Tests a secured object against this authority with the given context, and returns the associated capability.
   *
   * {{{
   * implicit val ctx: SecurityContext = ...
   * FooAuthority(foo) { fooCapability =>
   *    fooCapability.executeSensitiveOperation()
   * }
   * }}}
   *
   * @param so the secured object
   * @param args varadic arguments needed for the capability, if any.
   * @param block the block receiving the capability
   * @param ctx the implicit security context
   * @tparam DSO type of the secured object, which may be a subtype of DomainSecuredObject and is carried through to the capability.
   * @tparam T The return type of the block.
   * @return `scala.util.Success[T]` if the authorization was successful and the capability returned, `Failure[T]` otherwise.
   */
  def apply[DSO <: DomainSecuredObject, T](so: DSO, args: Args*)(block: (Capability[DSO] => T))(implicit ctx: SecurityContext): Try[T] = {
    if (ctx.authorize(this, so)) {
      newCapability(so, args: _*).map { capability =>
        block(capability)
      }
    } else {
      Failure(this.generateFailureMessage(so))
    }
  }

  /**
   * Generates a failure message when the context could not authorize this capability.
   *
   * @param so the secured object
   * @param ctx the implicit security context
   * @tparam DSO type of the secured object, which may be a subtype of DomainSecuredObject and is carried through to the capability.
   * @return Exception
   */
  protected def generateFailureMessage[DSO <: DomainSecuredObject](so: DSO)(implicit ctx: SecurityContext): Exception = {
    val myName = this.getClass.getSimpleName
    val msg = s"Cannot authorize $myName"
    new UnauthorizedException(msg)
  }

  /**
   * Generates a new capability tied to the specific secured object.
   *
   * @param so the secured object
   * @param args arguments for the capability constructor.
   * @tparam DSO type of the secured object, which may be a subtype of DomainSecuredObject and is carried through to the capability.
   * @return
   */
  protected def newCapability[DSO <: DomainSecuredObject](so: DSO, args: Args*): Try[Capability[DSO]]
}

/**
 * The security context.  Usually contains the security principal.
 */
trait SecurityContext {

  /**
   * End users this method to provide a security policy for the given context.
   *
   * This method must deal with all possible security objects, so you may want to pass several
   * partial functions through
   *
   * {{{
   * class ExampleSecurityContext(user: User) extends SecurityContext {
   *   override def authorize[DSO](authority: Authority[DSO], so: DSO): Boolean = {
   *     so match {
   *       case promotion: Promotion =>
   *          promotion match {
   *             case Deletable =>
   *               isAdmin(user)
   *             case Editable =>
   *               promotion.editor.equals(user) or isAdmin(user)
   *          }
   *
   *       case bitcoin: Bitcoin =>
   *          true
   *     }
   *   }
   * }
   * }}}
   */
  def authorize[DSO](authority: Authority[DSO], so: DSO): Boolean
}

/**
 * Thrown when an application is unauthorized.
 */
class UnauthorizedException(message: String) extends Exception(message)

