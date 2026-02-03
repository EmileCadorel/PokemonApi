package com.example.pokemon

import org.http4s.syntax.all.uri
import cats.effect.{Concurrent}
import cats.syntax.all.*
import cats.effect.std.Console
import org.http4s.{HttpRoutes, DecodeFailure, Request}
import org.http4s.dsl.Http4sDsl

import org.http4s.headers.Location

import com.example.pokemon.routes.*
import com.example.pokemon.data.{UserLikeRequest}

object PokemonRoutes {

  /**
    * Pokemon information page :
    * - Get /pokemon-info/{name}    
    */
  def pokemonInformation[F[_] : Concurrent](M : PokemonInformation[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl.*;
    HttpRoutes.of[F] {
      case GET -> Root / "pokemon-info" / name => {        
        M.get(name)
          .flatMap {
            result => Ok (result)
          }.handleErrorWith {
            case PokemonInformationError(t) =>
              NotFound()
          }        
      }
    }
  }

  def loginUser[F[_] : Concurrent](M : UserManager[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl.*;
    HttpRoutes.of[F] {
      case GET -> Root / "login" / name / password => {
        M.login(name, password)
          .flatMap {
            result => Ok (result)
          }.handleErrorWith {
            case _ =>
              NotFound()
          }
      }
    }
  }

  def home[F[_] : Concurrent]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl.*;
    HttpRoutes.of[F] {
      case GET -> Root / "home" => {
        Ok (Concurrent[F].pure ("Home"))
      }
    }
  }
  

  def likePokemon[F[_] : Concurrent: Console](M : UserManager[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl.*;
    HttpRoutes.of[F] {
      case req @ (POST -> Root / "like") => {
        (for {
          body <- req.as[UserLikeRequest]
          _ <- Console[F].println(s"=========== User user: $body")
          _ <- M.like (body.userId, body.jwt, body.pokemonId)
          result <- Ok ()
        } yield result).handleErrorWith {
          // JSON decoding error case
          case df: DecodeFailure =>
            BadRequest(s"Invalid JSON: ${df.getMessage}")

          // 
          case _: UserError =>
            SeeOther (Location (uri"/home"))            

          // unexpected errors
          case e =>
            InternalServerError(s"Unexpected error: ${e}")           
        }
      }
    }
  }


}
