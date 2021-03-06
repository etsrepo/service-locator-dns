/*
 * Copyright 2016 Lightbend, Inc.
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

package com.lightbend.lagom.javadsl.dns

import java.net.URI
import java.util.Optional
import java.util.concurrent.CompletionStage
import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.dns.locator.{ Settings, ServiceLocator => ServiceLocatorService }
import com.lightbend.lagom.javadsl.client.CircuitBreakersPanel
import com.lightbend.lagom.javadsl.client.CircuitBreakingServiceLocator

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * DnsServiceLocator implements Lagom's ServiceLocator by using the DNS Service Locator service, which is an actor.
 */
class DnsServiceLocator @Inject() (
  @Named("ServiceLocatorService") serviceLocatorService: ActorRef,
  system: ActorSystem,
  circuitBreakers: CircuitBreakersPanel,
  implicit val ec: ExecutionContext) extends CircuitBreakingServiceLocator(circuitBreakers) {

  val settings = Settings(system)

  private def locateAsScala(name: String): Future[Option[URI]] =
    serviceLocatorService
      .ask(ServiceLocatorService.GetAddress(name))(settings.resolveTimeout1 + settings.resolveTimeout1 + settings.resolveTimeout2)
      .mapTo[ServiceLocatorService.Addresses]
      .map {
        case ServiceLocatorService.Addresses(addresses) =>
          addresses
            .headOption
            .map(sa => new URI(sa.protocol, null, sa.host, sa.port, null, null, null))
      }

  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): CompletionStage[Optional[URI]] =
    locateAsScala(name).map(_.asJava).toJava
}
