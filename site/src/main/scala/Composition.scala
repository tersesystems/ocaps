/*
 * Copyright 2018 Will Sargent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ocaps._
import ocaps.macros._

object Composition {

  final class Foo(private var name: String) {
    private object capabilities {
      val doer: Foo.Doer = new Foo.Doer {
        override def doTheThing(): Unit =  {
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

    trait Derper {
      def derp(): Unit
    }

    class Access private {
      def doer(foo: Foo): Doer = (foo.capabilities.doer)
      def changer(foo: Foo): Changer = (foo.capabilities.changer)
    }

    object Access {
      def apply(): Access = new Access
    }
  }

  // #usage
  def main(args: Array[String]): Unit = {
    import Foo._
    val access = Foo.Access()
    val foo = new Foo("foo")
    val doer: Doer = access.doer(foo)
    val changer: Changer = access.changer(foo)
    val derper: Derper = () => println("derp!")
    val doerChangerDerper = compose[Doer with Changer with Derper](doer, changer, derper)

    // composition is often used when you want to return a "set" of capabilities after
    // some authorization event has taken place, after which you can do some pattern matching
    doerChangerDerper match {
      case d: Derper =>
        d.derp()
    }

    // this of course works with all of the compound types, but you can also use attenuation
    // to pull out a particular capability.
    val attenuatedChanger: Changer = doerChangerDerper match {
      case c: Changer =>
        attenuate[Changer](c)
    }
    attenuatedChanger.changeName("bar")
  }
  // #usage
}
