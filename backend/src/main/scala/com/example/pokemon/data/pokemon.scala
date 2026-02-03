package com.example.pokemon.data

import cats.effect.Concurrent
import io.circe.{Encoder, Decoder}
import org.http4s.circe.*
import org.http4s.{EntityEncoder, EntityDecoder}

final case class Pokemon(id : Int, name: String, weight: Int, sprites: PokemonSprite, types: List[PokemonType])
final case class PokemonSprite(front_default: String)
final case class PokemonType(`type`: PokemonTypeContent)
final case class PokemonTypeContent(name: String)

object Pokemon {
  given Encoder[Pokemon] = Encoder.AsObject.derived[Pokemon]
  given Decoder[Pokemon] = Decoder.derived[Pokemon]

  given Decoder[PokemonSprite] = Decoder.derived[PokemonSprite]
  given Encoder[PokemonSprite] = Encoder.AsObject.derived[PokemonSprite]

  given Decoder[PokemonType] = Decoder.derived[PokemonType]
  given Encoder[PokemonType] = Encoder.AsObject.derived[PokemonType]

  given Decoder[PokemonTypeContent] = Decoder.derived[PokemonTypeContent]
  given Encoder[PokemonTypeContent] = Encoder.AsObject.derived[PokemonTypeContent]

  given [F[_]: Concurrent]: EntityDecoder[F, Pokemon] = jsonOf
  given [F[_]]: EntityEncoder[F, Pokemon] = jsonEncoderOf
}
