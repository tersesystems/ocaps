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

// #membrane
import java.time.ZonedDateTime
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Locale, TimeZone}

import cats.Id
import ocaps._

// http://wiki.erights.org/wiki/Walnut/Secure_Distributed_Computing/Capability_Patterns#Membranes
// http://blog.ezyang.com/2013/03/what-is-a-membran/

object Membrane {

  import Location.{LocaleReader, TimeZoneReader}

  // #location
  class Location(locale: Locale, timeZone: TimeZone) {

    private object capabilities {
      val localeReader: Location.LocaleReader[Id] =
        new Location.LocaleReader[Id] {
          override val locale: Locale = Location.this.locale
        }

      val timeZoneReader: Location.TimeZoneReader[Id] =
        new Location.TimeZoneReader[Id] {
          override val timeZone: TimeZone = Location.this.timeZone
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

    class Access {
      def localeReader(location: Location): LocaleReader[Id] = {
        location.capabilities.localeReader
      }

      def timeZoneReader(location: Location): TimeZoneReader[Id] = {
        location.capabilities.timeZoneReader
      }
    }
  }

  def main(args: Array[String]): Unit = {

    class MembraneAccess(access: Location.Access, val membrane: PermeableMembrane) {
      type Wrapper[+A] = membrane.Wrapper[A]

      def localeReader(location: Location): LocaleReader[Wrapper] = {
        new LocaleReader[Wrapper] {
          override def locale: Wrapper[Locale] = {
            membrane.wrap(access.localeReader(location).locale)
          }
        }
      }

      def timeZoneReader(location: Location): TimeZoneReader[Wrapper] = {
        new TimeZoneReader[Wrapper] {
          override def timeZone: Wrapper[TimeZone] = {
            membrane.wrap(access.timeZoneReader(location).timeZone)
          }
        }
      }
    }

    // #membrane-setup
    val m = RevokerMembrane()
    val user = new Location(Locale.US, TimeZone.getTimeZone("PST"))
    val access = new MembraneAccess(new Location.Access(), m)

    val dryLocale: LocaleReader[access.Wrapper] = access.localeReader(user)
    val dryTimeZone: TimeZoneReader[access.Wrapper] =
      access.timeZoneReader(user)
    // #membrane-setup

    // #execution
    // Use an IO monad because the wrapper could throw revokerException when we call get

    // Uncommment this to see the operation fail...
    //m.revoke()

    val format: access.Wrapper[String] = for {
      timeZone <- dryTimeZone.timeZone
      locale <- dryLocale.locale
    } yield {
      ZonedDateTime
        .now(timeZone.toZoneId)
        .format(
          DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(locale)
        )
    }
    println(format.get)
  }

}

// #membrane