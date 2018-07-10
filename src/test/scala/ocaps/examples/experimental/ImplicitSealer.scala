package ocaps.examples.experimental

import ocaps._

object ImplicitSealer {

  case class Foo(text: String) {
    def matches(box: Brand.Box[Foo]): Boolean = {
      Foo.brand.unsealer(box).contains(this)
    }
  }

  object Foo {
    private val brand: Brand[Foo] = Brand.create[Foo]("Brand for singleton object Foo")

    // implicit sealer
    implicit val sealer: Brand.Sealer[Foo] = brand.sealer
  }

  def main(args: Array[String]): Unit = {
    val foo = new Foo("I am foo!")
    val foo2 = new Foo("I am foo2!")
    val boxedFoo: Brand.Box[Foo] = foo // boxed automatically using the implicit sealer

    println(s"foo = boxedFoo? ${foo.matches(boxedFoo)}")
    println(s"foo2 = boxedFoo? ${foo2.matches(boxedFoo)}")
  }
}
