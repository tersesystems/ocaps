package ocaps.examples.experimental

import java.util.UUID

import ocaps._

import scala.language.dynamics

object DynamicMembrane {


  class Foo extends Dynamic {
    // https://blog.scalac.io/2015/05/21/dynamic-member-lookup-in-scala.html
    // https://stackoverflow.com/questions/15799811/how-does-type-dynamic-work-and-how-to-use-it
    def selectDynamic(name: String) = name
  }

  def main(args: Array[String]): Unit = {
    val (sealer, unsealer) = Brand.create[Foo](UUID.randomUUID().toString).tuple
    val foo = new Foo
    val sealedFoo = sealer(foo)

    sealedFoo.hint == sealer.hint

    println(foo.bar)
  }

}
