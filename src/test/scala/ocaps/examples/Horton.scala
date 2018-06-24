package ocaps.examples

class Horton {

  // http://www.erights.org/elib/capability/horton/horton-talk.pdf
  // http://www.erights.org/elib/capability/horton/amplify.html
  // http://www.erights.org/elib/capability/horton/base.html
  // http://www.erights.org/elib/capability/horton/
  //
  //  class A(b: B, c: C) {
  //    def start {
  //      b.foo(c)
  //    }
  //  }
  //
  //  class B {
  //    def foo(c: C) = {
  //      c.hi(c)
  //    }
  //  }
  //
  //  class C {
  //    def hi(c: C) = println("hi")
  //  }
  //
  //  def makePrincipal(label :String) {
  //
  //    def makeQuoteln(printer: String => Unit, message: String, indent: Int): Unit =  {
  //      printer(message) // indent is not used right
  //    }
  //
  //    def reportln = makeQuoteln(println, s"$label said:", 77)
  //
  //    val brand = ocaps.Brand.create(label)
  //    val (whoMe, beMe) = brand.tuple
  //  }

  /*
  def alice := makePsrincipal("Alice")
  # value: Alice

  ? def bob := makePrincipal("Bob")
  # value: Bob

  ? def carol := makePrincipal("Carol")
  # value: Carol
  Initial connectiivity:

  ? def gs1 := bob.encodeFor(b, alice.who())

  ? def gs2 := carol.encodeFor(c, alice.who())

  ? def p1  := alice.decodeFrom(gs1, bob.who())
  ? def p2  := alice.decodeFrom(gs2, carol.who())
  ? def a := makeA(p1, p2)

   */
}
