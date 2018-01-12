/*
 * Copyright (C) 2018 Will Sargent. <http://www.tersesystems.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ocaps

class PermeableMembrane(revoker: WrappingRevoker) extends Revoker {
  self =>

  sealed abstract class Wrapper[+A] {
    def get: A

    @inline final def map[B](f: A => B): Wrapper[B] = SupplierWrapper(() => f(this.get))

    @inline final def flatMap[B](f: A => Wrapper[B]): Wrapper[B] = f(this.get)
  }

  // Not final because the scala compiler spits out a warning:
  // https://issues.scala-lang.org/browse/SI-4440
  case class SupplierWrapper[+A](supplier: () => A) extends Wrapper[A] {
    def get: A = supplier()
  }

  final case object Revoked extends Wrapper[Nothing] {
    override def get: Nothing = throw new RevokedException("Revoked")
  }

  @inline override final def revoked: Boolean = revoker.revoked

  @inline override final def revoke(): Unit = revoker.revoke()

  def wrap[A](capability: => A): Wrapper[A] = {
    SupplierWrapper(revoker.wrap(capability))
  }

  // Some experimentation to see how to make a Wrapper a cats.Monad.
  //  object Wrapper {
  //
  //    implicit val WrapperMonad: cats.Monad[Wrapper] = new Monad[Wrapper] {
  //      override def flatMap[A, B](wrapper: Wrapper[A])(f: A => Wrapper[B]): Wrapper[B] = {
  //        wrapper match {
  //          case Revoked => Revoked
  //          case SupplierWrapper(producer) => f(producer())
  //        }
  //      }
  //
  //      override def pure[A](x: A): Wrapper[A] = SupplierWrapper(() => x)
  //
  //      @tailrec
  //      override def tailRecM[A, B](a: A)(f: A => Wrapper[Either[A, B]]): Wrapper[B] = {
  //        f(a) match {
  //          case Revoked => Revoked
  //          case SupplierWrapper(supplier) =>
  //            supplier() match {
  //              case Right(b) =>
  //                pure(b)
  //              case Left(nextA) =>
  //                tailRecM(nextA)(f)
  //            }
  //        }
  //      }
  //    }
  //  }
}

object PermeableMembrane {
  def apply(): PermeableMembrane = {
    apply(new LatchRevoker())
  }

  def apply(revoker: WrappingRevoker): PermeableMembrane = {
    new PermeableMembrane(revoker)
  }
}
