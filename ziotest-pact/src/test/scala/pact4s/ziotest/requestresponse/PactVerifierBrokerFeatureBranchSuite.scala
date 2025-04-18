/*
 * Copyright 2021 io.github.jbwheatley
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

package pact4s.ziotest.requestresponse

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import pact4s.MockProviderServer
import pact4s.provider.{Branch, ConsumerVersionSelectors, ProviderInfoBuilder}
import pact4s.ziotest.PactVerifier
import zio.{Scope, ZIO, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

import scala.concurrent.duration.DurationInt

object PactVerifierBrokerFeatureBranchSuite extends ZIOSpecDefault with PactVerifier {
  val mock = new MockProviderServer(49200, hasFeatureX = true)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo(consumerVersionSelector = ConsumerVersionSelectors.branch("feat/x"))

  val mockLayer: ZLayer[Any with Scope, Throwable, IO[Unit]] = ZLayer.fromZIO {
    ZIO.acquireRelease(
      for {
        serverStart <- ZIO.attempt(mock.server.allocated.unsafeRunSync())
      } yield serverStart._2
    )(shutdown =>
      ZIO.attempt(shutdown.unsafeRunSync()).catchAll(e => ZIO.logError(s"failed to shutdown mock server: $e"))
    )
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    test("Verify pacts for provider `Pact4sProvider` with a feature branch, zio-test")(
      for {
        _ <- verifyPacts(Some(Branch("feat/x")))
        featureXState = mock.featureXState.get.unsafeRunTimed(10.seconds)
      } yield assertTrue(featureXState.contains(true))
    ).provideLayerShared(mockLayer)
}
