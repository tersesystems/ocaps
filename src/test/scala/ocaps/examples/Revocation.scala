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

import ocaps._

import scala.util.control.NonFatal

// #revocation
object Revocation {

  final class Foo(name: String) {
    private def privateDoTheThing(): Unit = {
      println(s"$name.doTheThing()")
    }

    private object capabilities {
      import Foo._
      val doer: Doer = new Doer {
        override def doTheThing(): Unit = Foo.this.privateDoTheThing()
      }
    }
  }

  object Foo {
    trait Doer {
      def doTheThing(): Unit
    }

    // #manual
    object Doer {
      def revocable(doer: Doer): Revocable[Doer] = {
        Revocable(doer) { thunk =>
          new Doer {
            override def doTheThing(): Unit = thunk().doTheThing()
          }
        }
      }
    }
    // #manual

    class Access {
      def doer(foo: Foo): Doer = foo.capabilities.doer
    }
  }

  import java.util.concurrent.TimeUnit._
  import java.util.concurrent._

  private val scheduler = Executors.newScheduledThreadPool(1)

  // Guest has delegated authority to do the thing without having access to foo
  class Guest(doer: Foo.Doer) {
    def start(): Unit = {
      // Keep doing the thing forever, every second.
      val doTheThing: Runnable = { () =>
        {
          try {
            doer.doTheThing()
          } catch {
            case NonFatal(e) => println("Cannot do the thing!")
          }
        }
      }
      scheduler.scheduleAtFixedRate(doTheThing, 0, 1L, SECONDS)
    }
  }

  // Revoker doesn't know about capability, only has kill switch
  class ScheduledRevoker(revoker: Revoker) {
    def start(): Unit = {
      // After three seconds, the admin decides to stop you doing the thing.
      val adminRevoke: Runnable = () => revoker.revoke()
      scheduler.schedule(adminRevoke, 3L, SECONDS)
    }
  }

  def main(args: Array[String]): Unit = {
    import Foo._
    val foo = new Foo("foo")

    val access = new Access
    val doer = access.doer(foo)

    // macro generates code equivalent to the `revocable` code above
    //val Revocable(revocableDoer, revoker) = Foo.Doer.revocable(doer)
    val Revocable(revocableDoer, revoker) = macros.revocable[Doer](doer)

    new Guest(revocableDoer).start()
    new ScheduledRevoker(revoker).start()

    // After five seconds, exit program.
    val shutdown: Runnable = () => scheduler.shutdown()
    scheduler.schedule(shutdown, 5L, SECONDS)
  }

}
// #revocation