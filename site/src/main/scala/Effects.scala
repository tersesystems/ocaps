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

import scala.util._

/**
  * Demonstration of using tagless final to apply an effect
  * (aka Algebraic Data Types aka Object Algebras) to a capability.
  */
// #effects
object Effects {
  type Id[A] = A

  // #definition
  final class Document(private var name: String) {
    private object capabilities {
      val nameChanger = new Document.NameChanger[Id] {
        override def changeName(newName: String): Unit = {
          name = newName
        }
      }
    }
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #access
  object Document {
    sealed trait NameChanger[F[_]] {
      def changeName(name: String): F[Unit]
    }

    trait Effect[F[_]] {
      def wrap(nameChanger: NameChanger[Id]): NameChanger[F]
    }

    // The default "no effect" type Id[A] = A
    implicit val idEffect: Effect[Id] = nameChanger => identity(nameChanger)

    // Apply a "Try" effect to the capability
    implicit val tryEffect: Effect[Try] = nameChanger => {
      new NameChanger[Try] {
        override def changeName(name: String): Try[Unit] =
          Try(nameChanger.changeName(name))
      }
    }

    class Access {
      def nameChanger[F[_]: Effect](doc: Document): NameChanger[F] = {
        val effect = implicitly[Effect[F]]
        effect.wrap(doc.capabilities.nameChanger)
      }
    }
  }
  // #access

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")
    val access = new Document.Access()

    val nameChanger = access.nameChanger[Try](document)
    // or...
    // val nameChanger: NameChanger[Id] = access.nameChanger(document)

    nameChanger.changeName("steve") match {
      case Success(_) =>
        println(s"result = $document")
      case Failure(ex) =>
        println(s"exception = $ex")
    }
  }
  // #usage
}
// #effects