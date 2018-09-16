package graphql


class FileIOError (private val message: String = "",
                   private val cause: Throwable = None.orNull)
  extends Exception(message, cause)
