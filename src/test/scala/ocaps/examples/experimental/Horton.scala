package ocaps.examples.experimental

import ocaps.Brand
import ocaps.Brand.Sealer

import scala.collection.mutable
import scala.language.dynamics

class Horton {

  // sealer(who) / unsealer(be)

  // http://www.erights.org/elib/capability/horton/horton-talk.pdf
  // http://www.erights.org/elib/capability/horton/amplify.html
  // http://www.erights.org/elib/capability/horton/base.html
  // http://www.erights.org/elib/capability/horton/

  class A(b: B, c: C) extends Dynamic {
    def start= {
      b.foo(c)
    }
  }

  class B extends Dynamic {
    def foo(c: C) = {
      c.hi(c)
    }
  }

  class C extends Dynamic {
    def hi(c: C) = println("hi")
  }

  // https://blog.scalac.io/2015/05/21/dynamic-member-lookup-in-scala.html
  // https://stackoverflow.com/questions/15799811/how-does-type-dynamic-work-and-how-to-use-it
  class Principal(label: String) extends Dynamic {
    type Who = String

    private val brand = Brand.create(label)

    def selectDynamic(name: String) = name

    def who: Sealer = brand.sealer

    def encodeFor[T](message: T, who: Sealer): Brand.Box[T] = ???

    def decodeFrom[T](gs: Brand.Box[T], who: Sealer): T = ???

    //    def principal {
    //      to __printOn(out :TextWriter) {
    //        out.print(label)
    //      }
    //      to who() {
    //        return whoMe
    //      }
    //      to encodeFor(targ, whoBlame) {
    //        def stub := makeStub(whoBlame, targ)
    //        return wrap(stub, whoBlame)}
    //      to decodeFrom(gift, whoBlame) {
    //        def stub := unwrap(gift, whoBlame)
    //        return makeProxy(whoBlame, stub)
    //      }
    //    }

    //  def makeQuoteln := <elang:interp.makeQuoteln>
    //  def makeBrand := <elib:sealing.makeBrand>
    //  def makeWeakKeyMap := <unsafe:org.erights.e.elib.tables.makeWeakKeyMap> # added

    private val proxyAmps = new mutable.WeakHashMap[Any, Any]()

    // Returns stub?
    private def makeStub(whoBlame: Sealer, targ: Any): Any = {
      ???
    }

    //  # E sample
    //
    //def makePrincipal(label :String) {
    //  def reportln := makeQuoteln(println, `$label said:`, 77)
    //  def [whoMe, beMe] := makeBrand(label)
    //  def proxyAmps := makeWeakKeyMap()                                   # added
    //
    //  def makeProxy(whoBlame, stub) {
    //    def log := makeQuoteln(reportln,
    //      `I ask ${whoBlame.getBrand()} to:`,
    //      75)
    //    def proxy {
    //      # getGuts method removed
    //
    //      # as P1
    //      match [verb, [p2]] {
    //        log(`$verb/1`)
    //        def [s2, whoCarol] := proxyAmps[p2]                     # changed
    //        def gs3 := s2.intro(whoBlame)
    //        def p3Desc := [gs3, whoCarol]
    //        stub.deliver(verb, [p3Desc])
    //      }
    //    }
    //    proxyAmps[proxy] := [stub, whoBlame]                            # added
    //    return proxy
    //  }
    //
    //  # as S2
    //  def wrap(s3, whoBob) {
    //    def provide(fillBox) {
    //      def fill := beMe.unseal(fillBox)
    //      fill(s3)
    //    }
    //    return whoBob.seal(provide)
    //  }
    //
    //  # as S1
    //  def unwrap(gs3, whoCarol) {
    //    def provide := beMe.unseal(gs3)
    //    var result := null
    //    def fill(s3) {
    //      result := s3
    //    }
    //    def fillBox := whoCarol.seal(fill)
    //    provide(fillBox)
    //    return result
    //  }
    //
    //  def makeStub(whoBlame, targ) {
    //    def log := makeQuoteln(reportln,
    //      `${whoBlame.getBrand()} asks me to:`,
    //      75)
    //    def stub {
    //      # as S2
    //        to intro(whoBob) {
    //        log(`meet ${whoBob.getBrand()}`)
    //        def s3 := makeStub(whoBob, targ)
    //        return wrap(s3, whoBob)
    //      }
    //      # as S1
    //        to deliver(verb, [p3Desc]) {
    //        log(`$verb/1`)
    //        def [gs3, whoCarol] := p3Desc
    //        def s3 := unwrap(gs3, whoCarol)
    //        def p3 := makeProxy(whoCarol, s3)
    //        E.call(targ, verb, [p3])
    //      }
    //    }
    //    return stub
    //  }
    //
    //  def principal {
    //    to __printOn(out :TextWriter) {
    //      out.print(label)
    //    }
    //    to who() {
    //      return whoMe
    //    }
    //    to encodeFor(targ, whoBlame) {
    //      def stub := makeStub(whoBlame, targ)
    //      return wrap(stub, whoBlame)}
    //    to decodeFrom(gift, whoBlame) {
    //      def stub := unwrap(gift, whoBlame)
    //      return makeProxy(whoBlame, stub)
    //    }
    //  }
    //  return principal
    //  }

  }

  object Principal {
    def apply(name: String): Principal = ???
  }

  def main(args: Array[String]): Unit = {
    val b = new B()
    val c = new C()

    val alice = Principal("Alice")
    val bob = Principal("Bob")
    val carol = Principal("Carol")

    val gs1 = bob.encodeFor(b, alice.who)

    val gs2 = carol.encodeFor(c, alice.who)

    val p1: B = alice.decodeFrom(gs1, bob.who)
    val p2: C = alice.decodeFrom(gs2, carol.who)
    val a = new A(p1, p2)

    a.start
  }

}
