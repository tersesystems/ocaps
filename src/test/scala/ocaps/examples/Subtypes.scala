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

import scala.util.Try

// #subtypes
object Subtypes {

  import Fruit._

  sealed trait FruitState

  object FruitState {
    case object Uneaten extends FruitState
    case object Eaten extends FruitState
  }

  trait Fruit {
    import Fruit._

    private[this] var state: FruitState = FruitState.Uneaten

    def currentState: FruitState = state

    object capabilities {
      val eater: Eater[Fruit.this.type] = new Eater[Fruit.this.type] {
        def eat(): Fruit.this.type = {
          Fruit.this.state match {
            case FruitState.Uneaten =>
              Fruit.this.state = FruitState.Eaten
              Fruit.this

            case FruitState.Eaten =>
              throw new IllegalStateException("Already eaten!")
          }
        }
      }
    }
  }

  object Fruit {

    trait Eater[+F <: Fruit] {
      def eat(): F
    }

    object Eater {
      def logger[F <: Fruit](eater: Eater[F]): Eater[F] = new Eater[F] {
        override def eat(): F = {
          println("about to do the thing")
          eater.eat()
        }
      }
    }

    class Access {
      // Expose this capability raw, with no checks or anything.
      def eater[F <: Fruit](fruit: F): Eater[F] = fruit.capabilities.eater
    }
  }

  final class Apple extends Fruit {
    override def toString: String = s"Apple($currentState)"
  }

  final class Pear extends Fruit {
    override def toString: String = s"Pear($currentState)"
  }

  case class User(name: String, caps: Map[Fruit, Eater[_]] = Map.empty) {

    def canEat[F <: Fruit](fruit: F, eater: Eater[F]): User = {
      copy(caps = caps + (fruit -> eater))
    }

    def eats[F <: Fruit](fruit: F): Try[Option[F]] = {
      val triedFruit = Try {
        println(s"${name} eating $fruit:")

        caps.get(fruit).map { eater: Eater[_] =>
          val castEater = eater.asInstanceOf[Eater[F]] // Map strips off type info :-(
          Eater.logger(castEater).eat()
        }
      }
      println(s"  $triedFruit")
      triedFruit
    }
  }

  def main(args: Array[String]): Unit = {

    val access = new Access
    def grantEater[F <: Fruit](user: User, fruit: F): Eater[F] = {
      access.eater(fruit)
    }

    var steve = User("steve")
    val apple = new Apple()
    steve = steve.canEat(apple, grantEater(steve, apple))
    val eatenApple: Try[Option[Apple]] = steve.eats(apple)
    println(s"steve's apple = $eatenApple")

    val apple2 = new Apple()
    steve.eats(apple2)

    var mutt = User("mutt")

    val pear = new Pear()
    mutt = mutt.canEat(pear, grantEater(mutt, pear))
    val eatenPear: Try[Option[Pear]] = mutt.eats(pear)
    println(s"mutt's pear = $eatenPear")

    val jeff = User("jeff")
    val pear2 = new Pear()
    jeff.eats(pear2)
  }
}
// #subtypes
