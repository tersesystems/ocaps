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

class Membrane(thunker: Thunker) {

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

object Membrane {
  def apply(thunker: Thunker): Membrane = new Membrane(thunker)
}

class RevokerMembrane(revoker: Revoker with Thunker) extends Membrane(revoker) with Revoker {
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