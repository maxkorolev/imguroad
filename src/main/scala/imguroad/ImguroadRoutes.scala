package imguroad

import io.circe._
import cats.effect.Sync
import cats.syntax.all._
import cats.Applicative
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import shapeless._

object ImguroadRoutes {

  implicit def entityDecoder[F[_]: Sync, T: Decoder](
      implicit notEq: T =:!= Json
  ): EntityDecoder[F, T] =
    jsonOf[F, T]

  implicit def entityEncoder[F[_]: Applicative, T: Encoder](
      implicit notEq: T =:!= Json
  ): EntityEncoder[F, T] =
    jsonEncoderOf[F, T]

  def uploadRoutes[F[_]: Sync](U: Uploader[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "v1" / "images" / "upload" =>
        for {
          dto <- req.as[Uploader.UploadURLsDTO]
          jobID <- U.upload(dto.urls.toSet)
          resp <- Ok(Uploader.UploadedJobIDDTO(jobID))
        } yield resp

      case req @ GET -> Root / "callback" =>
        for {
          dto <- req.as[String]
          resp <- Ok(dto)
        } yield resp
    }
  }

}
