package common

import java.text.SimpleDateFormat
import java.time.ZonedDateTime

import org.joda.time.{DateTime, DateTimeZone}

object DateTimeUtils {
  def toPk(yyyy: Int, mm: Int, day: Int, hour: Int) =
    s"$yyyy-$mm-$day-$hour"


  implicit class ExtentionDateTime(val dt: DateTime) {
    def toZonedDateTime = dt.toGregorianCalendar.toZonedDateTime
  }

  implicit class ExtentionLong(val utc: Long) {
    def toZonedDateTime = new DateTime(utc, DateTimeZone.UTC).toZonedDateTime
  }

  def toDateTime(someString: String)(implicit sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")) =
    if (someString == "null") None
    else {
      try {
        val date = sdf.parse(someString.replace('T', ' '))
        Some(new DateTime(date))
      } catch {
        case e: Exception => None
      }
    }

  implicit class ExtentionZonedDateTime(val zdt: ZonedDateTime) {
    def toDateTime = {
      val zone = DateTimeZone.forID(zdt.getZone.getId)
      new DateTime(zdt.toInstant.toEpochMilli, zone)
    }

    def getMillis = {
      zdt.toDateTime.getMillis
    }
  }

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

}
