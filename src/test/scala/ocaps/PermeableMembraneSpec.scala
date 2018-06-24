package ocaps

import java.time.ZonedDateTime
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.{Locale, TimeZone}

import cats.Id
import org.scalatest._

class PermeableMembraneSpec extends WordSpec with Matchers  {

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

  "RevokerMembrane" should {

    "work" in {
      val m = RevokerMembrane()
      val location = new Location(Locale.US, TimeZone.getTimeZone("PST"))
      val access = new Location.MembraneAccess(m)

      val dryLocale: Location.LocaleReader[access.Wrapper] = access.localeReader(location)
      val dryTimeZone:  Location.TimeZoneReader[access.Wrapper] = access.timeZoneReader(location)

      val formatWrapper: access.Wrapper[String] = for {
        timeZone <- dryTimeZone.timeZone
        locale <- dryLocale.locale
      } yield {
        ZonedDateTime.now(timeZone.toZoneId)
          .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(locale))
      }
      formatWrapper.get should not be(null)
    }

    "not work" in {
      val m = RevokerMembrane()
      val location = new Location(Locale.US, TimeZone.getTimeZone("PST"))
      val access = new Location.MembraneAccess(m)

      val dryLocale: Location.LocaleReader[access.Wrapper] = access.localeReader(location)
      val dryTimeZone:  Location.TimeZoneReader[access.Wrapper] = access.timeZoneReader(location)

      val formatWrapper: access.Wrapper[String] = for {
        timeZone <- dryTimeZone.timeZone
        locale <- dryLocale.locale
      } yield {
        ZonedDateTime.now(timeZone.toZoneId)
          .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(locale))
      }

      m.revoke()

      assertThrows[RevokedException] {
        formatWrapper.get
      }
    }
  }

  "Ad-hoc Membrane" should {

    "work" in {
      val m = PermeableMembrane(new Thunker {
        override def thunk[C](capability: => C): Thunk[C] = {
          () => {
            val cap = capability
            println(s"putting on mah thunking $cap") // could log etc here.
            cap
          }
        }
      })
      val location = new Location(Locale.US, TimeZone.getTimeZone("PST"))
      val access = new Location.MembraneAccess(m)

      val dryLocale: Location.LocaleReader[access.Wrapper] = access.localeReader(location)
      val dryTimeZone:  Location.TimeZoneReader[access.Wrapper] = access.timeZoneReader(location)

      val formatWrapper: access.Wrapper[String] = for {
        timeZone <- dryTimeZone.timeZone
        locale <- dryLocale.locale
      } yield {
        ZonedDateTime.now(timeZone.toZoneId)
          .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(locale))
      }
      formatWrapper.get should not be null
    }
  }

}
