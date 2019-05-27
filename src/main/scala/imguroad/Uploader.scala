package imguroad

import cats.syntax.all._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe._
import fs2._
import fs2.concurrent._
import java.{util => ju}

trait Uploader[F[_]] {
  def upload(urls: Set[Uploader.URL]): F[Uploader.JobID]
  def listen: Stream[F, Uploader.Job]
}

object Uploader {
  implicit def apply[F[_]](implicit ev: Uploader[F]): Uploader[F] = ev

  type URL = String
  type JobID = ju.UUID

  final case class UploadURLsDTO(urls: List[URL])
  object UploadURLsDTO {
    implicit val decoder: Decoder[UploadURLsDTO] = deriveDecoder
    implicit val encoder: Encoder[UploadURLsDTO] = deriveEncoder
  }

  final case class UploadedJobIDDTO(jobId: JobID)
  object UploadedJobIDDTO {
    implicit val decoder: Decoder[UploadedJobIDDTO] = deriveDecoder
    implicit val encoder: Encoder[UploadedJobIDDTO] = deriveEncoder
  }

  sealed trait Issue
  final case object Skip extends Issue
  final case class Job(id: JobID, urls: Set[URL]) extends Issue
  object Job {
    implicit val decoder: Decoder[Job] = deriveDecoder
    implicit val encoder: Encoder[Job] = deriveEncoder
  }

  def impl[F[_]](eventsTopic: Topic[F, Issue])(implicit F: Sync[F]): Uploader[F] =
    new Uploader[F] {
      def upload(urls: Set[Uploader.URL]): F[JobID] =
        for {
          // generating UUID is a side effect
          jobID <- F.delay(ju.UUID.randomUUID())
          job = Job(jobID, urls)
          _ <- eventsTopic.publish1(job)
        } yield jobID
        
      def listen: Stream[F, Uploader.Job] = eventsTopic.subscribe(1).flatMap {
        case Skip => Stream.empty
        case job: Job => Stream.emit(job)
      }
    }
}
