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

// #gatekeeper
import java.io.{BufferedReader, BufferedWriter, InputStream, OutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, _}

import ocaps._
import ocaps.macros._

object Gatekeeper {

  import Document._

  private val gatekeeper = new DocumentGatekeeper(Document.Access())

  def main(args: Array[String]): Unit = {
    val jeff = new User("jeff")
    val steve = new User("steve")
    val mutt = new User("mutt")
    val users: Seq[User] = Seq(jeff, steve, mutt)

    // Admin can do anything, so it gets to create the documents...
    val adminActivity = new AdminActivity(Access())
    val documents = adminActivity.createDocuments(users)

    for (user <- users) {
      implicit val ctx: SecurityContext = new SecurityContext(user)
      for (doc <- documents) {
        val capabilities = gatekeeper.capabilities(doc)

        // Capabilities are passed to the scope
        // Everything inside this user activity scope is "non-privileged"
        val userActivity = new UserActivity(user, capabilities)
        userActivity.attemptDocument(doc)
      }
    }
    adminActivity.deleteDocuments(documents)
  }

  /**
   * Admin activities have direct access to all capabilities, as this is not a
   * user context.
   */
  class AdminActivity(access: Access) {
    def createDocuments(users: Seq[User]): Seq[Document] = {
      for (owner <- users) yield {
        val doc =
          Document(owner = owner.name, Files.createTempFile(null, ".txt"))
        val writer = access.writer(doc)
        writer.bufferedWriter(Seq(StandardOpenOption.SYNC)) { bw: java.io.BufferedWriter =>
          bw.write(s"Created by ${owner.name}")
        }
        doc
      }
    }

    def deleteDocuments(documents: Seq[Document]): Unit = {
      documents.foreach { doc =>
        val deleter = access.deleter(doc)
        deleter.delete()
      }
    }
  }

  /**
   * UserActivity operations are not guaranteed to have full access to capabilities.
   */
  class UserActivity(user: User, capabilities: Set[Capability]) {

    def attemptDocument(doc: Document): Unit = {
      maybeWriter(capabilities) match {
        case Some(cap) =>
          cap.bufferedWriter(Seq(StandardOpenOption.SYNC)) { nioWriter =>
            nioWriter.write(s"Written to by $user")
          }

          println(s"$user writing to $doc")
        case None =>
          println(s"$user CANNOT write to doc $doc")
      }

      maybeReader(capabilities) match {
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

    private var userRevokerMap = Map[User, Revoker]()

    def capabilities(doc: Document)(implicit ctx: SecurityContext): Set[Capability] = {
      var capSets = Set[Document.Capability]()

      var revokers = Set[Revoker]()
      if (policy.canRead(ctx.user, doc)) {
        val (reader, revoker) = revocable[Reader](access.reader(doc)).tuple
        capSets += reader
        revokers += revoker
      }

      if (policy.canWrite(ctx.user, doc)) {
        val (writer, revoker) = revocable[Writer](access.writer(doc)).tuple
        capSets += writer
        revokers += revoker
      }

      if (policy.canDelete(ctx.user, doc)) {
        val (deleter, revoker) = revocable[Deleter](access.deleter(doc)).tuple
        capSets += deleter
        revokers += revoker
      }

      userRevokerMap += (ctx.user -> Revoker.compose(revokers.toSeq: _*))

      capSets
    }

    // If a user's session expires or the user misbehaves, we can revoke
    // any access that the user has to all documents.
    def revoke(user: User): Unit = {
      // Note that the revoker map prevents the document object from being GC'ed,
      // so if you want to release it you may need to use scala.WeakReference
      userRevokerMap.get(user).foreach(_.revoke())
      userRevokerMap -= user
    }

    class DocumentPolicy {
      // anyone can read.
      def canRead(user: User, doc: Document): Boolean = true

      // only document owner can write.
      def canWrite(user: User, doc: Document): Boolean = {
        isDocumentOwner(user, doc)
      }

      // only document owner can delete.
      def canDelete(user: User, doc: Document): Boolean = {
        isDocumentOwner(user, doc)
      }

      private def isDocumentOwner(user: User, doc: Document): Boolean = {
        doc.owner.equals(user.name)
      }
    }

  }

  /**
   * Implicit security context authorizes a particular user for a particular doc against a gatekeeper.
   */
  class SecurityContext(val user: User)

  /**
   * A document resource.  The path is private, and no operations are possible without
   * an associated capability.
   */
  final class Document private(
                                val owner: String,
                                private[this] val path: Path
                              ) {

    private object capabilities {
      val reader: Document.Reader = new Document.Reader {
        override def bufferedReader[T](block: BufferedReader => T): T = {
          val reader =
            Files.newBufferedReader(Document.this.path, StandardCharsets.UTF_8)
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
        override def bufferedWriter[T](
                                        options: Seq[OpenOption] = Seq(StandardOpenOption.SYNC)
                                      )(block: BufferedWriter => T): T = {
          val bufWriter = Files.newBufferedWriter(
            Document.this.path,
            StandardCharsets.UTF_8,
            options: _*
          )
          try {
            block(bufWriter)
          } finally {
            bufWriter.close()
          }
        }

        override def outputStream[T](
                                      options: Seq[OpenOption]
                                    )(block: OutputStream => T): T = {
          val os = Files.newOutputStream(Document.this.path, options: _*)
          try {
            block(os)
          } finally {
            os.close()
          }
        }
      }

      val deleter: Document.Deleter = new Deleter {
        override def delete(): Unit = Files.delete(Document.this.path)
      }
    }

    override def toString: String = s"Document(owner = $owner)"
  }

  object Document {

    trait Capability

    trait Reader extends Capability {
      def bufferedReader[T](block: BufferedReader => T): T

      def inputStream[T](block: InputStream => T): T
    }

    trait Writer extends Capability {
      def bufferedWriter[T](options: Seq[OpenOption])(block: BufferedWriter => T): T

      def outputStream[T](options: Seq[OpenOption])(block: OutputStream => T): T
    }

    trait Deleter extends Capability {
      def delete(): Unit
    }

    final class Access private {
      def reader(document: Document): Reader = document.capabilities.reader

      def writer(document: Document): Writer = document.capabilities.writer

      def deleter(document: Document): Deleter = document.capabilities.deleter
    }

    object Access {
      private val instance = new Access()

      def apply(): Access = instance
    }

    def apply(owner: String, path: java.nio.file.Path): Document = {
      new Document(owner, path)
    }

  }

  private def maybeWriter(capabilities: Set[Capability]): Option[Writer] = {
    capabilities.collectFirst {
      case writer: Writer => writer
    }
  }

  private def maybeReader(capabilities: Set[Capability]): Option[Reader] = {
    capabilities.collectFirst {
      case reader: Reader => reader
    }
  }

}

// #gatekeeper