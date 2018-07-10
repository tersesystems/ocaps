/*
 * Copyright 2018 Will Sargent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ocaps

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec

/**
 * Brand contains a sealer and unsealer.
 *
 * Generally you want to keep the brand private, and only hand out a
 * sealer or an unsealer if you must.  You can match sealers, unsealers
 * and boxes together through the `Hint`.
 *
 * @param hint hint shown in boxes.
 */
final class Brand private (val hint: Hint) {
  import Brand._

  private val shared: AtomicReference[Any] = new AtomicReference[Any]()

  val sealer: Sealer = new Sealer {
    def hint: Hint = Brand.this.hint

    def apply[T](input: T): Box[T] = new Box[T](input, Brand.this)

    override def toString: String = {
      s"Sealer(hashCode = ${super.hashCode()}, hint = $hint)"
    }
  }

  val unsealer: Unsealer = new Unsealer {
    override def hint: Hint = Brand.this.hint

    def apply[T](box: Box[T]): Option[T] = {
      if (box.brand == Brand.this) {
        val value = transformAndGet(_ => box.value)
        Option(value)
      } else {
        None
      }
    }

    override def toString: String = {
      s"Unsealer(hashCode = ${super.hashCode()}, hint = $hint)"
    }
  }

  /**
   * Convenience tuple.
   */
  val tuple: (Sealer, Unsealer) = (sealer, unsealer)

  /**
   * Convenience method for applying the sealer method.
   *
   * @param value the input value.
   * @tparam T the type of the input value.
   * @return the boxed value.
   */
  def apply[T](value: T): Box[T] = sealer.apply(value)

  /**
   * Convenience method for applying the unsealer method in pattern matching.
   *
   * @param box the box containing the value.
   * @tparam T the type of the boxed value.
   * @return the option of the unboxed value.
   */
  def unapply[T](box: Box[T]): Option[T] = unsealer.apply(box)

  // https://alexn.org/blog/2013/05/07/towards-better-atomicreference-scala.html
  @tailrec
  private def transformAndGet[T](cb: Any => T): T = {
    val oldValue = shared.get
    val newValue = cb(oldValue)
    if (!shared.compareAndSet(oldValue, newValue))
      transformAndGet(cb)
    else
      newValue
  }

  override def toString: String = s"Brand(hashCode = ${super.hashCode()}, hint = $hint)"
}

/**
 * Hint is used to provide brand origin and check equality for brands.
 */
trait Hint

object Hint {
  def apply(stringHint: String): Hint = StringHint(stringHint)

  case class StringHint(hint: String) extends Hint {
    override def toString: String = hint
  }
}

object Brand {

  trait Sealer {
    /**
     * Seals the input into a Box.
     *
     * {{{
     *   val (sealer, unsealer) = Brand.create("test brand").tuple
     *   val boxed: Brand.Box[String] = sealer("this is a test")
     * }}}
     *
     * @param value the unboxed value
     * @return a sealed box abstracting the input
     */
    def apply[T](value: T): Box[T]

    def hint: Hint
  }

  trait Unsealer {

    /**
     * Unseals the box, returning `Some(value)` if the box has the same brand
     * as this unsealer, `None` otherwise.
     *
     * {{{
     * val box: Brand.Box[String] = ...
     * val unboxed: Option[String] = unsealer(box)
     * }}}
     *
     * @param box the boxed item.
     * @tparam T the type of the value in the box.
     * @return `Some(value)` if the box has the same brand as this unsealer, `None` otherwise.
     */
    def apply[T](box: Box[T]): Option[T]

    def hint: Hint
  }

  class Box[T] private[Brand] (private[Brand] val value: T, private[Brand] val brand: Brand) {
    def hint: Hint = brand.hint
    override def toString: String = s"Box(hashCode = ${super.hashCode()}, hint = ${brand.hint})"
  }

  /**
   * Implicitly boxes input values.
   *
   * {{{
   * import ImplicitSealing._
   * implicit val sealer = ...
   * val boxedString: Brand.Box[String] = "I am boxed!"
   * }}}
   */
  trait ImplicitSealing {
    implicit def implicitSeal[T](value: T)(implicit sealer: Sealer): Brand.Box[T] = sealer(value)
  }

  object ImplicitSealing extends ImplicitSealing

  /**
   * Implicitly unboxes sealed boxes.
   *
   * {{{
   * import ImplicitUnsealing._
   * implicit val unsealer = ...
   * val maybeString: Option[String] = boxedString
   * }}}
   */
  trait ImplicitUnsealing {
    implicit def implicitUnseal[T](box: Box[T])(implicit unsealer: Unsealer): Option[T] = unsealer(box)
  }

  object ImplicitUnsealing extends ImplicitUnsealing

  /**
   * Implicitly boxes and unboxes, if the appropriate sealer/unsealer is in implicit scope.
   */
  object Implicits extends ImplicitSealing with ImplicitUnsealing

  /**
   * Creates a new Brand.
   *
   * @param stringHint a string used as a hint.  Note that the string is used for comparison, so should be unique.
   * @return the brand.
   */
  def create(stringHint: String): Brand = new Brand(Hint(stringHint))

  /**
   * Creates a new Brand.
   *
   * @param hint a hint used for comparison on sealer/unsealer.
   * @return the brand.
   */
  def create(hint: Hint): Brand = new Brand(hint)

  /**
   * Creates a new Brand using a random UUID as a string.
   */
  def create(): Brand = new Brand(Hint(UUID.randomUUID().toString))

}
