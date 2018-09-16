package graphql


import model.RestApi.FileNamesQL
import model._
import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object QueryDefinition {

  val filesFetcherCaching = Fetcher.caching(
    (ctx: Repo, ids: Seq[String]) ⇒
      Future.sequence(ids.map(id ⇒ ctx.getFilesFake(id))))(HasId(_.get.file_name))
  val FileNameType: ObjectType[Repo, FileName] =
    ObjectType(
      "FileNameType",
      "File Name",
      fields[Repo, FileName](
        Field("file_name", StringType, Some("File name"), resolve = _.value.file_name),
        Field("folder_name", StringType, Some("Folder name"), resolve = _.value.folder_name),
        Field("content_length", LongType, Some("Content Length"), resolve = _.value.content_length)
      ))

  val name = Argument("fileName", StringType, description = "File Name")


  val GetFileNameGraphQL: ObjectType[Repo, FileNameGraphQL] =
    ObjectType(
      "GetFileNameGraphQL",
      "File Name Graph QL",
      fields[Repo, FileNameGraphQL](
        Field("file_name", StringType, Some("File name"), resolve = _.value.file_name),
        Field("content_length", LongType, Some("Content length"), resolve = _.value.content_length),
        Field("destination_type", IntType, Some("Destination type"), resolve = _.value.destination_type)
      ))
  val GetFileNamesQL: ObjectType[Repo, FileNamesQL] =
    ObjectType(
      "GetFileNamesQL",
      "Get FileNames QL",
      fields[Repo, FileNamesQL](
        Field("file_names_ql", ListType(GetFileNameGraphQL), Some("File names collection"), resolve = _.value.fileNamesQL)
      ))
  val queryText = Argument("queryText", StringType, description = "Query Text")


  val queries = ObjectType(
    "Query",
    "Files and Metadata",
    fields[Repo, Unit](
      Field(
        name = "filename",
        description = Some("get file names"),
        fieldType = OptionType(FileNameType),
        arguments = name :: Nil,
        resolve = ctx ⇒ ctx.ctx.getFilesFake(ctx arg name)),
      Field(
        name = "getFileNamesQL",
        description = Some("find file names by text in content"),
        fieldType = OptionType(GetFileNamesQL),
        arguments = queryText :: Nil,
        resolve = ctx ⇒ ctx.ctx.getFileNamesQL(ctx arg queryText))
    ))

}



object MutationDefinition {

}



object SchemaDefinition {

  val fileIOSchema = Schema(query = QueryDefinition.queries, mutation = None)

}


/**

Request example

{
  getFileNamesQL(queryText: "публикаций") {
    file_names_ql{folder_name content_length destination_type}
  }
}



  */