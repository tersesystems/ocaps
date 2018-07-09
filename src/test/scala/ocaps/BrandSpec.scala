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

package ocaps

import org.scalatest._

class BrandSpec extends WordSpec with Matchers {

  "Brand" should {

    "create a new brand with hint" in {
      val brand = Brand.create("some brand hint")
      brand.hint should equal(Hint("some brand hint"))
    }

    "Provide tuple" in {
      val brand = Brand.create("some brand")
      brand.tuple shouldBe a[Tuple2[_, _]]
      brand.tuple._1 shouldBe a[Brand.Sealer]
      brand.tuple._2 shouldBe a[Brand.Unsealer]
    }

    "apply and unapply correctly" in {
      val brand = Brand.create("string brand")
      val boxedDerp: Brand.Box[String] = brand.sealer("derp")

      val unboxed = boxedDerp match {
        case brand(derp) =>
          derp
        case _ =>
          ""
      }

      unboxed should equal("derp")
    }

    "not unseal with a different brand" in {
      val brand1 = Brand.create("brand1")
      val brand2 = Brand.create("brand2")
      val boxedWith1: Brand.Box[String] = brand1("derp")

      val unboxed = boxedWith1 match {
        case brand2(derp) =>
          derp
        case _ =>
          ""
      }

      unboxed should equal("")
    }

    "seal and unseal with functions" in {
      trait Request[A] {
        def body: A
      }
      trait Response {
        def body: String
      }

      type Action[A] = Request[A] => Response

      val actionBrand = Brand.create("complex function")

      val sealedAction = actionBrand((v1: Request[String]) => new Response { val body: String = v1.body })
      val unboxed = sealedAction match {
        case actionBrand(action) =>
          action
      }

      val response: Response = unboxed(new Request[String] { val body = "hello" })
      response.body should equal("hello")
    }

    "work with multiple brands" in {
      val fooFactory1 = new FooFactory("factory1")
      val foo1 = fooFactory1.create("foo1")

      val fooFactory2 = new FooFactory("factory2")
      val foo2 = fooFactory2.create("foo2")

      fooFactory1.validate(foo1) should be(true)
      fooFactory2.validate(foo1) should be(false)
      fooFactory1.validate(foo2) should be(false)
      fooFactory2.validate(foo2) should be(true)
    }

    "work with ints" in {
      val brand = Brand.create("int brand")
      val boxedInt = brand.sealer(5)
      brand.unsealer(boxedInt) should equal(Some(5))
    }

  }

  class FooFactory(name: String) {
    private val selfBrand = Brand.create(s"brand for $name")

    def create(name: String): Foo = {
      val box = selfBrand.sealer(this)
      //println(s"name = $name, box = $box")
      Foo(name, box)
    }

    def validate(foo: Foo): Boolean = {
      val maybeFactory = selfBrand.unsealer(foo.source)
      //println(s"foo = $foo, maybeFactory = $maybeFactory, source = ${foo.source}")
      maybeFactory.contains(this)
    }

    override def toString: String = {
      name
    }
  }

  case class Foo(name: String, source: Brand.Box[FooFactory])

}
