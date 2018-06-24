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

/**
  * Contains a value and revoker.
  *
  * @tparam A the type of value
  */
sealed abstract class Revocable[+A] {

  @inline final def getOrElse[B >: A](default: => B): B = {
    if (revoked) default else this.get
  }

  def get: A

  @inline final def orNull[A1 >: A](implicit ev: Null <:< A1): A1 =
    this getOrElse ev(null)

  @inline final def revoked: Boolean = revoker.revoked

  def revoker: Revoker

  def tuple: (A, Revoker) = (get, revoker)
}

object Revocable {

  /**
    * Creates a new revocable from an input capability.
    *
    * {{{
    * trait Doer {
    *   def doTheThing(): Unit
    * }
    *
    * def revocable(doer: Doer): Revocable[Doer] = {
    *   Revocable(doer) { thunk =>
    *     new Doer {
    *       override def doTheThing(): Unit = forwarder().doTheThing()
    *     }
    *   }
    * }
    * }}}
    */
  def apply[C](capability: C)(cblock: Thunk[C] => C): Revocable[C] = {
    val (thunk, r) = Revoker.tuple(capability)
    Revocable(cblock(thunk), r)
  }

  def apply[C](c: C, r: Revoker): Revocable[C] = {
    new Revocable[C] {
      override def get: C = c
      override def revoker: Revoker = r
    }
  }

  def unapply[C](revocable: Revocable[C]): Option[(C, Revoker)] = {
    Some((revocable.get, revocable.revoker))
  }

  case object Revoked extends Revocable[Nothing] {
    def get = throw new NoSuchElementException("Revoked.get")

    override val revoker: Revoker = Revoker.Revoked
  }

}
