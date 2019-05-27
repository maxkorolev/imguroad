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

class UploaderSpec extends org.specs2.mutable.Specification {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  "Uploader" >> {
    "make its job - create jobs" >> {

      val io = for {
        topic <- Stream.eval(Topic[IO, Uploader.Issue](Uploader.Skip))
        service = Uploader.impl[IO](topic)

        publishing = Stream.range(0, 10).map(_.toString).evalMap(_ => service.upload(Set("1", "2", "3")))

        job <- publishing zip service.listen

      } yield {
        job match {
          case (jobID, Job(id, urls)) => jobID == id
          case (_, Skip) => false
        }
      }
      
      io.compile.toList.unsafeRunSync() must beEqualTo ((0 to 9).map(_ => true).toList)

    }
  }


}
