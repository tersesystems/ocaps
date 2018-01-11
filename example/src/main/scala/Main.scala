import java.nio.file.Files

import com.tersesystems.capabilities._
import document._
import fruit._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util._

object Main {
  case class User(name: String)

  val jeff = User("jeff")
  val steve = User("steve")
  val mutt = User("mutt")
  val users: Set[User] = Set(jeff, steve, mutt)

  val fruitAuthorizer = new FruitAuthorizer(
    appleGroup = Set(jeff, steve),
    pearGroup = Set(steve, mutt)
  )

  def main(args: Array[String]): Unit = {
    testFruit()
    testDocuments()
  }

  def testFruit(): Unit = {
    def eat[F <: Fruit](fruit: F)(implicit context: SecurityContext): Try[F] = {
      Eatable(fruit) { eatCapability =>
        // We have permission to eat only inside this scope -- this capability lets us change the fruit internal state
        eatCapability.eat()
        // after this point the fruit is eaten, and cannot be eaten again...
        // eatCapability.eat()
      }
    }

    for (currentUser <- users) {
      implicit val ctx: SecurityContext = new ExampleSecurityContext(currentUser)
      val apple = new Apple()
      val pear = new Pear()

      println(s"${currentUser.name} eating $apple:")
      val triedApple: Try[Apple] = eat(apple)
      println(s"  $triedApple")
      // will not compile
      //val eatenPear: Pear = eat(apple)

      println(s"${currentUser.name} eating $pear:")
      val triedPear: Try[Pear] = eat(pear)
      println(s"  $triedPear")
      println()

      // Does not compile!
      //println(s"Is a pear readable? ${Readable(pear)}")
    }
  }

  def testDocuments(): Unit = {
    val documents = Seq(
      createDocument(jeff),
      createDocument(mutt),
      createDocument(steve)
    )

    for (currentUser <- users) {
      implicit val ctx: ExampleSecurityContext = new ExampleSecurityContext(currentUser)

      for (doc <- documents) {
        testDocument(doc)
      }
    }
  }

  def createDocument(user: User): Document = {
    // We can also ensure that even the document doesn't have access to things it can't read or write.
    val doc = new Document(owner = user.name, Files.createTempFile(null, ".txt"))
    implicit val ctx: ExampleSecurityContext = new ExampleSecurityContext(user)
    Await.result(doc.write(s"Created by ${user.name}"), Duration.Inf)
    doc
  }

  def testDocument(doc: Document)(implicit ctx: ExampleSecurityContext): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    val currentUser = ctx.user.name

    doc.write(s"Written to by ${ctx.user.name}").onComplete {
      case Success(result) =>
        println(
          s"""$currentUser writing ${doc.owner}'s document:
             |   successful write!
             |""".stripMargin)
        readDocument(doc)

      case Failure(e) =>
        println(
          s"""$currentUser writing ${doc.owner}'s document:
             |   unsuccessful write!
             |""".stripMargin)
        readDocument(doc)
    }

  }

  def readDocument(doc: Document)(implicit ctx: ExampleSecurityContext): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    val currentUser = ctx.user.name

    // Only read after the write is complete...
    doc.read().onComplete {
      case Success(result) =>
        println(
          s"""$currentUser reading ${doc.owner}'s document:
             |   successful read "$result"
             |""".stripMargin)

      case Failure(e) =>
        println(
          s"""$currentUser reading ${doc.owner}'s document:
             |   unsuccessful read!
             |""".stripMargin)

    }
  }

  class FruitAuthorizer(appleGroup: Set[User],
                        pearGroup: Set[User]) {

    def authorize(user: User, authority: Authority[_], fruit: Fruit): Boolean = {
      authority match {
        case Eatable =>
          fruit match {
            case a: Apple =>
              appleGroup.exists(u => u.name == user.name)
            case p: Pear =>
              pearGroup.exists(u => u.name == user.name)
          }
      }
    }
  }

  class ExampleSecurityContext(val user: User) extends SecurityContext {
    override def authorize[DSO](authority: Authority[DSO], so: DSO): Boolean = {
      so match {
        case document: Document =>
          authority match {
            case Readable =>
              true
            case Writable =>
              document.owner.equals(user.name)
          }
        case fruit: Fruit =>
          fruitAuthorizer.authorize(user, authority, fruit)

        case _ =>
          false
      }
    }
  }
}
