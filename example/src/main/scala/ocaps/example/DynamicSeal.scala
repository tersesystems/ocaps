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

import ocaps.Brand

// Brands allow for "flexible private fields" where
// you have lexical scoping or in situations where
// access modifiers are not suitable (i.e. there is not
// a containing class)
object DynamicSeal {

  sealed trait Message

  case object Save extends Message

  case object Kill extends Message

  sealed trait Decision

  case object Saved extends Decision

  case object Killed extends Decision

  // All of the users are of the same type, and so private fields
  // are no good here.  In addition, the boxed field is public
  // so anyone who is asking and who has the unsealer can see it.
  case class User(name: String,
                  sentencer: User => User = identity,
                  boxed: Option[Brand.Box[Message]] = None,
                  private val brand: Option[Brand[Message]] = None) {
    def sentence(user: User): User = sentencer(user)

    def process(user: User): Option[Message] = {
      for {
        box <- user.boxed
        brand <- brand
        message <- brand.unapply(box)
      } yield message
    }
  }

  def main(args: Array[String]): Unit = {
    val softBrand = Brand.create[Message]("Brand for Judge Softtouch")
    val doomBrand = Brand.create[Message]("Brand for Judge Doom")

    val judgeSofttouch = User("Judge Softtouch",
      sentencer = { user => user.copy(boxed = Some(softBrand(Save))) },
      brand = Some(softBrand))

    val judgeDoom = User("Judge Doom",
      sentencer = { user => user.copy(boxed = Some(doomBrand(Kill))) },
      brand = Some(doomBrand))

    val steve = judgeDoom.sentence(User("steve"))
    val will = judgeSofttouch.sentence(User("will"))
    val judgedDoom = judgeSofttouch.sentence(judgeDoom)

    val steveDecision = judgeDoom.process(steve)
    println(s"User ${steve.name} has message ${steve.boxed} and decision $steveDecision")
    val willDecision = judgeSofttouch.process(will)
    println(s"User ${will.name} has message ${will.boxed} and decision $willDecision")

    // What's going on here...
    val judgeDecision = judgedDoom.process(judgedDoom)
    println(s"User ${judgedDoom.name} has message ${judgedDoom.boxed} and decision $judgeDecision")
  }

}
