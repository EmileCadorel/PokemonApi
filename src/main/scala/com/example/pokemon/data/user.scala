package com.example.pokemon.data

import cats.effect.Concurrent
import io.circe.{Encoder, Decoder}
import org.http4s.circe.*
  import org.http4s.{EntityEncoder, EntityDecoder}

case class User (id : Int, name: String)
object User {
  given Decoder[User] = Decoder.derived[User]
  given Encoder[User] = Encoder.AsObject.derived[User]

  given [F[_]: Concurrent]: EntityDecoder[F, User] = jsonOf
  given [F[_]]: EntityEncoder[F, User] = jsonEncoderOf
}

