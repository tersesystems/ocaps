package horton

import java.util.concurrent.atomic.AtomicReference

import ocaps.Brand._
import ocaps._

import scala.collection.mutable
import reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

/**
 * Implementation of Horton in Scala.
 *
 * http://www.erights.org/elib/capability/horton/amplify.html
 */
object Horton {

  class A(b: B, c: C) {
    def start = {
      b.foo(c)
    }
  }

  trait B {
    def foo(c: C): Unit
  }

  object B {
    implicit val proxyMakerB: ProxyMaker[B] = new ProxyMaker[B] {
      override def makeProxy(reportln: String => Unit, whoBlame: Who, stub: Stub[B])(implicit principal: Principal): Proxy[B] = {
        def log(msg: String) = {
          reportln(s"I ask ${whoBlame.hint} to:\n> $msg")
        }

        val proxyB: B = new B {
          override def foo(c: C): Unit = {
            log("> foo/1")

            val (stubC, whoCarol) = principal.proxyAmps(c)
            val gift = stubC.intro(whoBlame)
            val p3Desc = (gift, whoCarol)
            stub.deliver("foo", p3Desc)
          }

          override def toString: String = "B Proxy"
        }
        principal.proxyAmps.put(proxyB, (stub, whoBlame))
        proxyB
      }
    }
  }

  trait C {
    def hi(): Unit
  }

