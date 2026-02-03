package com.example.pokemon

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = PokemonServer.run[IO]
}
