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
import org.http4s.client.blaze.BlazeClientBuilder

class ImgurShooterSpec extends org.specs2.mutable.Specification {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  "ImgurShooter" >> {
    "make its job - create jobs" >> {

      val uploader = new Uploader[IO] {
        def upload(urls: Set[Uploader.URL]): IO[Uploader.JobID] = IO.never
        def listen: Stream[IO, Uploader.Job] =
          Stream.emit(
            Uploader.Job(
              ju.UUID.randomUUID(),
              Set(
                "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg",
                // "https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"
              )
            )
          )

      }

      val io = for {
        client <- BlazeClientBuilder[IO](ExecutionContext.global).stream

        _ <- ImgurShooter.impl(client, uploader).run()

      } yield ()

      io.compile.drain.unsafeRunSync() must beEqualTo(())

    }
  }

}
