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

object SealedMint {

  final class Mint private (name: String) { self =>
    trait Purser {
      def createPurse(balance: Long): self.Purse
    }

    def purse(): Purse = new Purse(0)

    private object capabilities {
      def purser = new Purser {
        def createPurse(balance: Long): self.Purse = new Purse(balance)
      }
    }

    final class Purse(private var balance: Long) {
      require(balance >= 0)

      def deposit[P <: Purse](purse: P, amount: Long): Unit = {
        require(amount <= purse.balance)
        require(amount + balance >= 0)
        purse.balance -= amount
        balance += amount
      }

      override def toString: String = {
        s"Purse($balance)"
      }
    }
  }

  object Mint {
    def apply(name: String): Mint = new Mint(name)

    class Access {
      def purser(mint: Mint): mint.Purser = mint.capabilities.purser
    }
  }

  def main(args: Array[String]): Unit = {
    val mint = Mint("Bank of Carol")
    val access = new Mint.Access()
    println(mint) // value: <Carol's mint>

    // This is a protected capability to mint money...
    val purser = access.purser(mint)
    val aliceMainPurse = purser.createPurse(1000)
    println("alice has: " + aliceMainPurse) // value: <has 1000 Carol bucks>

    val bobMainPurse: mint.Purse = mint.purse()
    println("bob has: " + bobMainPurse) // value: <has 0 Carol bucks>

    println("transfer 50 from alice to bob.")
    bobMainPurse.deposit(aliceMainPurse, 50)

    println("alice has: " + aliceMainPurse)
    println("bob has: " + bobMainPurse)
  }

}

