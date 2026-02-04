package com.example.pokemon.routes

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import cats.effect.std.Console

import org.http4s.*
import org.http4s.client.dsl.Http4sClientDsl

import com.example.pokemon.data.{UserPass, UserConnected}
import com.example.pokemon.utils.Jwt

import doobie.*
import doobie.implicits.*

import org.mindrot.jbcrypt.BCrypt

/*!
* ====================================================================================================
* ====================================================================================================
* =======================================          DB          =======================================
* ====================================================================================================
* ====================================================================================================
*/

trait UserRepo[F[_]] {
  def find (name: String): F[Option[UserPass]]

  def findLike (userId: Int, pokemonId: Int): F[Option[Int]]
  def insertLike (userId: Int, pokemonId: Int): F[Int]

  def like (userId: Int, pokemonId: Int): F[Int]
}

class UserRepoLive[F[_]: Async](xa: Transactor[F]) extends UserRepo[F] {

  def find(login: String): F[Option[UserPass]] =
    sql"SELECT id, login, password FROM users WHERE login = $login"
      .query[UserPass]
      .option
      .transact(xa)

  def findLike (userId: Int, pokemonId: Int): F[Option[Int]] =
    sql"SELECT id FROM likes WHERE user_id = $userId and pokemon_id = $pokemonId"
      .query[Int]
      .option
      .transact(xa)

  def insertLike(userId: Int, pokemonId: Int): F[Int] =
    sql"INSERT INTO likes (user_id, pokemon_id) VALUES ($userId, $pokemonId)"
      .update
      .run
      .transact(xa)      

  /**
    * Insert if not exist
    */
  def like(userId: Int, pokemonId: Int): F[Int] =
    findLike(userId, pokemonId).flatMap {
      case Some(id) => Concurrent[F].pure(id)
      case None     => insertLike(userId, pokemonId)
    }

}

/*!
* ====================================================================================================
* ====================================================================================================
* =====================================          ERRORS          =====================================
* ====================================================================================================
* ====================================================================================================
*/

trait UserError extends Throwable
case class UserNotFound(name: String) extends UserError
case class UserPassMismatch(name: String) extends UserError
case class UserNotConnected() extends UserError

/*!
* ====================================================================================================
* ====================================================================================================
* ==================================          ROUTE TRAIT          ===================================
* ====================================================================================================
* ====================================================================================================
*/

trait UserManager[F[_]] {
  def login(n : String, pass: String): F[UserConnected]
  def like(userId: Int, jwt : String, pokemonId: Int): F[Unit]
}

object UserManager:
  def impl[F[_]: Concurrent: Console](repo: UserRepo[F]): UserManager[F] = new UserManagerImpl[F](repo)


/*!
* ====================================================================================================
* ====================================================================================================
* ======================================          IMPL          ======================================
* ====================================================================================================
* ====================================================================================================
*/

class UserManagerImpl[F[_]: Concurrent: Console](repo: UserRepo[F]) extends UserManager[F] {
  val dsl = new Http4sClientDsl[F]{}
//  import dsl.*

  /**
    * Implementation of the user login 
    */
  def login (name : String, password: String) : F[UserConnected] = {
     repo.find(name).flatMap {
       case Some(user) => {
         for {
           // Just a debug console for testing how basic io works
           _ <- Console[F].println(s"=========== Fetching user: $name -> $user")
           result <- {
             // Check for the hash of the password (maybe it can be put on the client side to avoid sending plain text password through netword ;) )
             if (BCrypt.checkpw (password, user.password)) { 
               Concurrent[F].pure(UserConnected (user.id, user.name, Jwt (Map ("login"-> user.name, "id"-> user.id, "issued"-> s"@UserManager"))))
             } else {
               Concurrent[F].raiseError (UserPassMismatch (name))
             }
           }
         } yield result
       }
       case None => Concurrent[F].raiseError (UserNotFound (name))    
     }
  }

  /**
    *  A user request to like a pokemon
    */
  def like (userId: Int, jwt: String, pokemonId: Int): F[Unit] = {
    jwt match {
      case Jwt (claims) => {
        if (claims.contains ("id") && claims ("id") == userId) {
          for {
            _ <- repo.like (userId, pokemonId)
            result <- Concurrent[F].unit
          } yield result
        } else {
          Concurrent[F].raiseError (UserNotConnected ())
        }
      }
      case _ => {
        Concurrent[F].raiseError (UserNotConnected ())
      }
    }
  }

}
