package com.example

import tyrian.*
import tyrian.http.*

import cats.effect.IO
import cats.syntax.either.*

import io.circe.HCursor
import io.circe.Json
import io.circe.parser.*


object LoginApi {

  /**
    * Decode the result of the server side
    */
  def jsonDecode(cursor: HCursor): Either[String, LoginMessage.Succeeded] = {
    for {
      token <- cursor.get[String]("jwt").leftMap(_.message)
      name <- cursor.get[String]("name").leftMap(_.message)
    } yield LoginMessage.Succeeded(name, token)
  }

  /**
    * Store a function that transform a server response into a message
    */
  private val onResponse: Response => Msg = { response =>
    parse(response.body)
      .leftMap(_.message)
      .flatMap(j => jsonDecode(j.hcursor))
      .fold(
        err => Msg.LMsg (LoginMessage.Failed(err)),
        msg => Msg.LMsg (msg)
      )       
  }

  /**
    * Store a function that transform a server error into a message
    */
  private val onError: HttpError => Msg =
    e => Msg.LMsg (LoginMessage.Failed (e.toString))


  /**
    * Decode the server response
    */
  def fromHttpResponse: tyrian.http.Decoder[Msg] = {
    tyrian.http.Decoder[Msg](onResponse, onError)
  }

  /** 
    * Call a api login and return the callback command 
    */
  def login(username: String, password: String): Cmd[IO, Msg] = {    
    Http.send(
      Request.post("/api/login", Body.json(
        Json.obj(
          "login" -> Json.fromString(username),
          "password" -> Json.fromString(password) // should bcrypt here, instead of doing it server side
        ).noSpaces
      )), LoginApi.fromHttpResponse)
    }
}
