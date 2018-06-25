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

// #modulation
import ocaps.macros._
import org.slf4j.Logger

import scala.util.Try

object Modulation {
  final case class Foo(private val name: String) {
    private object capabilities {
      val doer: Foo.Doer = new Foo.Doer {
        override def doTheThing(): Int = {
          42
        }
      }
    }
  }

  object Foo {
    trait Doer {
      def doTheThing(): Int
    }

    class Access {
      def doer(foo: Foo): Doer = foo.capabilities.doer
    }
  }

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
    val access = new Foo.Access()
    val foo = new Foo("foo")
    val doer = access.doer(foo)

    val logger = org.slf4j.LoggerFactory.getLogger("modulation.Foo.Doer")
    val logDoer = loggingDoer(doer, logger)
    val result1 = Try(logDoer.doTheThing())
    println(s"result after first call: $result1")

    val result2 = Try(logDoer.doTheThing())
    println(s"result after second call: $result2")
  }
}
// #modulation