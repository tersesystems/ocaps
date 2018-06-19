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

import java.util.UUID

import cats.implicits._
import cats.{Id, _}

import scala.util._

/**
  * Demonstrates exposing capabilities as facets of a repository, so individual elements are exposed.
  *
  * The capabilities use tagless final to show how you can use different effects with capabilities.
  *
  * For example, the Id effect is an identity, so a failure will cause an exception.
  *
  * The Try effect is a disjoint union with Exception, so a failure will return Failure(Exception) as a result.
  */
object RepositoryComposition {
  val ID = UUID.fromString("c31d34e2-5892-4a2d-9fd5-3ce2e0efedf7")

  import ItemRepository._

  val access = new ItemRepository.Access()

  def main(args: Array[String]): Unit = {
    val itemRepository = new ItemRepository()

    changeWithId(itemRepository)
    changeWithTry(itemRepository)
  }

  def changeWithId(itemRepository: ItemRepository): Unit = {
    val idNameChanger = new NameChanger[Id](
      access.finder(itemRepository),
      access.updater(itemRepository),
      _.map(identity)
    )
    val idResult = idNameChanger.changeName(ID, "new name")
    println(s"id result = $idResult")
  }

  def changeWithTry(itemRepository: ItemRepository): Unit = {
    val idFinder = access.finder(itemRepository)
    val tryFinder = new Finder[Try] {
      override def find(id: UUID): Try[Option[Item]] = Try(idFinder.find(id))
    }

    val idUpdater = access.updater(itemRepository)
    val tryUpdater = new Updater[Try] {
      override def update(item: Item): Try[UpdateResult] =
        Try(idUpdater.update(item))
    }

    val tryNameChanger = new NameChanger[Try](tryFinder, tryUpdater, {
      case Success(Some(result)) => result.map(Some(_))
      case Success(None)         => Success(None)
      case Failure(ex)           => Failure(ex)
    })
    val tryResult = tryNameChanger.changeName(ID, "new name")
    println(s"try result = $tryResult")
  }

  class NameChanger[G[_]: Functor](
    finder: Finder[G],
    updater: Updater[G],
    transform: G[Option[G[UpdateResult]]] => G[Option[UpdateResult]]
  ) {
    def changeName(id: UUID, newName: String): G[Option[UpdateResult]] = {
      val saved: G[Option[G[UpdateResult]]] = finder.find(id).map {
        maybeItem: Option[Item] =>
          maybeItem.map { item =>
            updater.update(item.copy(name = newName))
          }
      }
      transform(saved)
    }
  }

  // #repository
  case class Item(id: UUID, name: String)

  class ItemRepository {
    import ItemRepository._

    private var items = Seq(Item(ID, "item name"))

    private def find(id: UUID): Option[Item] = items.find(_.id == id)

    private def update(u: Item): UpdateResult = UpdateResult(s"item $u updated")

    private object capabilities {
      // "Id" type comes from cats, and reItem(id, ItemName("user@example.com"))turns the result itself
      val finder: Finder[Id] = new Finder[Id]() {
        override def find(id: UUID): Id[Option[Item]] =
          ItemRepository.this.find(id)
      }
      val updater: Updater[Id] = new Updater[Id]() {
        override def update(item: Item): Id[UpdateResult] =
          ItemRepository.this.update(item)
      }
    }
  }

  object ItemRepository {
    trait Finder[F[_]] {
      def find(id: UUID): F[Option[Item]]
    }

    trait Updater[F[_]] {
      def update(item: Item): F[UpdateResult]
    }

    case class UpdateResult(message: String)

    class Access {
      def finder(repo: ItemRepository): Finder[Id] = repo.capabilities.finder
      def updater(repo: ItemRepository): Updater[Id] = repo.capabilities.updater
    }

    class TryAccess(access: Access) {
      def finder(repo: ItemRepository): Finder[Try] = (id: UUID) => Try(access.finder(repo).find(id))
      def updater(repo: ItemRepository): Updater[Try] = (item: Item) => Try(access.updater(repo).update(item))
    }
  }
  // #repository

}
