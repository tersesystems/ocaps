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

package ocaps.example

import ocaps._

import scala.util.{Success, Try}

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

    sealed trait Doer {
      def doTheThing(): Unit
    }

    class Policy private {
      def askForDoer(foo: Foo): Try[Doer] = Success(foo.capabilities.doer)
    }

    object Policy {
      // restrict access to powerbox here
      def apply(): Policy = new Policy
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
    val powerbox = Foo.Policy()

    val foo = new Foo("foo")
    val doer = powerbox.askForDoer(foo).get

    val alice = new User("alice")
    alice.setDoer(doer)
    val bob = new User("alice")
    alice.delegateDoer(bob)

    println(s"bob doing the thing through delegation:")
    val result = Try(bob.doTheThing())
    println(s"bob result: $result")
  }
}
