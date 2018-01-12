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

import java.time.ZonedDateTime
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Locale, TimeZone}

import cats._
import cats.effect.IO
import ocaps._
import ocaps.example.Membrane.Location.{LocaleReader, TimeZoneReader}

import scala.language.reflectiveCalls

// http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns#Membranes
// http://blog.ezyang.com/2013/03/what-is-a-membran/

object Membrane {

  // #location
  class Location(locale: Locale, timeZone: TimeZone) {
    private object capabilities {
      def localeReader: Location.LocaleReader[Id] = new Location.LocaleReader[Id] {
        override def locale: Locale = Location.this.locale
      }

      def timeZoneReader: Location.TimeZoneReader[Id] = new Location.TimeZoneReader[Id] {
        override def timeZone: TimeZone = Location.this.timeZone
      }
    }
  }

  object Location {
    trait LocaleReader[F[_]] {
      def locale: F[Locale]
    }

    trait TimeZoneReader[F[_]] {
      def timeZone: F[TimeZone]
    }

    class IdAccess {
      def localeReader(location: Location): LocaleReader[Id] = {
        location.capabilities.localeReader
      }

      def timeZoneReader(location: Location): TimeZoneReader[Id] = {
        location.capabilities.timeZoneReader
      }
    }

    class MembraneAccess(val membrane: PermeableMembrane) {
      type Wrapper[+A] = membrane.Wrapper[A]

      def localeReader(location: Location): LocaleReader[Wrapper] = {
        new LocaleReader[Wrapper] {
          override def locale: Wrapper[Locale] = {
            membrane.wrap(location.capabilities.localeReader.locale)
          }
        }
      }

      def timeZoneReader(location: Location): TimeZoneReader[Wrapper] = {
        new TimeZoneReader[Wrapper] {
          override def timeZone: Wrapper[TimeZone] = {
            membrane.wrap(location.capabilities.timeZoneReader.timeZone)
          }
        }
      }
    }
  }
  // #user

  def main(args: Array[String]): Unit = {
    // #membrane-setup
    val m = PermeableMembrane()
    val user = new Location(Locale.US, TimeZone.getTimeZone("PST"))
    val access = new Location.MembraneAccess(m)

    val dryLocale: LocaleReader[access.Wrapper] = access.localeReader(user)
    val dryTimeZone: TimeZoneReader[access.Wrapper] = access.timeZoneReader(user)
    // #membrane-setup

    // #execution
    // Use an IO monad because the wrapper could throw revokerException when we call get

    // Uncommment this to see the operation fail...
    //m.revoke()

    val program: IO[String] = IO {
      val format: access.Wrapper[String] = for {
        timeZone <- dryTimeZone.timeZone
        locale <- dryLocale.locale
      } yield {
        ZonedDateTime.now(timeZone.toZoneId)
          .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(locale))
      }
      format.get
    }
    program.unsafeRunAsync {
      case Left(ex) =>
        ex.printStackTrace()
      case Right(success) =>
        println(s"Using time ${success}")
    }
    // #execution
  }

}