  object C {
    implicit val proxyMakerC: ProxyMaker[C] = new ProxyMaker[C] {
      override def makeProxy(reportln: String => Unit, whoBlame: Who, stub: Stub[C])(implicit principal: Principal): C = {
        def log(msg: String) = {
          reportln(s"I ask ${whoBlame.hint} to:\n> $msg")
        }

        val proxyC: C = new C {
          override def hi(): Unit = {
            log("> hi/1")

            val (stubToIntro, whoFrom) = principal.proxyAmps(this)
            val gift = stubToIntro.intro(whoBlame)
            val desc = (gift, whoFrom)
            stub.deliver("hi", desc)
          }

          override def toString: String = "C Proxy"
        }
        principal.proxyAmps.put(proxyC, (stub, whoBlame))
        proxyC
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

    val alice = new Principal("Alice")
    val bob = new Principal("Bob")
    val carol = new Principal("Carol")

    val gs1 = bob.encodeFor(b, alice.who)
    val gs2 = carol.encodeFor(c, alice.who)

    val p1: B = alice.decodeFrom(gs1, bob.who)
    val p2: C = alice.decodeFrom(gs2, carol.who)
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
    a.start
  }

  trait Fill[T] extends (Stub[T] => Unit)

  type FillBox[T] = Box[Fill[T]]

  trait Provide[T] extends (FillBox[T] => Unit)

  type Gift[T] = Box[Provide[T]]

  trait Wrap[T] extends ((Stub[T], Who) => Gift[T])

  trait Unwrap[T] extends ((Gift[T], Who) => Stub[T])

  type Proxy[T] = T

  class Who(sealer: Sealer) {
    def apply[T](provide: Provide[T]): Gift[T] = sealer(provide)

    def apply[T](fill: Fill[T]): FillBox[T] = sealer(fill)

    def hint: Hint = sealer.hint

    override def toString: String = s"Who(sealer = $sealer)"
  }

  class Be(unsealer: Unsealer) {
    def apply[T](gift: Gift[T]): Provide[T] = {
      require(gift.hint == unsealer.hint, s"Gift hint ${gift.hint} does not match unsealer hint ${unsealer.hint}")
      unsealer(gift).get
    }

    def apply[T](fillbox: FillBox[T]): Fill[T] = {
      require(fillbox.hint == unsealer.hint, s"Fillbox hint ${fillbox.hint} does not match unsealer hint ${unsealer.hint}")
      unsealer(fillbox).get
    }

    def hint: Hint = unsealer.hint

    override def toString: String = s"Be(unsealer = $unsealer)"
  }

  trait Stub[T] {
    def intro(whoBob: Who): Gift[T]

    def deliver[ArgType](verb: String, desc: (Gift[ArgType], Who))
                        (implicit tTag: TypeTag[T], cTag: ClassTag[T], proxyMaker: ProxyMaker[ArgType]): Any
  }

  trait ProxyMaker[T] {
    def makeProxy(reportln: String => Unit, whoBlame: Who, stub: Stub[T])(implicit principal: Principal): Proxy[T]
  }

  trait ProxyAmps {
    def put[T](key: T, value: (Stub[T], Who)): Unit

    def apply[T](key: T): (Stub[T], Who)
  }

  class Principal(label: String) {
    val proxyAmps = new ProxyAmpsImpl()

    private val (whoMe, beMe) = {
      val brand = Brand.create(label)
      (new Who(brand.sealer), new Be(brand.unsealer))
    }

    val who: Who = whoMe

    def encodeFor[T](targ: T, whoBlame: Who): Gift[T] = {
      val stub = makeStub(whoBlame, targ)
      wrap(stub, whoBlame)
    }

    def decodeFrom[T](gift: Gift[T], whoBlame: Who)(implicit proxyMaker: ProxyMaker[T]): T = {
      val stub = unwrap(gift, whoBlame)
      proxyMaker.makeProxy(reportln, whoBlame, stub)(this)
    }

    def makeStub[T](who: Horton.Who, t: T): StubImpl[T] = {
      new StubImpl(who, t)
    }

    override def toString: String = s"Principal($label)"

    private def reportln(msg: String): Unit = {
      println(s"$label said:\n> $msg")
    }

    private def wrap[T](stub: Stub[T], whoBlame: Who): Gift[T] = {
      val provide: Provide[T] = { fillBox: FillBox[T] =>
        val fill: Fill[T] = beMe(fillBox)
        fill(stub)
      }
      whoBlame(provide)
    }

    private def unwrap[T](gs3: Gift[T], whoCarol: Who): Stub[T] = {
      val provide: Provide[T] = beMe(gs3)
      val result = new AtomicReference[Stub[T]]()
      val fill: Fill[T] = s3 => result.set(s3)
      val fillBox: FillBox[T] = whoCarol(fill)
      provide(fillBox)
      result.get()
    }

    class StubImpl[T](whoBlame: Who, delegate: T) extends Stub[T] with Dynamic {

      def intro(whoBob: Who): Gift[T] = {
        log(s"meet ${whoBob.hint}")
        val stub = new StubImpl[T](whoBob, delegate)
        wrap(stub, whoBob)
      }

      def deliver[ArgType](verb: String, desc: (Gift[ArgType], Who))
                          (implicit tTag: TypeTag[T], cTag: ClassTag[T], proxyMaker: ProxyMaker[ArgType]): Any = {
        log(s"$verb/1")

        val (gift3, whoCarol) = desc
        val stub3: Stub[ArgType] = unwrap(gift3, whoCarol)
        val proxy: Proxy[ArgType] = proxyMaker.makeProxy(reportln, whoCarol, stub3)(Principal.this)
        applyDynamic(verb)(proxy)(tTag, cTag)
      }

      //  http://www.erights.org/elib/capability/horton/amplify.html
      // https://gist.github.com/bartschuller/4687387
      def applyDynamic(method: String)(args: Any*)(implicit tTag: TypeTag[T], classTag: ClassTag[T]): Any = {
        val m = ru.runtimeMirror(delegate.getClass.getClassLoader)
        val sym = ru.weakTypeTag[T].tpe.decl(ru.TermName(method)).asMethod
        val im = m.reflect(delegate)
        val methodMirror = im.reflectMethod(sym)
        methodMirror.apply(args: _*)
      }

      override def toString: String = {
        s"StubImpl(whoBlame = $whoBlame, delegate = $delegate, principal = $label)"
      }

      private def log(msg: String) = {
        reportln(s"${whoBlame.hint} asks me to:\n> > " + msg)
      }
    }

    class ProxyAmpsImpl() extends ProxyAmps {
      private val weakMap = mutable.WeakHashMap[AnyRef, (Stub[_], Who)]()

      def put[T](key: T, value: (Stub[T], Who)): Unit = {
        weakMap(key.asInstanceOf[AnyRef]) = value
      }

      def apply[T](key: T): (Stub[T], Who) = {
        weakMap(key.asInstanceOf[AnyRef]).asInstanceOf[(Stub[T], Who)]
      }
    }

  }

}

