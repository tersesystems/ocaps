/*
 * Copyright (C) 2018 Will Sargent. <http://www.tersesystems.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ocaps.example

import java.util.concurrent.CountDownLatch

import ocaps._
import ocaps.macros._
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.util.Try

object Modulation {
  final case class Foo(private val name: String) {
    private object capabilities {
      val doer: Foo.Doer = new Foo.Doer {
        override def doTheThing(): Int =  {
          42
        }
      }
    }
  }

  object Foo {
    sealed trait Doer {
      def doTheThing(): Int
    }

    class Access private {
      def doer(foo: Foo): Doer = foo.capabilities.doer
    }

    object Access {
      def apply(): Access = new Access
    }
  }

  // #manual
  def manualDoer(doer: Foo.Doer): Foo.Doer = {
    new Foo.Doer {
      override def doTheThing(): Int = doer.doTheThing()
    }
  }
  // #manual

  // #countable
  def countBasedExpiration(doer: Foo.Doer, count: Int): Foo.Doer = {
    val countDownLatch = new CountDownLatch(count)
    val Revocable(revocableDoer, revoker) = revocable[Foo.Doer](doer)
    val before: String => Unit = _ => countDownLatch.countDown()
    val after: (String, Any) => Unit = (_, _) =>
     if (countDownLatch.getCount == 0) {
       revoker.revoke()
     }
    modulate[Foo.Doer](revocableDoer, before, after)
  }
  // #countable

  // #timer
  def timerBasedExpiration(doer: Foo.Doer, duration: FiniteDuration): Foo.Doer = {
    val deadline = duration.fromNow
    val Revocable(revocableDoer, revoker) = revocable[Foo.Doer](doer)
    val before: String => Unit = _ => ()
    val after: (String, Any) => Unit = { (_, _) =>
      if (deadline.isOverdue()) {
        revoker.revoke()
      }
    }
    modulate[Foo.Doer](revocableDoer, before, after)
  }
  // #timer

  // #logging
  def loggingDoer(doer: Foo.Doer, logger: Logger): Foo.Doer = {
    val before: String => Unit = methodName => {
      logger.info(s"$methodName: before call")
    }
    val after: (String, Any) => Unit = (methodName, result) =>
      logger.info(s"$methodName: after returns $result")
    modulate[Foo.Doer](doer, before, after)
  }
  // #logging

  def main(args: Array[String]): Unit = {
    val access = Foo.Access()
    val foo = new Foo("foo")
    val doer = access.doer(foo)

    val logger = org.slf4j.LoggerFactory.getLogger("modulation.Foo.Doer")
    val modulatedDoer = countBasedExpiration(loggingDoer(doer, logger), 1)
    val result1 = Try(modulatedDoer.doTheThing())
    println(s"result after first call: $result1")

    val result2 = Try(modulatedDoer.doTheThing())
    println(s"result after second call: $result2")
  }
}
