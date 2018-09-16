package common

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}


object ZonedDateTimeProtocol   {

  implicit object ZonedDateTimeJsonFormat extends RootJsonFormat[ZonedDateTime] with DefaultJsonProtocol {

    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault)

    def write(obj: ZonedDateTime): JsValue = {
      JsString(formatter.format(obj))
    }

    def read(json: JsValue): ZonedDateTime = json match {
      case JsString(s) => try {
        ZonedDateTime.parse(s, formatter)
      } catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): ZonedDateTime = {
      ZonedDateTime.now()
    }
  }

}

