import cats.data.{Kleisli, OptionT}
import cats.effect.kernel.Ref
import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s._
import fs2.io.file.{Path, Files}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, Request}
import scala.concurrent.duration.DurationInt
import scala.util.Try


object Main extends IOApp.Simple {

  final case class Counter(counter: Int)

  implicit val counterEncoder: Encoder[Counter] = deriveEncoder

  private def counterService(counter: Ref[IO, Int]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root => for {
      c <- counter.updateAndGet(_ + 1)
      r <- Ok(Counter(c).asJson)
    } yield r

  }

  private def slowServiceMiddleware(slowService: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli {
    (req: Request[IO]) => {
      val segments = req.uri.path.segments
      Try(
        segments
          .slice(1, segments.length)
          .map(_.toString.toInt)
          .foreach(p => if (p <= 0)
            throw new RuntimeException("Wrong path parameters. Path parameters must be greater than 0."))
      ).toOption match {
        case Some(_) => slowService(req)
        case None => OptionT.liftF(BadRequest("Wrong path parameters"))
      }
    }
  }

  private val slowService = HttpRoutes.of[IO] {
    case GET -> Root / IntVar(chunk) / LongVar(total) / IntVar(time) =>
      val path = getClass.getResource("").getPath
      val stream = Files[IO].readRange(Path(s"$path/file.txt"), chunk, 0, total)
        .metered(time.seconds)
      Ok(stream)
  }

  def httpApp(counter: Ref[IO, Int]): HttpRoutes[IO] =
    Router("counter" -> counterService(counter), "slow" -> slowServiceMiddleware(slowService))

  private val server = for {
    c <- Resource.eval(Ref[IO].of(0))
    s <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp(c).orNotFound)
      .build
  } yield s

  override def run: IO[Unit] = server.use(_ => IO.never)
}
