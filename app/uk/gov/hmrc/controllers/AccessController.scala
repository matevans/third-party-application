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

package uk.gov.hmrc.controllers

import java.util.UUID
import javax.inject.Inject

import play.api.libs.json.Json.toJson
import play.api.mvc.BodyParsers.parse.json
import uk.gov.hmrc.connector.AuthConnector
import uk.gov.hmrc.models.JsonFormatters._
import uk.gov.hmrc.services.{AccessService, ApplicationService}

import scala.concurrent.ExecutionContext.Implicits.global

class AccessController @Inject()(accessService: AccessService,
                                 val authConnector: AuthConnector,
                                 val applicationService: ApplicationService) extends CommonController with AuthorisationWrapper {

  def readScopes(applicationId: UUID) = requiresGatekeeperForPrivilegedOrRopcApplications(applicationId).async { implicit request =>
    accessService.readScopes(applicationId) map { scopeResponse =>
      Ok(toJson(scopeResponse))
    } recover recovery
  }

  def updateScopes(applicationId: UUID) = requiresGatekeeperForPrivilegedOrRopcApplications(applicationId).async(json) { implicit request =>
    withJsonBody[ScopeRequest] { scopeRequest =>
      accessService.updateScopes(applicationId, scopeRequest) map { scopeResponse =>
        Ok(toJson(scopeResponse))
      } recover recovery
    }
  }

  def readOverrides(applicationId: UUID) = requiresGatekeeperForStandardApplications(applicationId).async { implicit request =>
    accessService.readOverrides(applicationId) map { overrideResponse =>
      Ok(toJson(overrideResponse))
    } recover recovery
  }

  def updateOverrides(applicationId: UUID) = requiresGatekeeperForStandardApplications(applicationId).async(json) { implicit request =>
    withJsonBody[OverridesRequest] { overridesRequest =>
      accessService.updateOverrides(applicationId, overridesRequest) map { overridesResponse =>
        Ok(toJson(overridesResponse))
      } recover recovery
    }
  }
}
