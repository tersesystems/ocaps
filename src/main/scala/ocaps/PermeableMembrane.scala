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

/** A permeable membrane that can wrap operations with the same so that the same effect is felt on
  * all participants of the operation.
  *
  * {{{
  * val m = RevokerMembrane()
  * val wrappedCap1: m.Wrapper[Capability1] = m.wrap(capability1)
  * val wrappedCap2: m.Wrapper[Capability2] = m.wrap(capability2)
  * m.revoke() // revokes wrappedCap1 and wrappedCap2.
  * }}}
  *
  * @param thunker
  *   deferred execution of the capability
  */
class PermeableMembrane(thunker: Thunker) {

  sealed abstract class Wrapper[+A] {
    def get: A

    @inline final def map[B](f: A => B): Wrapper[B] = {
      new Wrapper[B]() {
        override def get: B = Thunk(f(Wrapper.this.get))()
      }
    }

    @inline final def flatMap[B](f: A => Wrapper[B]): Wrapper[B] = f(this.get)
  }

  def wrap[A](capability: => A): Wrapper[A] = {
    new Wrapper[A] {
      def get: A = thunker.thunk(capability)()
    }
  }
}

object PermeableMembrane {
  def apply(thunker: Thunker): PermeableMembrane = new PermeableMembrane(thunker)
}

/** A permeable membrane that is also a revoker.
  *
  * @param revoker
  */
class RevokerMembrane(revoker: Revoker with Thunker)
    extends PermeableMembrane(revoker)
    with Revoker {
  @inline override final def revoked: Boolean = revoker.revoked

  @inline override final def revoke(): Unit = revoker.revoke()
}

object RevokerMembrane {
  def apply(): RevokerMembrane = {
    apply(new LatchRevoker())
  }

  def apply(revoker: Revoker with Thunker): RevokerMembrane = {
    new RevokerMembrane(revoker)
  }
}
