package com.example.pokemon

import cats.effect.Sync
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object PokemonRoutes {

  /**
    * Pokemon information page :
    * - Get /pokemon-info/{name}    
    */
  def pokemonInformation[F[_] : Sync](M : PokemonInformation[F]): HttpRoutes[F] = {
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

}
