/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.thirdpartyapplication.mocks.connectors

import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.verification.VerificationMode
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.thirdpartyapplication.connector.EmailConnector

import scala.concurrent.Future
import scala.concurrent.Future.successful

trait EmailConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  object EmailConnectorMock {
    val aMock: EmailConnector = mock[EmailConnector]

    def verify: EmailConnector = MockitoSugar.verify(aMock)

    def verify(mode: VerificationMode): EmailConnector = MockitoSugar.verify(aMock, mode)

    def verifyZeroInteractions(): Unit = MockitoSugar.verifyZeroInteractions(aMock)

    object SendAddedClientSecretNotification {
      def thenReturnOk(): ScalaOngoingStubbing[Future[HttpResponse]] = {
        when(aMock.sendAddedClientSecretNotification(*, *, *, *)(*)).thenReturn(successful(HttpResponse(OK)))
      }

      def verifyCalled(): Future[HttpResponse] = {
        verify.sendAddedClientSecretNotification(*, *, *, *)(*)
      }

      def verifyCalledWith(actorEmailAddress: String,
                           clientSecret: String,
                           applicationName: String,
                           recipients: Set[String]): Future[HttpResponse] = {
        verify.sendAddedClientSecretNotification(eqTo(actorEmailAddress), eqTo(clientSecret), eqTo(applicationName), eqTo(recipients))(*)
      }
    }

    object SendRemovedClientSecretNotification {
      def thenReturnOk(): ScalaOngoingStubbing[Future[HttpResponse]] = {
        when(aMock.sendRemovedClientSecretNotification(*, *, *, *)(*)).thenReturn(successful(HttpResponse(OK)))
      }

      def verifyCalled(): Future[HttpResponse] = {
        verify.sendRemovedClientSecretNotification(*, *, *, *)(*)
      }

      def verifyCalledWith(actorEmailAddress: String,
                           clientSecret: String,
                           applicationName: String,
                           recipients: Set[String]): Future[HttpResponse] = {
        verify.sendRemovedClientSecretNotification(eqTo(actorEmailAddress), eqTo(clientSecret), eqTo(applicationName), eqTo(recipients))(*)
      }

      def verifyNeverCalled() = EmailConnectorMock.verify(never).sendRemovedClientSecretNotification(*, *, *, *)(*)
    }
  }
}
