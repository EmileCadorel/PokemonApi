package com.example.pokemon

import cats.effect.IO
import fs2.io.net.Network
// import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*
import org.http4s.*
import munit.CatsEffectSuite

import org.http4s.ember.client.EmberClientBuilder


import com.example.pokemon.data.Pokemon

class PokemonInformationSpec extends CatsEffectSuite {

  test("PokemonInformation returns status code 404 for unknown pokemon") {
    assertIO(retPokemonInformationFstTest("patate").map(_.status) ,Status.NotFound)
  }

  test("PokemonInformation returns status code 200 for known pokemon") {
    assertIO(retPokemonInformationFstTest("charizard").map(_.status) ,Status.Ok)
  }

  test("PokemonInformation returns valid informations for pokemon Ditto") {
    retPokemonInformationFstTest ("ditto").flatMap { response => response.as[Pokemon].map {
      pokemon => {
        assertEquals(pokemon.name, "ditto")
        assertEquals(pokemon.id, 132)
        assertEquals(pokemon.weight, 40)
        assertEquals (pokemon.types.length, 1)
        assertEquals(pokemon.types (0).`type`.name, "normal")
      }
    }}    
  }

  test("PokemonInformation returns valid informations for pokemon Pikachu") {
    retPokemonInformationFstTest ("pikachu").flatMap { response => response.as[Pokemon].map {
      pokemon => {
        assertEquals(pokemon.name, "pikachu")
        assertEquals(pokemon.id, 25)
        assertEquals(pokemon.weight, 60)
        assertEquals (pokemon.types.length, 1)
        assertEquals(pokemon.types (0).`type`.name, "electric")
      }
    }}    
  }

  test("PokemonInformation returns valid informations for pokemon Charizard") {
    retPokemonInformationFstTest ("charizard").flatMap { response => response.as[Pokemon].map {
      pokemon => {
        assertEquals(pokemon.name, "charizard")
        assertEquals(pokemon.id, 6)
        assertEquals(pokemon.weight, 905)
        assertEquals (pokemon.types.length, 2)
        assertEquals(pokemon.types (0).`type`.name, "fire")
        assertEquals(pokemon.types (1).`type`.name, "flying")
      }
    }}    
  }


  def retPokemonInformationFstTest(pokemon: String) = {
    EmberClientBuilder.default[IO].build.use { cli =>
      val str = s"/pokemon-info/${pokemon}"      
      val url = Uri.fromString(str).getOrElse (throw new Exception("Invalid URI"))

      val get = Request[IO](Method.GET, url)
      val impl = PokemonInformation.impl[IO](cli)
      PokemonRoutes.pokemonInformation (impl).orNotFound(get)
    }
  }

}
