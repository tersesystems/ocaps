package ocaps.examples.experimental

import ocaps._

object Validator {

  class FooFactory(name: String) {
    private val selfBrand = Brand.create[FooFactory](s"brand for $name")

    def create(name: String): Foo = {
      val box = selfBrand.sealer(this)
      println(s"name = $name, box = $box")
      Foo(name, box)
    }

    def validate(foo: Foo): Boolean = {
      val maybeFactory = selfBrand.unsealer(foo.source)
      println(s"foo = $foo, maybeFactory = $maybeFactory, source = ${foo.source}")
      maybeFactory.contains(this)
    }

    override def toString: String = {
      name
    }
  }

  case class Foo(name: String, source: Brand.Box[FooFactory])

  def main(args: Array[String]): Unit = {
    val fooFactory1 = new FooFactory("factory1")
    val foo1 = fooFactory1.create("foo1")

    val fooFactory2 = new FooFactory("factory2")
    val foo2 = fooFactory2.create("foo2")

    println(s"foo1 is product of foofactory1? ${fooFactory1.validate(foo1)}")
    val foo1factory2 = fooFactory2.validate(foo1)
    println(s"foo1 is product of foofactory2? ${foo1factory2}")
    val foo2factory1 = fooFactory1.validate(foo2)
    println(s"foo2 is product of foofactory1? ${foo2factory1}")
    println(s"foo2 is product of foofactory2? ${fooFactory2.validate(foo2)}")
  }
}
