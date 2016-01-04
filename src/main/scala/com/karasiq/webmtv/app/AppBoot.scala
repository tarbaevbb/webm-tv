package com.karasiq.webmtv.app

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object AppBoot extends App {
  def startup(): Unit = {
    implicit val timeout = Timeout(20 seconds)

    implicit val actorSystem = ActorSystem("webm-tv")

    val store = actorSystem.actorOf(Props(classOf[WebmStoreDispatcher], WebmHeapStore), "storeDispatcher")

    val service = actorSystem.actorOf(Props(classOf[AppHandler], store), "webService")

    // Schedule rescan
    val config = ConfigFactory.load().getConfig("webm-tv")

    IO(Http) ? Http.Bind(service, interface = config.getString("host"), port = config.getInt("port"))

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        Await.result(actorSystem.terminate(), FiniteDuration(5, TimeUnit.MINUTES))
      }
    }))
  }

  startup()
}
