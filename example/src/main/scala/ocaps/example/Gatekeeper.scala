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

package ocaps.example

import java.io.{BufferedReader, BufferedWriter, InputStream, OutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, _}

import ocaps._

import scala.util._

object Gatekeeper {
  import Document._

  private val gatekeeper = new DocumentGatekeeper(Document.Access())

  def main(args: Array[String]): Unit = {
    val jeff = new User("jeff")
    val steve = new User("steve")
    val mutt = new User("mutt")
    val users: Seq[User] = Seq(jeff, steve, mutt)

    // Admin can do anything, so it gets to create the documents...
    val adminActivity = new AdminActivity(gatekeeper)
    val documents = adminActivity.createDocuments(users)

    for (user <- users) {
      implicit val ctx: SecurityContext = new SecurityContext(user)
      for (doc <- documents) {
        val maybeReader = gatekeeper.reader(doc).toOption
        val maybeWriter = gatekeeper.writer(doc).toOption

        // Capabilities are passed to the scope
        // Everything inside this user activity scope is "non-privileged"
        val userActivity = new UserActivity(user, maybeReader, maybeWriter)
        userActivity.attemptDocument(doc)
      }
    }
    adminActivity.deleteDocuments(documents)
  }

  /**
   * AdminActivity operations use admin's security context
   */
  class AdminActivity(gatekeeper: DocumentGatekeeper) {
    private val admin = new User("admin")
    private implicit val ctx: SecurityContext = new SecurityContext(admin)

    def createDocuments(users: Seq[User]): Seq[Document] = {
      for (owner <- users) yield {
        val doc = Document(owner = owner.name, Files.createTempFile(null, ".txt"))
        val writer = gatekeeper.writer(doc).get

        writer.bufferedWriter() { bw: java.io.BufferedWriter =>
          bw.write(s"Created by ${owner.name}")
        }
        doc
      }
    }

    def deleteDocuments(documents: Seq[Document]): Unit = {
      documents.foreach { doc =>
        gatekeeper.deleter(doc).get.delete()
      }
    }
  }

  /**
   * UserActivity operations are not guaranteed to have full access to capabilities.
   */
  class UserActivity(user: User, maybeReader: Option[Reader], maybeWriter: Option[Writer]) {

    def attemptDocument(doc: Document): Unit = {
      maybeWriter match {
        case Some(cap) =>
          cap.bufferedWriter() { nioWriter =>
            nioWriter.write(s"Written to by $user")
          }

          println(s"$user writing to $doc")
        case None =>
          println(s"$user CANNOT write to doc $doc")
      }

      maybeReader match {
        case Some(cap) =>
          val result = cap.bufferedReader(buf => buf.readLine())
          println(s"$user reading from $doc: $result")
        case None =>
          println(s"$user CANNOT read from doc $doc")
      }
    }
  }

  /** Simplest possible user */
  class User(val name: String) {
    override def toString: String = s"User($name)"
  }

  /**
   * Gatekeeper controls who has access to capabilities.
   */
  class DocumentGatekeeper(access: Access) {

    private val policy = new DocumentPolicy

    def reader(doc: Document)(implicit ctx: SecurityContext): Try[Reader] = {
      if (policy.canRead(ctx.user, doc)) {
        Success(access.reader(doc))
      } else {
        Failure(new CapabilityException(s"Cannot authorize ${ctx.user} for writer to doc $doc"))
      }
    }

    def writer(doc: Document)(implicit ctx: SecurityContext): Try[Writer] = {
      if (policy.canWrite(ctx.user, doc)) {
        Success(access.writer(doc))
      } else {
        Failure(new CapabilityException(s"Cannot authorize ${ctx.user} for writer to doc $doc"))
      }
    }

    def deleter(doc: Document)(implicit ctx: SecurityContext): Try[Deleter] = {
      if (policy.canDelete(ctx.user, doc)) {
        Success(access.deleter(doc))
      } else {
        Failure(new CapabilityException(s"Cannot authorize ${ctx.user} for deleter to doc $doc"))
      }
    }

    /**
     * Define the operational contract between users and documents
     *
     * https://types.cs.washington.edu/ftfjp2013/preprints/a6-Drossopoulou.pdf
     */
    // Normal users can read anything, but only write to document they own.
    class DocumentPolicy {
      def canRead(user: User, doc: Document): Boolean = true

      def canWrite(user: User, doc: Document): Boolean = {
        isDocumentOwner(user, doc) || isAdmin(user)
      }

      def canDelete(user: User, doc: Document): Boolean = {
        isDocumentOwner(user, doc) || isAdmin(user)
      }

      private def isDocumentOwner(user: User, doc: Document): Boolean = {
        doc.owner.equals(user.name) || isAdmin(user)
      }

      private def isAdmin(user: User): Boolean = {
        user.name.equals("admin")
      }
    }

  }

  /**
   *  Implicit security context authorizes a particular user for a particular doc against a gatekeeper.
   */
  class SecurityContext(val user: User)

  /**
   * A document resource.  The path is private, and no operations are possible without
   * an associated capability.
   */
  final class Document private(val owner: String,
                               private[this] val path: Path) {

    private object capabilities {
      val reader: Document.Reader = new Document.Reader {
        override def bufferedReader[T](block: BufferedReader => T): T = {
          val reader = Files.newBufferedReader(Document.this.path, StandardCharsets.UTF_8)
          try {
            block(reader)
          } finally {
            reader.close()
          }
        }

        override def inputStream[T](block: InputStream => T): T = {
          val is = Files.newInputStream(Document.this.path)
          try {
            block(is)
          } finally {
            is.close()
          }
        }
      }

      val writer: Document.Writer = new Document.Writer {
        override def bufferedWriter[T](options: Seq[OpenOption] = Seq(StandardOpenOption.SYNC))(block: BufferedWriter => T): T = {
          val bufWriter = Files.newBufferedWriter(Document.this.path, StandardCharsets.UTF_8, options: _*)
          try {
            block(bufWriter)
          } finally {
            bufWriter.close()
          }
        }

        override def outputStream[T](options: Seq[OpenOption])(block: OutputStream => T): T = {
          val os = Files.newOutputStream(Document.this.path, options: _*)
          try {
            block(os)
          } finally {
            os.close()
          }
        }
      }

      val deleter: Document.Deleter = () => Files.delete(Document.this.path)
    }

    override def toString: String = s"Document(owner = $owner)"
  }

  object Document {

    trait Reader {
      def bufferedReader[T](block: BufferedReader => T): T
      def inputStream[T](block: InputStream => T): T
    }

    trait Writer {
      def bufferedWriter[T](options: Seq[OpenOption] = Seq(StandardOpenOption.SYNC))(block: BufferedWriter => T): T
      def outputStream[T](options: Seq[OpenOption] = Seq(StandardOpenOption.SYNC))(block: OutputStream => T): T
    }

    trait Deleter {
      def delete(): Unit
    }

    final class Access private {
      def reader(document: Document): Reader = document.capabilities.reader
      def writer(document: Document): Writer = document.capabilities.writer
      def deleter(document: Document): Deleter  = document.capabilities.deleter
    }

    object Access {
      private val instance = new Access()
      def apply(): Access = instance
    }

    def apply(owner: String, path: java.nio.file.Path): Document = {
      new Document(owner, path)
    }

  }
}