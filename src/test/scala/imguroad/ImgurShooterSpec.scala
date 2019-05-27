package imguroad

import fs2._
import fs2.concurrent._
import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import scala.concurrent.ExecutionContext
import imguroad.Uploader.Job
import imguroad.Uploader.Skip
import java.{util => ju}
import org.http4s.client._
import org.http4s.dsl.Http4sDsl
import cats.data.Kleisli

class ImgurShooterSpec extends org.specs2.mutable.Specification {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val dsl = new Http4sDsl[IO] {}
  import dsl._

  val config = ImgurConfig("test")

  "ImgurShooter" >> {
    "send to imgur requests" >> {

      val urls = Set(
        "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg",
        "http://clipart-library.com/images/6cp6Rbyri.jpg"
      )

      val uploader = new Uploader[IO] {
        def upload(urls: Set[Uploader.URL]): IO[Uploader.JobID] = IO.never
        def listen: Stream[IO, Uploader.Job] =
          Stream.emit(Uploader.Job(ju.UUID.randomUUID(), urls))
      }

      val client = Client.fromHttpApp[IO](Kleisli {
        case req @ GET -> Root / "2879" / "11234651086_681b3c2c00_b_d.jpg" =>
          Ok("first image")
        case req @ GET -> Root / "images" / "6cp6Rbyri.jpg" =>
          Ok("second image")

        case req => Ok("ololol")
      })

      ImgurShooter
        .impl(client, uploader, config)
        .run()
        .compile
        .drain
        .unsafeRunSync() must beEqualTo(())

    }
  }

}
