import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import io.circe.Json
import io.circe.syntax.KeyOps
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request}
import org.scalatest.funsuite.AnyFunSuite

class CounterServiceTest extends AnyFunSuite {
  test("response on request to '/counter' uri") {
    val req: Request[IO] = Request[IO](Method.GET, uri = uri"/counter")
    val clientIo: IO[Client[IO]] = for {
      c <- Ref[IO].of(0)
      ca = Client.fromHttpApp(Main.httpApp(c).orNotFound)
    } yield ca

    val client = clientIo.unsafeRunSync
    val expectedCounterOne = Json.obj({
      "counter" := 1
    })
    val expectedCounterTwo = Json.obj({
      "counter" := 2
    })
    val expectedCounterThree = Json.obj({
      "counter" := 3
    })

    val resOne = client.expect[Json](req)
    assert(resOne.unsafeRunSync == expectedCounterOne)

    val resTwo = client.expect[Json](req)
    assert(resTwo.unsafeRunSync == expectedCounterTwo)

    val resThree = client.expect[Json](req)
    assert(resThree.unsafeRunSync == expectedCounterThree)
  }
}
