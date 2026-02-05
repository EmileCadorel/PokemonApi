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
import doobie.implicits._
import doobie.util.ExecutionContexts

import com.example.pokemon.routes.*
import com.example.pokemon.data.{DBConfig, AppConfig}

object PokemonServer {

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ======================================          SQL          =======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def createAdminXA[F[_]: Async](config: DBConfig): Transactor[F] = {
    Transactor.fromDriverManager[F](
      driver = "com.mysql.cj.jdbc.Driver",  // JDBC driver classname
      url = s"jdbc:mysql://${config.addr}:${config.port}/",     // Connect URL - Driver specific
      user = config.user,                 // Database user name
      password = config.password,             // Database password
      logHandler = None                  // Don't setup logging for now. See Logging page for how to log events in detail
    )
  }

  def createDBXA[F[_]: Async](config: DBConfig): Transactor[F] = {
    Transactor.fromDriverManager[F](
      driver = "com.mysql.cj.jdbc.Driver",  // JDBC driver classname
      url = s"jdbc:mysql://${config.addr}:${config.port}/${config.name}",     // Connect URL - Driver specific
      user = config.user,                 // Database user name
      password = config.password,             // Database password
      logHandler = None                  // Don't setup logging for now. See Logging page for how to log events in detail
    )
  }

  def createDBTransactor[F[_]: Async](config: DBConfig): Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) 
      xa <- HikariTransactor.newHikariTransactor[F](
        driverClassName = "com.mysql.cj.jdbc.Driver",
        url             = s"jdbc:mysql://${config.addr}:${config.port}/${config.name}",
        user            = config.user,
        pass            = config.password,
        connectEC       = ce
      )
    } yield xa
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ===============================          CREATION TABLE/DB          ================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def ensureDatabase[F[_]: Async](dbName: String, adminXa: Transactor[F]): F[Unit] = {
    // Ugly but apparently, doobie does not support interpolation inside a CREATE Database query
    val dbIdent = Fragment.const(s"`$dbName`")
    (fr"CREATE DATABASE IF NOT EXISTS " ++ dbIdent)
      .update
      .run
      .transact(adminXa)
      .void    
  }

  def ensureTables[F[_]: Async](adminXa: Transactor[F]): F[Unit] = {
    val q1 = sql"""CREATE TABLE IF NOT EXISTS users (id INT NOT NULL AUTO_INCREMENT,
                                                   login VARCHAR (255) NOT NULL,
                                                   password VARCHAR (255) NOT NULL,
                                                   PRIMARY KEY (id))
    """.update

    val q2 = sql"""CREATE TABLE if NOT EXISTS likes (id INT NOT NULL AUTO_INCREMENT,
                                                     user_id INT NOT NULL,
                                                     pokemon_id INT NOT NULL,
                                                     PRIMARY KEY (id), FOREIGN KEY (user_id) REFERENCES users(id))
    """.update

    val q3 = sql"DROP TABLE IF EXISTS pokemon_names".update ;                                                             
    val q4 = sql"""CREATE TABLE if NOT EXISTS pokemon_names (id INT NOT NULL AUTO_INCREMENT,
                                                             name VARCHAR(30) NOT NULL,
                                                             PRIMARY KEY (id))                                                     
    """.update

    for {
      _ <- q1.run.transact (adminXa)
      _ <- q2.run.transact (adminXa)
      _ <- q3.run.transact (adminXa)
      _ <- q4.run.transact (adminXa)
    } yield ()
  }

  /**
    * Insert all pokemon names in the table
    */
  def insertAllPokemons[F[_]: Async](names: List[String], xa: Transactor[F]): F[Unit] = {
    val q = "INSERT INTO pokemon_names (name) VALUES (?) ON DUPLICATE KEY UPDATE name = VALUES(name)"
    Update[String](q)
      .updateMany(names)
      .transact(xa)
      .void
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =================================          CONFIGURATION          ==================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def configure[F[_]: Async: Network: Console](config: AppConfig) = {
    for {
      adminXa <- Resource.pure(createAdminXA[F](config.db))
      _       <- Resource.eval(ensureDatabase[F](config.db.name, adminXa))
      dbXa    <- Resource.pure(createDBXA[F](config.db))
      _       <- Resource.eval(ensureTables[F](dbXa))   
      cl      <- EmberClientBuilder.default[F].build

      pokemonInfoAlg = PokemonInformation.impl[F](cl, new PokemonRepoDummy ())      
      lst     <- Resource.eval(pokemonInfoAlg.findPokemonNames)
      _       <- Resource.eval(insertAllPokemons[F](lst.names, dbXa))
    } yield ()
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ====================================          RUNNING          =====================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def run[F[_]: Async: Network: Console: Files](config: AppConfig): F[Nothing] = {
    for {      
      client <- EmberClientBuilder.default[F].build
      mainXa <- createDBTransactor[F](config.db)
      
      // DB connection for users
      userRepo = new UserRepoLive[F](mainXa)
      pokemonRepo = new PokemonRepoLive[F](mainXa)

      // Controller for retreiving pokemon information
      pokemonInfoAlg = PokemonInformation.impl[F](client, pokemonRepo)

      // Controller to manage user connections and user associated requests
      userAlg = UserManager.impl[F](userRepo)

      // Define the routes
      httpApp = ( 
        PokemonRoutes.pokemonInformation[F](pokemonInfoAlg) <+>
          PokemonRoutes.suggestPokemons[F](pokemonInfoAlg) <+>
          PokemonRoutes.loginUser[F](userAlg) <+>
          PokemonRoutes.likePokemon[F](userAlg) <+>
          PokemonRoutes.frontendResources[F] <+>
          PokemonRoutes.homeResources[F] <+>
          PokemonRoutes.indexHTML[F]
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, logBody = false)(httpApp)

      host = Host.fromString(config.addr).getOrElse(sys.error(s"Invalid host: ${config.addr}"))
      port = Port.fromInt(config.port).getOrElse(sys.error(s"Invalid port: ${config.port}"))
      _ <-
        EmberServerBuilder.default[F]
          .withHost(host)
          .withPort(port)
          .withHttpApp(finalHttpApp)
          .build      
    } yield ()
  }.useForever

}
