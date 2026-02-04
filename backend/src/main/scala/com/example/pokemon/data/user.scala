package com.example.pokemon.data

import cats.effect.{Concurrent}
import io.circe.{Encoder, Decoder}
import org.http4s.circe.*
import org.http4s.{EntityEncoder, EntityDecoder}


/*!
* ====================================================================================================
* ====================================================================================================
* ============================          RESPONSE AND INNER DATAS          ============================
* ====================================================================================================
* ====================================================================================================
*/

case class User (id : Int, name: String)
object User {
  given Decoder[User] = Decoder.derived[User]
  given Encoder[User] = Encoder.AsObject.derived[User]

  given [F[_]: Concurrent]: EntityDecoder[F, User] = jsonOf
  given [F[_]]: EntityEncoder[F, User] = jsonEncoderOf
}

case class UserPass (id : Int, name: String, password: String)
object UserPass {
  given Decoder[UserPass] = Decoder.derived[UserPass]
  given Encoder[UserPass] = Encoder.AsObject.derived[UserPass]

  given [F[_]: Concurrent]: EntityDecoder[F, UserPass] = jsonOf
  given [F[_]]: EntityEncoder[F, UserPass] = jsonEncoderOf
}

case class UserConnected (id : Int, name: String, jwt: String)
object UserConnected {
  given Decoder[UserConnected] = Decoder.derived[UserConnected]
  given Encoder[UserConnected] = Encoder.AsObject.derived[UserConnected]

  given [F[_]: Concurrent]: EntityDecoder[F, UserConnected] = jsonOf  
  given [F[_]]: EntityEncoder[F, UserConnected] = jsonEncoderOf
}

/*!
* ====================================================================================================
* ====================================================================================================
* ====================================          REQUESTS          ====================================
* ====================================================================================================
* ====================================================================================================
*/

case class UserLikeRequest (userId : Int, jwt : String, pokemonId: Int)
object UserLikeRequest {
  given Decoder[UserLikeRequest] = Decoder.derived[UserLikeRequest]
  given Encoder[UserLikeRequest] = Encoder.AsObject.derived[UserLikeRequest]
  
  given [F[_]: Concurrent]: EntityDecoder[F, UserLikeRequest] = jsonOf
  given [F[_]]: EntityEncoder[F, UserLikeRequest] = jsonEncoderOf  
}

case class UserLoginRequest (login : String, password : String)
object UserLoginRequest {
  given Decoder[UserLoginRequest] = Decoder.derived[UserLoginRequest]
  given Encoder[UserLoginRequest] = Encoder.AsObject.derived[UserLoginRequest]
  
  given [F[_]: Concurrent]: EntityDecoder[F, UserLoginRequest] = jsonOf
  given [F[_]]: EntityEncoder[F, UserLoginRequest] = jsonEncoderOf  
}

