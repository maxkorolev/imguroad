package imguroad

import fs2._
import fs2.concurrent._
import cats.effect.Sync
import cats.syntax.all._
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.multipart._
import org.http4s.headers._
import org.http4s.Method._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.util.CaseInsensitiveString
import scala.concurrent.ExecutionContext
import cats.effect.ContextShift
import org.log4s.getLogger

trait ImgurShooter[F[_]] {
  def run(): Stream[F, Unit]
}

object ImgurShooter {

  private[this] val logger = getLogger

  def impl[F[_]: ContextShift](
      C: Client[F],
      U: Uploader[F],
      config: ImgurConfig
  )(
      implicit F: Sync[F]
  ): ImgurShooter[F] = new ImgurShooter[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    def run(): Stream[F, Unit] = {

      for {
        job <- U.listen
        url <- Stream.emits(job.urls.toSeq)

        // TODO get rid of match - we should just lift here
        urlParsed <- Stream.eval(Uri.fromString(url) match {
          case Left(err) => F.raiseError(err)
          case Right(v)  => F.point(v)
        })

        downloadReq <- Stream.eval(GET(urlParsed))
        downloadResp <- C.stream(downloadReq)
        multipart = Multipart[F](
          Vector(Part.fileData("image", "image", downloadResp.body))
        )

        uploadReq <- Stream.eval(
          POST
            .apply(
              multipart,
              uri"https://api.imgur.com/3/upload"
            )
            .map(
              _.withHeaders(
                Headers.of(
                  Authorization(
                    Credentials.Token(AuthScheme.Bearer, config.bearer)
                  )
                ) ++ multipart.headers
              )
            )
        )

        uploadResp <- C.stream(uploadReq)

        json <- Stream.eval(uploadResp.as[String])
      } yield (())

    }.handleErrorWith { err =>
      Stream.emit(logger.error(err.getMessage()))
    }
  }
}
