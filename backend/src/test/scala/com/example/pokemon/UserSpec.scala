package com.example.pokemon

import cats.effect.{IO, Resource, Async}
import cats.implicits.*

import fs2.io.net.Network
// import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.*
import org.http4s.*
import munit.CatsEffectSuite

import com.example.pokemon.data.*
import com.example.pokemon.routes.*

import doobie._
import doobie.implicits.*

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client

import org.mindrot.jbcrypt.BCrypt

class UserSpec extends CatsEffectSuite {

  type TestEnv = () => (AppConfig, Transactor[IO], Client[IO])

  val resources = for {
    config <- Resource.pure (AppConfig (DBConfig ("localhost", 3306, "root", "", "pokemon-test"), "0.0.0.0", 8080))
    _      <- PokemonServer.configure[IO](config)
    xa     <- PokemonServer.createDBTransactor[IO](config.db)
    _      <- Resource.eval (insertDummyUsers[IO](xa))
    cli    <- EmberClientBuilder.default[IO].build
  } yield (config, xa, cli)

  val env = ResourceSuiteLocalFixture("env", resources)
  override def munitFixtures = List(env)

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =====================================          TESTS          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  test ("User alice should be able to login") {
    val (_, xa, _) = env ()
    assertIO(loginUser ("alice", "password", xa).map(_.status), Status.Ok)
  }

  test ("User alice shouldn't be able to login if she uses the wrong password") {
    val (_, xa, _) = env ()
    assertIO(loginUser ("alice", "wrong", xa).map(_.status), Status.Forbidden)
  }


  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =====================================          UTILS          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def loginUser(user: String, pass: String, xa: Transactor[IO]): IO[Response[IO]] = {
    val str = s"/api/login"
    val url = Uri.fromString(str).getOrElse (throw new Exception("Invalid URI"))

    val post = Request[IO](Method.POST, url).withEntity (UserLoginRequest (user, pass))
    val impl = UserManager.impl[IO](new UserRepoLive[IO](xa))
    PokemonRoutes.loginUser (impl).orNotFound(post)
  }

  def insertDummyUsers[F[_]: Async](xa: Transactor[F]): F[Unit] = {
    val password = BCrypt.hashpw("password", BCrypt.gensalt ())

    val q0 = sql"DELETE FROM likes".update
    val q1 = sql"DELETE FROM users".update
    val q2 = sql"""
    INSERT INTO users (login, password)
    VALUES
      ('alice',   $password),
      ('bob',     $password),
      ('charlie', $password)
    """.update

    for {
      _ <- q0.run.transact(xa)
      _ <- q1.run.transact(xa)
      _ <- q2.run.transact(xa)
    } yield ()
  }

}
