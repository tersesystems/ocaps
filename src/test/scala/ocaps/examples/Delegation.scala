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
package ocaps.examples

// #delegation
import ocaps._
import scala.util._

object Delegation {
  import Foo._

  final class Foo(private var name: String) {
    private def doTheThing() = {
      println(s"$name.doTheThing()")
    }

    private object capabilities {
      val doer: Doer = new Doer {
        override def doTheThing(): Unit = Foo.this.doTheThing()
      }
    }
  }

  object Foo {

    trait Doer {
      def doTheThing(): Unit
    }

    class Access private {
      def doer(foo: Foo): Doer = foo.capabilities.doer
    }

    object Access {
      def apply(): Access = new Access
    }
  }

  class User(name: String) {
    def setDoer(doer: Doer) = {
      maybeUser = Option(doer)
    }

    def delegateDoer(otherUser: User): Unit = {
      maybeUser.foreach { myDoer =>
        otherUser.setDoer(myDoer)
      }
    }

    private var maybeUser: Option[Doer] = None

    def doTheThing() = {
      try {
        maybeUser.foreach(_.doTheThing())
      } catch {
        case e: RevokedException =>
          e.printStackTrace()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val access = Foo.Access()

    val foo = new Foo("foo")
    val doer = access.doer(foo)

    val alice = new User("alice")
    alice.setDoer(doer)
    val bob = new User("alice")
    alice.delegateDoer(bob)

    println(s"bob doing the thing through delegation:")
    val result = Try(bob.doTheThing())
    println(s"bob result: $result")
  }
}
// #delegation
