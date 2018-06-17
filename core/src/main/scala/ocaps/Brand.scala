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

import java.util.concurrent.locks.StampedLock

final class Brand[T] private(val hint: String) {
  import Brand._

  private val sl = new StampedLock()
  private var shared: Option[T] = None

  /**
   * Applies a sealed box to the given input.
   *
   * {{{
   *   val brand = Brand.create[String]("test brand")
   *   val boxed: Brand.Box[String] = brand("this is a test")
   * }}}
   *
   * @param input the unboxed input
   * @return a sealed box abstracting the input
   */
  def apply(input: T): Box[T] = new Box[T](input, this)

  def unapply(box: Box[T]): Option[T] = {
      val l = sl.writeLock()
      try {
        box.shareContent()
        shared.map { content =>
          shared = None
          content
        }
      } finally {
        sl.unlockWrite(l)
      }
    }

  val sealer: Sealer[T] = apply

  val unsealer: Unsealer[T] = unapply

  val tuple: (Sealer[T], Unsealer[T]) = (sealer, unsealer)
}

object Brand {

  type Sealer[T] = T => Box[T]
  type Unsealer[T] = Box[T] => Option[T]

  class Box[T](obj: T, brand: Brand[T]) {
    private[Brand] def shareContent(): Unit = {
      brand.shared = Option(obj)
    }
    override def toString: String = "<sealed " + brand.hint + " box>"
  }

  def create[T](hint: String) = new Brand[T](hint)

  def tuple[T](brand: Brand[T]): (Sealer[T], Unsealer[T]) = brand.tuple
}
