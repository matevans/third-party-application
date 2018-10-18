/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.repository

import java.util.UUID
import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.libs.json.Json._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.Command
import reactivemongo.bson.{BSONArray, BSONBoolean, BSONDocument, BSONObjectID}
import reactivemongo.core.commands.{Match, PipelineOperator, Project}
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.JSONSerializationPack
import uk.gov.hmrc.models.MongoFormat._
import uk.gov.hmrc.models._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.util.mongo.IndexHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApplicationRepository @Inject()(mongo: ReactiveMongoComponent)
  extends ReactiveRepository[ApplicationData, BSONObjectID]("application", mongo.mongoConnector.db,
    MongoFormat.formatApplicationData, ReactiveMongoFormats.objectIdFormats) {

  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  private val applicationProjection = Project(
    "id" -> BSONBoolean(true),
    "name" -> BSONBoolean(true),
    "normalisedName" -> BSONBoolean(true),
    "collaborators" -> BSONBoolean(true),
    "description" -> BSONBoolean(true),
    "wso2Username" -> BSONBoolean(true),
    "wso2Password" -> BSONBoolean(true),
    "wso2ApplicationName" -> BSONBoolean(true),
    "tokens" -> BSONBoolean(true),
    "state" -> BSONBoolean(true),
    "access" -> BSONBoolean(true),
    "createdOn" -> BSONBoolean(true),
    "rateLimitTier" -> BSONBoolean(true),
    "environment" -> BSONBoolean(true))

  override def indexes = Seq(
    createSingleFieldAscendingIndex(
      indexFieldKey = "state.verificationCode",
      indexName = Some("verificationCodeIndex")
    ),
    createAscendingIndex(
      indexName = Some("stateName_stateUpdatedOn_Index"),
      isUnique = false,
      isBackground = true,
      indexFieldsKey = List("state.name", "state.updatedOn"): _*
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "id",
      indexName = Some("applicationIdIndex"),
      isUnique = true
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "normalisedName",
      indexName = Some("applicationNormalisedNameIndex")
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "tokens.production.clientId",
      indexName = Some("productionTokenClientIdIndex"),
      isUnique = true
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "tokens.sandbox.clientId",
      indexName = Some("sandboxTokenClientIdIndex"),
      isUnique = true
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "access.overrides",
      indexName = Some("accessOverridesIndex")
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "access.accessType",
      indexName = Some("accessTypeIndex")
    ),
    createSingleFieldAscendingIndex(
      indexFieldKey = "collaborators.emailAddress",
      indexName = Some("collaboratorsEmailAddressIndex")
    )
  )

  def save(application: ApplicationData): Future[ApplicationData] = {
    collection.find(Json.obj("id" -> application.id.toString)).one[BSONDocument].flatMap {
      case Some(document) => collection.update(selector = BSONDocument("_id" -> document.get("_id")), update = application)
      case None => collection.insert(application)
    }.map(_ => application)
  }

  def fetchStandardNonTestingApps(): Future[Seq[ApplicationData]] = {
    collection.find(Json.obj("$and" -> Json.arr(
      Json.obj("state.name" -> Json.obj("$ne" -> State.TESTING)),
      Json.obj("access.accessType" -> Json.obj("$eq" -> AccessType.STANDARD))
    ))).cursor[ApplicationData]().collect[Seq]()
  }

  def fetch(id: UUID): Future[Option[ApplicationData]] = {
    collection.find(Json.obj("id" -> id)).one[ApplicationData]
  }

  def fetchNonTestingApplicationByName(name: String): Future[Option[ApplicationData]] = {
    collection.find(Json.obj("$and" -> Json.arr(
      Json.obj("normalisedName" -> name.toLowerCase),
      Json.obj("state.name" -> Json.obj("$ne" -> State.TESTING))))).one[ApplicationData]
  }


  def fetchVerifiableUpliftBy(verificationCode: String): Future[Option[ApplicationData]] = {
    collection.find(Json.obj("state.verificationCode" -> verificationCode)).one[ApplicationData]
  }


  def fetchAllByStatusDetails(state: State.State, updatedBefore: DateTime): Future[Seq[ApplicationData]] = {
    find("state.name" -> state, "state.updatedOn" -> Json.obj("$lte" -> updatedBefore))
  }


  def fetchByClientId(clientId: String): Future[Option[ApplicationData]] = {
    collection
      .find(
        Json.obj("$or" -> Json.arr(
          Json.obj("tokens.production.clientId" -> clientId),
          Json.obj("tokens.sandbox.clientId" -> clientId))))
      .one[ApplicationData]
  }

  def fetchByServerToken(serverToken: String): Future[Option[ApplicationData]] = {
    collection
      .find(
        Json.obj("$or" -> Json.arr(
          Json.obj("tokens.production.accessToken" -> serverToken),
          Json.obj("tokens.sandbox.accessToken" -> serverToken))))
      .one[ApplicationData]
  }

  def fetchAllForEmailAddress(emailAddress: String): Future[Seq[ApplicationData]] = {
    find("collaborators.emailAddress" -> emailAddress)
  }

  def fetchAllForEmailAddressAndEnvironment(emailAddress: String, environment: String): Future[Seq[ApplicationData]] = {
    find("collaborators.emailAddress" -> emailAddress, "environment" -> environment)
  }

  private def processResults[T](json: JsObject)(implicit fjs: Reads[T]): Future[T] = {
    (json \ "result").validate[T] match {
      case JsSuccess(result, _) => Future.successful(result)
      case JsError(errors) => Future.failed(new RuntimeException((json \ "errmsg").asOpt[String].getOrElse(errors.mkString(","))))
    }
  }

  private def lookupByAPI(operators: PipelineOperator*): Future[Seq[ApplicationData]] = {
    val lookup: BSONDocument = BSONDocument(
      "$lookup" -> BSONDocument(
        "from" -> "subscription",
        "localField" -> "id",
        "foreignField" -> "applications",
        "as" -> "subscribedApis"))

    val commandDoc: BSONDocument = BSONDocument(
      "aggregate" -> "application",
      "pipeline" -> BSONArray(lookup +: operators.map(_.makePipe)))

    val runner = Command.run(JSONSerializationPack)
    runner.apply(collection.db, runner.rawCommand(commandDoc))
      .one[JsObject]
      .flatMap(processResults[Seq[ApplicationData]])
  }

  def fetchAllForContext(apiContext: String): Future[Seq[ApplicationData]] =
    lookupByAPI(
      Match(BSONDocument("subscribedApis.apiIdentifier.context" -> apiContext)),
      applicationProjection)

  def fetchAllForApiIdentifier(apiIdentifier: APIIdentifier): Future[Seq[ApplicationData]] =
    lookupByAPI(
      Match(BSONDocument("subscribedApis.apiIdentifier" -> BSONDocument("context" -> apiIdentifier.context, "version" -> apiIdentifier.version))),
      applicationProjection)

  def fetchAllWithNoSubscriptions(): Future[Seq[ApplicationData]] =
    lookupByAPI(
      Match(BSONDocument("subscribedApis" -> BSONDocument("$size" -> 0))),
      applicationProjection)

  def fetchAll(): Future[Seq[ApplicationData]] = {
    collection.find(Json.obj()).cursor[ApplicationData]().collect[Seq]()
  }

  def delete(id: UUID): Future[HasSucceeded] = {
    collection.remove(Json.obj("id" -> id)).map(_ => HasSucceeded)
  }
}

sealed trait ApplicationModificationResult
final case class SuccessfulApplicationModificationResult(numberOfDocumentsUpdated: Int) extends ApplicationModificationResult
final case class UnsuccessfulApplicationModificationResult(message: Option[String]) extends ApplicationModificationResult