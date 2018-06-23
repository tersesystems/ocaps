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

// #amplification
import ocaps._

object Amplification {

  case class Food(name: String)

  case class Can(food: Brand.Box[Food])

  class CanOpener(unsealer: Brand.Unsealer[Food]) {
    def open(can: Can): Food = {
      unsealer(can.food).get
    }
  }

  def main(args: Array[String]): Unit = {
    // We want to get at the food here.

    val (sealer, unsealer) = Brand.create[Food]("canned food").tuple
    val canOfSpam: Can = Can(sealer(Food("spam")))

    // The can by itself has the food, but we have no way to get to it
    val cannedFood: Brand.Box[Food] = canOfSpam.food
    println(s"food = ${cannedFood.toString}") // DOES NOT WORK

    // The can opener by itself can open cans, but if we don't have a can
    // then there's also no food.
    val canOpener = new CanOpener(unsealer)

    // We need both Can and CanOpener.
    def openCan(can: Can, canOpener: CanOpener) = {
      val food: Food = canOpener.open(can) // DOES WORK

      println(s"food = $food")
    }

    openCan(canOfSpam, canOpener)
  }
}
// #amplification