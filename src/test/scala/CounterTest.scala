import cats.data.Kleisli
import cats.effect.{IO, Ref, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request}
import org.scalatest.funsuite.AnyFunSuite

class CounterTest extends AnyFunSuite {
  test("response on request to '/counter' uri") {
  }
}
