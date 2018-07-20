package ocaps.examples.horton


/**
 * Implementation of Horton in Scala.
 *
 * http://www.erights.org/elib/capability/horton/amplify.html
 */
// #main
object Main {
  import Principal.ProxyMaker

  class A(b: B, c: C) {
    def start(): Unit = {
      b.foo(c)
    }
  }

  trait B {
    def foo(c: C): Unit
  }

  object B {
    implicit val proxyMaker: ProxyMaker[B] = new ProxyMaker[B] {
      override def makeProxy(stub: Stub[B])(implicit context: Context): Proxy[B] = {
        import context._

        def log(msg: String): Unit = {
          reportln(s"I ask ${whoBlame.hint} to:\n> $msg")
        }

        new B {
          override def foo(c: C): Unit = {
            log("> foo/1")
            stub.deliver("foo", c) // createDescription implicitly converts c
          }

          override def toString: String = "B Proxy"
        }
      }
    }
  }

  trait C {
    def hi(): Unit
  }

  object C {
    implicit val proxyMaker: ProxyMaker[C] = new ProxyMaker[C] {
      override def makeProxy(stub: Stub[C])(implicit context: Context): C = {
        import context._

        def log(msg: String): Unit = {
          reportln(s"I ask ${whoBlame.hint} to:\n> $msg")
        }

        new C {
          override def hi(): Unit = {
            log("> hi/1")
            stub.deliver("hi", this) // createDescription implicitly converts this
          }

          override def toString: String = "C Proxy"
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val b = new B {
      def foo(c: C): Unit = {
        c.hi()
      }

      override def toString: String = "B"
    }
    val c = new C {
      def hi(): Unit = println("hi")

      override def toString: String = "C"
    }

    val alice = new Principal("Alice", println)
    val bob = new Principal("Bob", println)
    val carol = new Principal("Carol", println)

    // Gifts are sealed and can only be unsealed by the recipient.
    val toAliceFromBob: Gift[B] = bob.encodeFor(b, alice.who)
    val toAliceFromCarol: Gift[C] = carol.encodeFor(c, alice.who)

    // When gift is unsealed, a proxy is returned that establishes provenance.
    val p1: B = alice.decodeFrom(toAliceFromBob, bob.who)
    val p2: C = alice.decodeFrom(toAliceFromCarol, carol.who)
    val a = new A(p1, p2)

    /*
      Alice said:
      > I ask Bob to:
      > > foo/1
      Carol said:
      > Alice asks me to:
      > > meet Bob
      Bob said:
      > Alice asks me to:
      > > foo/1
      Bob said:
      > I ask Carol to:
      > > hi/1
      Carol said:
      > Bob asks me to:
      > > meet Carol
      Carol said:
      > Bob asks me to:
      > > hi/1
      hi
     */
    a.start()
  }

}
// #main
