package imguroad

import fs2._
import fs2.concurrent._
import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import scala.concurrent.ExecutionContext

class UploaderSpec extends org.specs2.mutable.Specification {

  "Uploader" >> {
    "make its job - create jobs" >> {

      program.compile.drain.unsafeRunSync() must beEqualTo (())
    }
  }

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  def program = for {
    topic <- Stream.eval(Topic[IO, Uploader.Issue](Uploader.Skip))
    service = Uploader.impl[IO](topic)

    jobID <- Stream.eval(service.upload(Set("1", "2", "3")))

  } yield jobID

}
