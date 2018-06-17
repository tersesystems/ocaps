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

// #before
object Before {

  // #definition
  final class Document(private[this] var name: String) {
    def changeName(newName: String): Unit = name = newName
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")
    document.changeName("steve")
    println(s"Before: result = $document")
  }
  // #usage
}
// #before

// #after-attenuation
object AfterAttenuation {

  // #definition
  final class Document(private var name: String) extends Document.NameChanger {
    override def changeName(newName: String): Unit =  {
      name = newName
    }
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #access
  object Document {
    trait NameChanger {
      def changeName(name: String): Unit
    }
  }
  // #access

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")

    // expose a single facet through attenuation
    val nameChanger = new Document.NameChanger {
      override def changeName(name: String): Unit = document.changeName(name)
    }
    nameChanger.changeName("steve")
    println(s"AfterAttenuation: result = $document")
  }
  // #usage
}
// #after-attenuation


// #after-amplificationo
object AfterAmplification {

  // #definition
  final class Document(private var name: String) {
    private object capabilities {
      val nameChanger = new Document.NameChanger {
        override def changeName(newName: String): Unit =  {
          name = newName
        }
      }
    }
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #access
  object Document {
    sealed trait NameChanger {
      def changeName(name: String): Unit
    }

    // Policy controls who has access to what
    class Access private {
      def nameChanger(doc: Document): NameChanger = {
        doc.capabilities.nameChanger
      }
    }

    object Access {
      def apply(): Access = new Access
    }
  }
  // #access

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")
    val access = Document.Access()
    val nameChanger = access.nameChanger(document)
    nameChanger.changeName("steve")
    println(s"AfterAmplification: result = $document")
  }
  // #usage
}
