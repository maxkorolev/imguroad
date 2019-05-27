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

trait ImgurShooter[F[_]] {
  def run(): Stream[F, Unit]
}

object ImgurShooter {

  def impl[F[_]: ContextShift](C: Client[F], U: Uploader[F])(
      implicit F: Sync[F]
  ): ImgurShooter[F] = new ImgurShooter[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    import fs2.io.file.readAll
    import java.io.{File, InputStream}

    private val ChunkSize = 8192

    def run(): Stream[F, Unit] =
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
          Vector(Part(downloadResp.headers, downloadResp.body))
        )

        uploadReq <- Stream.eval(
          POST[Multipart[F]](
            multipart,
            uri"https://api.imgur.com/3/upload",
            Header(
              "Authorization",
              "Client-ID 5eeae49394cd929e299785c8805bd168fc675280"
            )
          )
        )

        uploadResp <- C.stream(uploadReq)

        // json <- Stream.eval(uploadResp.as[String])

// _ <- C.streaming(POST()) {

// }
      } yield (())

  }
}
