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

object Attenuation {

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

    class Access {
      def doer(foo: Foo): Doer = foo.capabilities.doer
      def changer(foo: Foo): Changer = foo.capabilities.changer
    }
  }

  def main(args: Array[String]): Unit = {
    import Foo._

    val access = new Access()
    val foo = new Foo("foo")

    val doer: Doer = access.doer(foo)
    val changer: Changer = access.changer(foo)
    val derper: Derper = () => println("derp!")
    val doerChangerDerper = compose[Doer with Changer with Derper](doer, changer, derper)

    // #unsafe
    // We want an attenuation that makes only Doer available
    val castDoer: Doer = doerChangerDerper.asInstanceOf[Doer]
    // But we can recover Derper capability here!
    val doerAsDerper: Derper = castDoer.asInstanceOf[Derper]
    // whoops :-(
    doerAsDerper.derp()
    // #unsafe

    // #safe
    // Attenuation doesn't use downcasting, is safe!
    val attenuatedDoer: Doer = attenuate[Doer](doerChangerDerper)
    try {
      val downcastAttenuatedDerper = attenuatedDoer.asInstanceOf[Derper]
      attenuatedDoer.doTheThing()
    } catch {
      case e: ClassCastException =>
        println("Can't downcast to a different type using the macro!")
    }
    // #safe
  }
}
