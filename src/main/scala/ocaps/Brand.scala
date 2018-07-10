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
 * @param hint hint shown in boxes.
 */
final class Brand private (val hint: Hint) {
  import Brand._

  private val shared: AtomicReference[Any] = new AtomicReference[Any]()

  val sealer: Sealer = new Sealer {
    def hint: Hint = Brand.this.hint

    /**
     * Applies a sealed box to the given input.
     *
     * {{{
     *   val (sealer, unsealer) = Brand.create("test brand").tuple
     *   val boxed: Brand.Box[String] = sealer("this is a test")
     * }}}
     *
     * @param input the unboxed input
     * @return a sealed box abstracting the input
     */
    def apply[T](input: T): Box[T] = new Box[T](input, Brand.this)
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
  }

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

  def apply[T](value: T): Box[T] = sealer.apply(value)

  def unapply[T](box: Box[T]): Option[T] = unsealer.apply(box)

  val tuple: (Sealer, Unsealer) = (sealer, unsealer)
}

/**
 * Hint is used to provide brand origin and check equality.
 */
trait Hint

object Hint {
  def apply(stringHint: String): Hint = StringHint(stringHint)

  case class StringHint(hint: String) extends Hint {
    override def toString: String = hint
  }
}

object Brand {

  trait ImplicitSealing {
    implicit def implicitSeal[T](value: T)(implicit sealer: Sealer): Brand.Box[T] = sealer(value)
  }

  object ImplicitSealing extends ImplicitSealing

  trait ImplicitUnsealing {
    implicit def implicitUnseal[T](box: Box[T])(implicit unsealer: Unsealer): Option[T] = unsealer(box)
  }

  object ImplicitUnsealing extends ImplicitUnsealing

  object Implicits extends ImplicitSealing with ImplicitUnsealing

  trait Sealer {

    def apply[T](value: T): Box[T]

    def hint: Hint
  }

  trait Unsealer {

    def apply[T](box: Box[T]): Option[T]

    def hint: Hint
  }

  class Box[T] private[Brand] (private[Brand] val value: T, private[Brand] val brand: Brand) {
    def hint: Hint = brand.hint
    override def toString: String = s"Box(hashCode = ${super.hashCode()}, hint = ${brand.hint})"
  }

  def create(stringHint: String): Brand = new Brand(Hint(stringHint))

  def create(hint: Hint): Brand = new Brand(hint)

  def create(): Brand = new Brand(Hint(UUID.randomUUID().toString))

  def tuple(brand: Brand): (Sealer, Unsealer) = brand.tuple
}
