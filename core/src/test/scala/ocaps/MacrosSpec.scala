package ocaps

import org.scalatest._

class MacrosSpec extends WordSpec with Matchers {

  final class Foo(private var name: String) {

    private object capabilities {
      val doer: Foo.Doer = new Foo.Doer {
        override def doTheThing(): Unit = {
          println(s"$name.doTheThing()")
        }
      }
      val changer: Foo.Changer = new Foo.Changer {
        override def changeName(name: String): Foo.this.type = {
          Foo.this.name = name
          Foo.this
        }
      }
    }

  }

  object Foo {

    sealed trait Doer {
      def doTheThing(): Unit
    }

    sealed trait Changer {
      def changeName(name: String): Foo
    }

    class Access private {
      def doer(foo: Foo): Doer = foo.capabilities.doer

      def changer(foo: Foo): Changer = foo.capabilities.changer
    }

    object Access {
      def apply(): Access = new Access
    }

  }

  "compose" should {
    "work" in {
      import Foo._
      val access = Foo.Access()
      val foo = new Foo("foo")
      val doer: Doer = access.doer(foo)
      val changer: Changer = access.changer(foo)
      val doerChanger = macros.compose[Doer with Changer](doer, changer)
      assert(doerChanger.isInstanceOf[Doer])
      assert(doerChanger.isInstanceOf[Changer])
    }
  }

  "attenuate" should {
    "work" in {
      import Foo._
      val access = Foo.Access()
      val foo = new Foo("foo")
      val doer: Doer = access.doer(foo)
      val changer: Changer = access.changer(foo)

      // Do straight composition
      val doerChanger = new Doer with Changer {
        override def doTheThing(): Unit = doer.doTheThing()

        override def changeName(name: String): Foo = changer.changeName(name)
      }
      val attenuatedDoer = macros.attenuate[Doer](doerChanger)
      assert(attenuatedDoer.isInstanceOf[Doer])
      assert(!attenuatedDoer.isInstanceOf[Changer])
    }
  }

  "modulate" should {
    "work" in {
      import Foo._
      val access = Foo.Access()
      val foo = new Foo("foo")
      val doer: Doer = access.doer(foo)
      val changer: Changer = access.changer(foo)

      // Do straight composition
      val doerChanger = new Doer with Changer {
        override def doTheThing(): Unit = doer.doTheThing()

        override def changeName(name: String): Foo = changer.changeName(name)
      }

      var beforeHook = false
      val before: String => Unit = { methodName =>
        beforeHook = true
      }
      var afterHook = false
      val after: (String, Any) => Unit = { (s, a) =>
        afterHook = true
      }
      val modulatedDoer = macros.modulate[Doer](doerChanger, before, after)
      modulatedDoer.doTheThing()
      beforeHook should be(true)
      afterHook should be(true)
    }
  }

  "revocable" should {
    "work" in {
      import Foo._
      val access = Foo.Access()
      val foo = new Foo("foo")
      val doer: Doer = access.doer(foo)

      val Revocable(revocableDoer, revoker) = macros.revocable[Doer](doer)
      revoker.revoke()
      assertThrows[RevokedException] {
        revocableDoer.doTheThing()
      }
    }
  }

}
