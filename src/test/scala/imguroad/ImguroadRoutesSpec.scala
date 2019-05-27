package imguroad

import fs2._
import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import java.{util => ju}

class ImguroadRoutesSpec extends org.specs2.mutable.Specification {

  "ImguroadRoutes" >> {
    "return 200" >> {
      uriReturns200()
    }
  }

  private[this] val retHelloWorld: Response[IO] = {
    val req = Request[IO](Method.POST, uri"/v1/images/upload").withEntity(
      """
    |{
    |  "urls": [
    |    "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg",
    |    "https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"
    |    ]
    |}""".stripMargin
    )

    val uploader = new Uploader[IO] {
      def upload(urls: Set[Uploader.URL]): IO[Uploader.JobID] =
        IO.delay(ju.UUID.randomUUID())
      def listen: Stream[IO, Uploader.Job] = Stream.empty
    }

    ImguroadRoutes
      .uploadRoutes(uploader)
      .orNotFound(req)
      .unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retHelloWorld.status must beEqualTo(Status.Ok)

}
