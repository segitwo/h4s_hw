import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request}
import org.scalatest.funsuite.AnyFunSuite
import fs2._

class SlowServiceTest extends AnyFunSuite {
  test("streaming response on response to /slow/1/10/1") {
    val req: Request[IO] = Request[IO](Method.GET, uri = uri"/slow/1/10/1")
    val clientIo: IO[Client[IO]] = for {
      c <- Ref[IO].of(0)
      ca = Client.fromHttpApp(Main.httpApp(c).orNotFound)
    } yield ca

    val client: Client[IO] = clientIo.unsafeRunSync
    val res: Stream[IO, Byte] = client.stream(req).flatMap(_.body)
    val resList = res.compile.toList.unsafeRunSync
    assert(resList.length == 10)

    val resString = new String(resList.toArray)
    assert(resString == "Lorem ipsu")
  }
}
