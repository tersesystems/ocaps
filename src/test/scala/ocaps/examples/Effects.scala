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

package ocaps.examples

import scala.util._

/**
  * Demonstration of using tagless final to apply an effect
  */
// #effects
object Effects {
  type Id[A] = A

  // #definition
  final class Document(private var name: String) {
    import Document._
    private object capabilities {
      val nameChanger: NameChanger[Id] = new NameChanger[Id] {
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
    trait NameChanger[F[_]] {
      def changeName(name: String): F[Unit]
    }

    trait WithEffect[C[_[_]], F[_]] {
      def apply(capability: C[Id]): C[F]
    }

    // The default "no effect" type Id[A] = A
    implicit val idEffect: NameChanger WithEffect Id = new WithEffect[NameChanger, Id] {
      override def apply(capability: NameChanger[Id]): NameChanger[Id] = identity(capability)
    }

    // Apply a "Try" effect to the capability
    implicit val tryEffect: NameChanger WithEffect Try = new WithEffect[NameChanger, Try] {
      override def apply(capability: NameChanger[Id]): NameChanger[Try] = new NameChanger[Try] {
        override def changeName(name: String): Try[Unit] =  Try(capability.changeName(name))
      }
    }

    class Access {
      def nameChanger[F[_]](doc: Document)(implicit ev: NameChanger WithEffect F): NameChanger[F] = {
        val effect = implicitly[NameChanger WithEffect F]
        effect(doc.capabilities.nameChanger)
      }
    }
  }
  // #access

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")
    val access = new Document.Access()

    val tryNameChanger = access.nameChanger[Try](document)
    tryNameChanger.changeName("steve") match {
      case Success(_) =>
        println(s"result = $document")
      case Failure(ex) =>
        println(s"exception = $ex")
    }

    // or...

    val idNameChanger = access.nameChanger[Id](document)
    idNameChanger.changeName("Will")
    println(s"result = $document")
  }
  // #usage
}
// #effects