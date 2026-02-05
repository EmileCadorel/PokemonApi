package com.example.pokemon.routes

import cats.effect.{Concurrent, Async}
import cats.effect.std.Console

import cats.syntax.all.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*

import com.example.pokemon.data.*

import doobie.*
import doobie.implicits.*


/*!
* ====================================================================================================
* ====================================================================================================
* =======================================          DB          =======================================
* ====================================================================================================
* ====================================================================================================
*/

trait PokemonRepo[F[_]] {
  def suggest (name: String): F[List[String]]
}

class PokemonRepoDummy[F[_]: Async] extends PokemonRepo[F] {
  def suggest(name: String): F[List[String]] = {
    Async[F].pure(List.empty[String])    
  }
}

class PokemonRepoLive[F[_]: Async](xa: Transactor[F]) extends PokemonRepo[F] {
  def suggest(start: String): F[List[String]] =
    sql"SELECT name FROM pokemon_names WHERE name LIKE ${start + "%"} LIMIT 5"
      .query[String]
      .to[List]
      .transact(xa)
}


/*!
* ====================================================================================================
* ====================================================================================================
* =====================================          ERRORS          =====================================
* ====================================================================================================
* ====================================================================================================
*/

final case class PokemonInformationError(e: Throwable) extends RuntimeException

/*!
* ====================================================================================================
* ====================================================================================================
* =====================================          TRAIT          ======================================
* ====================================================================================================
* ====================================================================================================
*/

trait PokemonInformation[F[_]] {
  def get(n : String): F[Pokemon]
  def findPokemonNames: F[PokemonList]
  def suggest(start: String): F[PokemonList]
}

object PokemonInformation:
  def impl[F[_]: Concurrent: Console](C: Client[F], repo: PokemonRepo[F]): PokemonInformation[F] = new PokemonInformationImpl[F](C, repo)

/*!
* ====================================================================================================
* ====================================================================================================
* =================================          IMPLEMENTATION          =================================
* ====================================================================================================
* ====================================================================================================
*/

class PokemonInformationImpl[F[_]: Concurrent: Console](C : Client[F], repo: PokemonRepo[F]) extends PokemonInformation[F] {
  val dsl = new Http4sClientDsl[F]{}
  import dsl.*

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ================================          PokeAPI pipeline          ================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Find information about a specific pokemon using its name
    */
  def get (name : String) = {
    val str = s"https://pokeapi.co/api/v2/pokemon/$name";
    C.expect[Pokemon] (GET (Uri.fromString (str).valueOr (throw _)))
      .adaptError ({ case t =>        
        PokemonInformationError (t)
      })
  }

  /**
    * List all pokemon on PokeAPI and return their names
    */
  def findPokemonNames = {
    val str = s"https://pokeapi.co/api/v2/pokemon?limit=2000";
    C.expect[PokemonList] (GET (Uri.fromString (str).valueOr (throw _)))
      .adaptError ({ case t =>        
        PokemonInformationError (t)
      })    
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ====================================          DB SEARC          ====================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Suggest a list of names starting with 'start'
    */
  def suggest(start: String) = {
    repo.suggest(start)
      .flatMap { result =>        
        PokemonList(result).pure[F]
      } .handleErrorWith { err =>        
        PokemonInformationError(err).raiseError[F, PokemonList]
      }
  }

}

