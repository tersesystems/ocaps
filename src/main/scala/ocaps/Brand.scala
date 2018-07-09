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
  * @tparam T the type to be sealed
  */
final class Brand[T] private (val hint: Hint) {
  import Brand._

  private val shared: AtomicReference[T] = new AtomicReference[T]()

  val sealer: Sealer[T] = new Sealer[T] {
    def hint: Hint = Brand.this.hint

    /**
     * Applies a sealed box to the given input.
     *
     * {{{
     *   val (sealer, unsealer) = Brand.create[String]("test brand").tuple
     *   val boxed: Brand.Box[String] = sealer("this is a test")
     * }}}
     *
     * @param input the unboxed input
     * @return a sealed box abstracting the input
     */
    def apply(input: T): Box[T] = new Box[T](input, Brand.this)
  }

  val unsealer: Unsealer[T] = new Unsealer[T] {
    override def hint: Hint = Brand.this.hint
    def apply(box: Box[T]): Option[T] = {
      if (box.brand == Brand.this) {
        Option(transformAndGet(_ => box.value))
      } else {
        None
      }
    }
  }

  // https://alexn.org/blog/2013/05/07/towards-better-atomicreference-scala.html
  @tailrec
  private def transformAndGet(cb: T => T): T = {
    val oldValue = shared.get
    val newValue = cb(oldValue)
    if (!shared.compareAndSet(oldValue, newValue))
      transformAndGet(cb)
    else
      newValue
  }

  def apply(value: T): Box[T] = sealer.apply(value)

  def unapply(box: Box[T]): Option[T] = unsealer.apply(box)

  val tuple: (Sealer[T], Unsealer[T]) = (sealer, unsealer)
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

  trait Sealer[T] extends (T => Box[T]) {
    def hint: Hint
  }
  trait Unsealer[T] extends (Box[T] => Option[T]) {
    def hint: Hint
  }

  class Box[T] private[Brand] (private[Brand] val value: T, private[Brand] val brand: Brand[T]) {
    def hint: Hint = brand.hint
    override def toString: String = s"Box(hashCode = ${super.hashCode()}, hint = ${brand.hint})"
  }

  def create[T](stringHint: String): Brand[T] = new Brand[T](Hint(stringHint))

  def create[T](hint: Hint): Brand[T] = new Brand[T](hint)

  def create[T](): Brand[T] = new Brand[T](Hint(UUID.randomUUID().toString))

  def tuple[T](brand: Brand[T]): (Sealer[T], Unsealer[T]) = brand.tuple
}
