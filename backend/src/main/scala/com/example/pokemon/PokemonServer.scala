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
import fs2.io.file.Files

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

  def run[F[_]: Async: Network: Console: Files]: F[Nothing] = {
    for {      
      client <- EmberClientBuilder.default[F].build 
      xa <- makeTransactor[F] // Define the DB transaction system
      
      // DB connection for users
      userRepo = new UserRepoLive[F](xa)

      // Controller for retreiving pokemon information
      pokemonInfoAlg = PokemonInformation.impl[F](client)

      // Controller to manage user connections and user associated requests
      userAlg = UserManager.impl[F](userRepo)

      // Define the routes
      httpApp = ( 
        PokemonRoutes.pokemonInformation[F](pokemonInfoAlg) <+>
          PokemonRoutes.loginUser[F](userAlg) <+>
          PokemonRoutes.likePokemon[F](userAlg) <+>
          PokemonRoutes.frontendResources[F] <+>
          PokemonRoutes.homeResources[F] <+>
          PokemonRoutes.indexHTML[F]
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, logBody = false)(httpApp)

      _ <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build      
    } yield ()
  }.useForever

}
