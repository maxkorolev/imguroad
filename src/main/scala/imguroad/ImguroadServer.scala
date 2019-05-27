package imguroad

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import fs2.concurrent._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global

object ImguroadServer {

  def stream[F[_]: ConcurrentEffect](
      implicit T: Timer[F],
      C: ContextShift[F]
  ): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream
      topic <- Stream.eval(Topic[F, Uploader.Issue](Uploader.Skip))
      uploader = Uploader.impl[F](topic)

      httpApp = (
        ImguroadRoutes.uploadRoutes[F](uploader)
      ).orNotFound

      shooter = ImgurShooter.impl(client, uploader).run()

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve concurrently shooter
    } yield exitCode
  }.drain
}
