package com.example.pokemon

import cats.effect.{IO, Resource}
import fs2.io.net.Network
// import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*
import org.http4s.*
import munit.CatsEffectSuite

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client

import com.example.pokemon.data.*
import com.example.pokemon.routes.*

import doobie._


class PokemonInformationSpec extends CatsEffectSuite {

  type TestEnv = () => (AppConfig, Transactor[IO], Client[IO])

  val resources = for {
    config <- Resource.pure (AppConfig (DBConfig ("localhost", 3306, "root", "", "pokemon-test"), "0.0.0.0", 8080))
    _      <- PokemonServer.configure[IO](config)
    xa     <- PokemonServer.createDBTransactor[IO](config.db)
    cli    <- EmberClientBuilder.default[IO].build
  } yield (config, xa, cli)


  val env = ResourceSuiteLocalFixture("env", resources)
  override def munitFixtures = List(env)
  
  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ===================================          INFO TESTS          ===================================
  * ====================================================================================================
  * ====================================================================================================
  */

  test("PokemonInformation returns status code 404 for unknown pokemon") {    
    val (_, _, cli) = env()    
    assertIO(retPokemonInformationFstTest("patate", cli).map(_.status) ,Status.NotFound)    
  }

  test("PokemonInformation returns status code 200 for known pokemon") {    
    val (_, _, cli) = env()
    assertIO(retPokemonInformationFstTest("charizard", cli).map(_.status) ,Status.Ok)    
  }

  test("PokemonInformation returns valid informations for pokemon Ditto") {    
    val (_, _, cli) = env()
    retPokemonInformationFstTest("ditto", cli).flatMap { response =>
      response.as[Pokemon].map { pokemon =>
        assertEquals(pokemon.name, "ditto")
        assertEquals(pokemon.id, 132)
        assertEquals(pokemon.weight, 40)
        assertEquals(pokemon.types.length, 1)
        assertEquals(pokemon.types(0).value.name, "normal")      
      }
    }
  }

  test("PokemonInformation returns valid informations for pokemon Pikachu") {    
    val (_, _, cli) = env ()
    retPokemonInformationFstTest ("pikachu", cli).flatMap { response =>
      response.as[Pokemon].map {
        pokemon => {
          assertEquals(pokemon.name, "pikachu")
          assertEquals(pokemon.id, 25)
          assertEquals(pokemon.weight, 60)
          assertEquals (pokemon.types.length, 1)
          assertEquals(pokemon.types (0).value.name, "electric")
        }
      }
    }
  }

  test("PokemonInformation returns valid informations for pokemon Charizard") {
    val (_, _, cli) = env ()
    retPokemonInformationFstTest ("charizard", cli).flatMap { response =>
      response.as[Pokemon].map {
        pokemon => {
          assertEquals(pokemon.name, "charizard")
          assertEquals(pokemon.id, 6)
          assertEquals(pokemon.weight, 905)
          assertEquals (pokemon.types.length, 2)
          assertEquals(pokemon.types (0).value.name, "fire")
          assertEquals(pokemon.types (1).value.name, "flying")
        }
      }
    }
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ==================================          SUGGEST TEST          ==================================
  * ====================================================================================================
  * ====================================================================================================
  */

  test("Pokemon suggestion returns a list of names") {
    val (_, xa, cli) = env ()
    retPokemonSuggests ("ch", xa, cli).flatMap { response =>
      response.as[PokemonList].map {
        lst => {
          assertEquals(lst.names, List(
            "chandelure",
            "chandelure-mega",
            "chansey",
            "charcadet",
            "charizard"
          ))
        }
      }
    }
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =====================================          UTILS          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def retPokemonInformationFstTest(pokemon: String, cli: Client[IO]): IO[Response[IO]] = {
    val impl = PokemonInformation.impl[IO](cli, new PokemonRepoDummy)
    val uri  = uri"http://localhost/api/pokemon-info" / pokemon
    val req  = Request[IO](Method.GET, uri)

    PokemonRoutes
      .pokemonInformation(impl)
      .orNotFound(req)
  }

  def retPokemonSuggests(pokemon: String, xa: Transactor[IO], cli : Client[IO]): IO[Response[IO]] = {
    val impl = PokemonInformation.impl[IO](cli, new PokemonRepoLive[IO](xa))
    val uri  = uri"http://localhost/api/pokemon-lists" / pokemon
    val req  = Request[IO](Method.GET, uri)

    PokemonRoutes
      .suggestPokemons(impl)
      .orNotFound(req)
  }

}
