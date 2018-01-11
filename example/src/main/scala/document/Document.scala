package document

import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file._

import com.tersesystems.capabilities._

import scala.concurrent._
import scala.util._

// You can read and write a document.
final class Document(val owner: String,
                     private[document] val path: Path) {

  def read()(implicit context: SecurityContext): Future[String] = {
    Future.fromTry {
      Readable(this) { reader =>
        Future {
          blocking {
            reader.readLine()
          }
        }(ExecutionContext.global)
      }
    }.flatten
  }

  def write(newText: String)(implicit context: SecurityContext): Future[Unit] = {
    Future.fromTry {
      Writable(this, StandardOpenOption.SYNC) { writer =>
        Future {
          blocking {
            writer.write(newText)
          }
        }(ExecutionContext.global)
      }
    }
  }

  override def toString: String= s"Document(owner = $owner)"
}

case object Readable extends Authority[Document] {
  override type Capability[DSO] = ReadableCapability
  override type Args = Charset

  override protected def newCapability[DSO <: Document](so: DSO, args: Args*): Try[Capability[DSO]] = {
    Success(new Capability(so, args.headOption.getOrElse(StandardCharsets.UTF_8)))
  }

  class ReadableCapability private[Readable](document: Document, charset: Charset) {
    def readLine(): String = {
      val reader = Files.newBufferedReader(document.path, charset)
      try {
        reader.readLine()
      } finally {
        reader.close()
      }
    }
  }
}

case object Writable extends Authority[Document] {
  override type Capability[DSO] = WritableCapability
  override type Args = OpenOption

  override protected def newCapability[DSO <: Document](so: DSO, args: Args*): Try[Capability[DSO]] = {
    Success(new Capability(so, args:_*))
  }

  class WritableCapability private[Writable](document: Document, options: Args*) {
    def write(text: String): Unit = {
      val writer = Files.newBufferedWriter(document.path, options: _*)
      try {
        writer.write(text)
        writer.flush()
      } finally {
        writer.close()
      }
    }
  }
}
