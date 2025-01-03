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

package pact4s

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.RequestResponsePact
import pact4s.effect.Id
import pact4s.syntax.RequestResponsePactOps

/** Allows pacts to be split up into one interaction per test, instead of having one big Pact at the top of the test
  * suite. The file writer combines all the pacts between the same consumer and provider into a single file.
  */
trait InlineRequestResponsePactResources extends RequestResponsePactOps { self =>
  def pactTestExecutionContext: PactTestExecutionContext = new PactTestExecutionContext()
  protected val mockProviderConfig: MockProviderConfig   = MockProviderConfig.createDefault()

  private[pact4s] type Forger <: InlineRequestResponsePactForger

  def withPact(aPact: RequestResponsePact): Forger

  private[pact4s] type Effect[_]

  /** This effect runs after each consumer pact test are run, but before they get written to a file.
    */
  def beforeWritePacts(): Effect[Unit]

  private[pact4s] trait InlineRequestResponsePactForger extends RequestResponsePactForgerResources {
    override private[pact4s] type Effect[A] = Id[A]
    override def beforeWritePacts(): Effect[Unit]       = ()
    override val mockProviderConfig: MockProviderConfig = self.mockProviderConfig
  }
}
