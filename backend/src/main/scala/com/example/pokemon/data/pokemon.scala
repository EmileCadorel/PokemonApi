package com.example.pokemon.data

import cats.effect.Concurrent
import io.circe.{Encoder, Decoder}
import org.http4s.circe.*
import org.http4s.{EntityEncoder, EntityDecoder}
import cats.syntax.all.*

import io.circe.*

final case class Pokemon(id : Int, name: String, weight: Int, sprites: PokemonSprite, types: List[PokemonType])
final case class PokemonSprite(front_default: String)
final case class PokemonType(value: String)

object Pokemon {
  import io.circe.{Json, HCursor, DecodingFailure}

  def decodeTypeOrValue(cursor: HCursor): Either[DecodingFailure, String] = {
    // Case 1: { "type": { "name": ... } }
    cursor.downField("type").as[Json] match {
      case Right(json) =>
        json.asObject match {
          case Some(obj) =>
            obj("name") match {
              case Some(nested) =>
                nested.asString match {
                  case Some(s) => Right(s)
                  case None =>
                    Left(DecodingFailure("Expected type.name to be a string", cursor.history))
                }
              case None =>
                Left(DecodingFailure("Expected type.name field", cursor.history))
            }
          case None =>
            Left(DecodingFailure("Expected type to be an object", cursor.history))
        }

      // Case 2: { "value": ... }
      case Left(_) => 
        cursor.downField("value").as[String].leftMap { err =>
          DecodingFailure("Expected value field to be a string", cursor.history)
        }
    }
  }

  given Encoder[Pokemon] = Encoder.AsObject.derived[Pokemon]
  given Decoder[Pokemon] = Decoder.derived[Pokemon]

  given Decoder[PokemonSprite] = Decoder.derived[PokemonSprite]
  given Encoder[PokemonSprite] = Encoder.AsObject.derived[PokemonSprite]

  given Decoder[PokemonType] = Decoder.instance { cursor =>
    decodeTypeOrValue(cursor).map(v => PokemonType (v))
  }
  
  given Encoder[PokemonType] = Encoder.AsObject.derived[PokemonType]

  given [F[_]: Concurrent]: EntityDecoder[F, Pokemon] = jsonOf
  given [F[_]]: EntityEncoder[F, Pokemon] = jsonEncoderOf

}
