package com.example.pokemon

import cats.effect.{Async, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import cats.effect.std.Console

import doobie._
import doobie.hikari._
import doobie.util.ExecutionContexts

import com.example.pokemon.routes.*

object PokemonServer {

  def makeTransactor[F[_]: Async]: Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) 
      xa <- HikariTransactor.newHikariTransactor[F](
        driverClassName = "com.mysql.cj.jdbc.Driver",
        url             = "jdbc:mysql://localhost:3306/mydb",
        user            = "root",
        pass            = "",
        connectEC       = ce
      )
    } yield xa
  }

  def run[F[_]: Async: Network: Console]: F[Nothing] = {
    for {      
      client <- EmberClientBuilder.default[F].build
      xa <- makeTransactor[F]

      userRepo = new UserRepoLive[F](xa)
      pokemonInfoAlg = PokemonInformation.impl[F](client)
      userAlg = UserManager.impl[F](userRepo)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = ( 
          PokemonRoutes.pokemonInformation[F](pokemonInfoAlg) <+>
            PokemonRoutes.loginUser[F](userAlg) <+>
            PokemonRoutes.home[F] <+>
            PokemonRoutes.likePokemon[F](userAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever

}
