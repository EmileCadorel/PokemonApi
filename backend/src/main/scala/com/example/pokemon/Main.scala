package com.example.pokemon

import cats.effect.{IO, IOApp, ExitCode, Sync}
import cats.syntax.all.*
import com.example.pokemon.data.AppConfig

import io.circe.yaml.parser as yamlParser

object Main extends IOApp {

  def loadConfig[F[_]: Sync]: F[AppConfig] = {
    Sync[F].blocking {
      val stream = getClass.getClassLoader.getResourceAsStream("config.yaml")
      if stream == null then {
        throw new RuntimeException("config.yaml not found on classpath")
      }

      scala.io.Source.fromInputStream(stream).mkString
    }.flatMap { raw =>
      yamlParser.parse(raw) match {
        case Left(err) =>
          Sync[F].raiseError(new RuntimeException(s"YAML parse error: ${err.getMessage}"))

        case Right(json) =>
          json.as[AppConfig] match {
            case Left(err) =>
              Sync[F].raiseError(new RuntimeException(s"Config decode error: ${err.getMessage}"))
            case Right(cfg) =>
              Sync[F].pure(cfg)
          }
      }
    }
  }

  def run (args : List[String]): IO[ExitCode] = {
    for {
      config <- loadConfig[IO]
      _ <- PokemonServer.configure[IO](config).use (IO.pure)
      exit <- PokemonServer.run[IO](config)
    } yield exit        
  }

}
