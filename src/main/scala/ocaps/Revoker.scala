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

import java.util.concurrent.CountDownLatch

import scala.util.control.NoStackTrace

/**
 * Provides a handle to revoke a given capability.
 */
trait Revoker {

  /**
   * Revokes a given capability.
   */
  def revoke(): Unit

  /**
   * @return
   *   true if revoke() has been called, false otherwise.
   */
  def revoked: Boolean
}

/**
 * An implementation of a wrapping revoker that uses a countdown latch internally.
 */
class LatchRevoker extends Revoker with Thunker {
  private val latch = new CountDownLatch(1)
  override def revoke(): Unit = latch.countDown()
  override def revoked: Boolean = latch.getCount == 0

  /**
   * Return a Thunk of C, which returns C if this revoker is not `revoked`. If it has been revoked,
   * then then the thunk, when called, throws `RevokedException`.
   *
   * @throws RevokedException
   *   if revoked
   * @param cap
   *   the capability
   * @tparam C
   * @return
   *   a function providing a capability
   */
  override def thunk[C](cap: => C): Thunk[C] = { () =>
    if (revoked) {
      throw new RevokedException("Capability revoked!")
    } else {
      cap
    }
  }
}

object Revoker {

  /**
   * Creates a new revoker.
   *
   * @return
   *   new instance of revoker.
   */
  def apply(): Revoker = new LatchRevoker()

  /**
   * Creates a pair with a thunk which may throw an exception when called.
   *
   * Commonly used with `ocap.Revocable`.
   *
   * @param capability
   *   the input capability
   * @tparam C
   *   the type of the capability.
   * @return
   *   a thunk of a capability and a revoker as a tuple.
   */
  def tuple[C](capability: => C): (Thunk[C], Revoker) = {
    val revoker = new LatchRevoker()
    (revoker.thunk(capability), revoker)
  }

  /**
   * Composes a list of revokers together.
   *
   * @param revokers
   *   varadic number of revokers.
   * @return
   *   A single revoker which calls revoke on all the elements on the lsit.
   */
  def compose(revokers: Revoker*): Revoker = {
    new ListRevoker(revokers)
  }

  class ListRevoker(revokers: Iterable[Revoker]) extends Revoker {
    override def revoke(): Unit = revokers.foreach(_.revoke())
    override def revoked: Boolean = revokers.forall(_.revoked)
  }

  /**
   * A revoker which has been already revoked.
   */
  case object Revoked extends Revoker {
    override def revoke(): Unit = ()
    override def revoked: Boolean = true
  }
}

/**
 * Thrown when an application is unauthorized.
 *
 * Note that exception should NEVER pass around references to capabilities or objects.
 */
class CapabilityException(message: String) extends Exception(message) with NoStackTrace

/**
 * Used when a resource has been revoked.
 *
 * @param message
 *   the message of the resource.
 */
class RevokedException(message: String) extends CapabilityException(message)
