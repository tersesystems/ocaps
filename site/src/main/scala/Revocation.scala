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

object Revocation {

  import scala.language.reflectiveCalls
  import scala.util._
  import scala.util.control.NonFatal

  final class Foo(name: String) {
    private[this] def privateDoTheThing(): Unit = {
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
    sealed trait Doer {
      def doTheThing(): Unit
    }

    // #caretaker
    object Doer {
      def caretaker(doer: Doer): Revocable[Doer] = {
        Revocable(doer) { thunk =>
          new Doer {
            override def doTheThing(): Unit = thunk().doTheThing()
          }
        }
      }
    }
    // #caretaker

    class Policy {
      def askForDoer(foo: Foo): Try[Doer] = Success(foo.capabilities.doer)
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

    val policy = new Policy
    val Revocable(cap, revoker) =
      policy.askForDoer(foo).map(Foo.Doer.caretaker).get
    new Guest(cap).start()
    new ScheduledRevoker(revoker).start()

    // After five seconds, exit program.
    val shutdown: Runnable = () => scheduler.shutdown()
    scheduler.schedule(shutdown, 5L, SECONDS)
  }

}
