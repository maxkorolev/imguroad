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
import _root_.io.circe.Json

trait ImgurShooter[F[_]] {
  def run(): Stream[F, Unit]
}

object ImgurShooter {

  def impl[F[_]: ContextShift](C: Client[F], U: Uploader[F], config: ImgurConfig)(
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
        _ = println(downloadResp.headers.toList.mkString("\r\n"))

        multipart = Multipart[F](
          Vector(
            Part.fileData(
              "img",
              new File(
                "/home/maxkorolev/Downloads/11234651086_681b3c2c00_b.jpg"
              ),
              ExecutionContext.global,
              `Content-Type`(MediaType.image.jpeg),
            )
          )

          // Vector(
          //   Part.fileData(
          //     "img",
          //     "img",
          //     downloadResp.body,
          //     `Content-Type`(MediaType.image.png)
          //   )
          // )
          // Vector(Part(downloadResp.headers, downloadResp.body))
          // Vector()
        )

        uploadReq <- Stream.eval(
          POST[Multipart[F]](
            multipart,
            uri"https://api.imgur.com/3/upload",
              Header(
                "Authorization",
                s"Bearer ${config.bearer}"
              )
          ).map(_.withHeaders(multipart.headers))
        )

        uploadResp <- C.stream(uploadReq)

        json <- Stream.eval(uploadResp.as[String])

        _ = println(uploadResp.toString)
        _ = println(json.toString())

// _ <- C.streaming(POST()) {

// }
      } yield (())

  }
}
