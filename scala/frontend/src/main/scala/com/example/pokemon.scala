package com.example

import tyrian.*
import tyrian.http.*

import cats.effect.IO
import cats.syntax.either.*
import cats.implicits.*

import io.circe.HCursor
import io.circe.Json
import io.circe.parser.*

object PokemonApi {

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ==================================          INFORMATION          ===================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Decode a pokemon json
    */
  def jsonInfoDecode(cursor: HCursor): Either[String, Pokemon] = {
    for {
      id <- cursor.get[Int]("id").leftMap(_.message)
      name <- cursor.get[String]("name").leftMap(_.message)
      weight <- cursor.get[Int]("weight").leftMap(_.message)
      sprite <- cursor.downField ("sprites").get[String]("front_default").leftMap(_.message)      
      types <- {
        cursor.downField ("types").as[List[Json]].leftMap(_.message)
          .flatMap { arr =>
            arr.traverse { json => 
              json.hcursor.get[String]("value").leftMap(_.message)
            }
          }
      }
    } yield Pokemon (id, name, weight, sprite, types)
  }

  /**
    * Store a function that transform a server response into a message
    */
  private val onInfoResponse: Response => Msg = { response =>
    parse(response.body)
      .leftMap(_.message)
      .flatMap(j => jsonInfoDecode(j.hcursor))
      .fold(
        err => Msg.PMsg (PokemonMessage.InfoFailed(err)),
        pok => Msg.PMsg (PokemonMessage.InfoSucceeded (pok))
      )       
  }

  /**
    * Store a function that transform a server error into a message
    */
  private val onInfoError: HttpError => Msg =
    e => Msg.PMsg (PokemonMessage.InfoFailed (e.toString))


  /**
    * Decode an information retreive
    */
  def fromInfoHttpResponse: tyrian.http.Decoder[Msg] = {
    tyrian.http.Decoder[Msg](onInfoResponse, onInfoError)
  }
  
  /**
    * Call a api info get and return the callback command 
    */
  def search(name: String): Cmd[IO, Msg] = {
    Http.send(
      Request.get(s"/api/pokemon-info/${name}"),
      PokemonApi.fromInfoHttpResponse
    )
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ===================================          SUGGESTION          ===================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Store a function that transform a server response into a message
    */
  private val onSuggestResponse: Response => Msg = { response =>
    parse(response.body)
      .leftMap(_.message)
      .flatMap(j => j.hcursor.get[List[String]]("names").leftMap(_.message))      
      .fold(
        err => Msg.PMsg (PokemonMessage.SuggestionFailed(err)),
        pok => Msg.PMsg (PokemonMessage.SuggestionSucceeded (pok))
      )       
  }

  /**
    * Store a function that transform a server error into a message
    */
  private val onSuggestError: HttpError => Msg =
    e => Msg.PMsg (PokemonMessage.SuggestionFailed (e.toString))


  /**
    * Decode an information retreive
    */
  def fromSuggestHttpResponse: tyrian.http.Decoder[Msg] = {
    tyrian.http.Decoder[Msg](onSuggestResponse, onSuggestError)
  }

  /**
    * Call API to make a name suggestion
    */
  def suggest(name: String): Cmd[IO, Msg] = {
    Http.send(
      Request.get(s"/api/pokemon-lists/${name}"),
      PokemonApi.fromSuggestHttpResponse
    )
  }

}
