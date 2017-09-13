/*
 * Copyright © 2016 Lightbend, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
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
import com.lightbend.lagom.internal.client.CircuitBreakers
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
    circuitBreakers: CircuitBreakers,
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
