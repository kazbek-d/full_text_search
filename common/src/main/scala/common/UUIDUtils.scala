package common

import java.util.UUID

import spray.json.{DeserializationException, JsString, JsValue, JsonFormat}

object UUIDUtils {

  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _ => throw DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  def toUUID(someString: String): Option[UUID] = if (someString == "null") None
  else {
    try {
      Some(UUID.fromString(someString))
    } catch {
      case _: Exception => None
    }
  }

}
