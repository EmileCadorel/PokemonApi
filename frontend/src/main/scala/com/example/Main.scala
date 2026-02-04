package com.example

import cats.effect.IO
import cats.syntax.either.*

import tyrian.Html.*
import tyrian.*
import tyrian.http.*

import io.circe.HCursor
import scala.scalajs.js.annotation.*
import io.circe.Json
import io.circe.parser.*

import org.scalajs.dom

/*!
* ====================================================================================================
* ====================================================================================================
* =================================          MODEL/MESSAGE          ==================================
* ====================================================================================================
* ====================================================================================================
*/

final case class Model (  
  token: Option[String],
  username: Option[String],
  loginFailed: Boolean,
  loginForm: LoginForm
)
final case class LoginForm (username: String, password: String)

enum Msg:
  case UsernameChanged(value: String)
  case PasswordChanged(value: String)
  case SubmitLogin
  case SubmitLogout
  case LoginSucceeded(name: String, token: String)
  case LoginFailed(error: String)  
  case NoOp

/*!
* ====================================================================================================
* ====================================================================================================
* =====================================          LOGIN          ======================================
* ====================================================================================================
* ====================================================================================================
*/

object LoginApi {
  def jsonDecode(cursor: HCursor): Either[String, Msg.LoginSucceeded] = {
    for {
      token <- cursor.get[String]("jwt").leftMap(_.message)
      name <- cursor.get[String]("name").leftMap(_.message)
    } yield Msg.LoginSucceeded(name, token)
  }
    
  private val onResponse: Response => Msg = { response =>
    parse(response.body)
      .leftMap(_.message)
      .flatMap(j => jsonDecode(j.hcursor))
      .fold(
        err => Msg.LoginFailed(err),
        res => Msg.LoginSucceeded(res.name, res.token)
      )       
  }

  private val onError: HttpError => Msg =
    e => Msg.LoginFailed(e.toString)

  def fromHttpResponse: tyrian.http.Decoder[Msg] = {
    tyrian.http.Decoder[Msg](onResponse, onError)
  }

  def login(username: String, password: String): Cmd[IO, Msg] = {    
    Http.send(
      Request.post("http://localhost:8080/api/login", Body.json(
        Json.obj(
          "login" -> Json.fromString(username),
          "password" -> Json.fromString(password)
        ).noSpaces
      )), LoginApi.fromHttpResponse)
    }
}


/*!
* ====================================================================================================
* ====================================================================================================
* ======================================          APP          =======================================
* ====================================================================================================
* ====================================================================================================
*/

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model] {

  def router: Location => Msg = Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    // On home page, if the jwt is defined we are already logged, otherwise we must login
    val token = Option (dom.window.sessionStorage.getItem("jwt"))
    val name = Option (dom.window.sessionStorage.getItem("username"))

    (Model (token, name, false, LoginForm ("", "")), Cmd.None)
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {    
    case Msg.UsernameChanged(v) =>
      (model.copy(loginForm = model.loginForm.copy(username = v)), Cmd.None)

    case Msg.PasswordChanged(v) =>
      (model.copy(loginForm = model.loginForm.copy(password = v)), Cmd.None)

    case Msg.SubmitLogin =>
      dom.console.log ("Submitted")
      // call to the backend API
      val cmd = LoginApi.login(model.loginForm.username, model.loginForm.password)
      (model, cmd)

    case Msg.LoginSucceeded(name, token) =>
      dom.console.log(s"Success! ${name}, ${token}")

      // Or localStorage to survive tab close
      dom.window.sessionStorage.setItem("jwt", token)
      dom.window.sessionStorage.setItem("username", name)

      (model.copy(token = Some(token), username = Some (name), loginFailed = false), Cmd.None)      

    case Msg.LoginFailed(err) =>
      dom.console.log(s"Error ! ${err}")      
      (model.copy(loginFailed = true), Cmd.None)

    case Msg.SubmitLogout =>

      // Or localStorage to survive tab close
      dom.window.sessionStorage.removeItem("jwt")
      dom.window.sessionStorage.removeItem("username")

      // Back to login page
      (model.copy (token = None, username = None, loginFailed = false, loginForm = LoginForm ("", "")), Cmd.None)

    case Msg.NoOp =>
      (model, Cmd.None)
  }

  def view(model: Model): Html[Msg] = {
    (model.token, model.username) match {
      case (Some (token), Some (name)) =>
        homeView (model, name, token)
      case _ =>        
        loginFormView (model)
    }
  }

  def loginFormView(model: Model): Html[Msg] = {
    div(
      h2("Login"),
      div (
        if model.loginFailed then
          List(p (style(CSS.color("red")))("wrong credentials"))
        else
          List ()
      ),
      form (
        id := "form",
        onSubmit(Msg.SubmitLogin)
      )(
        input(
          placeholder:= "Username",
          value := model.loginForm.username,
          onInput(Msg.UsernameChanged.apply)
        ),
        input(
          placeholder := "Password",
          `type` := "password",
          value := model.loginForm.password,
          onInput(Msg.PasswordChanged.apply)
        ),
        button(`type` := "submit",
          onClick(Msg.SubmitLogin)
        )("Login")
      )            
    )
  }

  def homeView (model: Model, name: String, token: String): Html[Msg] = {
    div(
      h2(s"Welcome! $name"),
      button (onClick(Msg.SubmitLogout))("Logout"),
      p(s"Your token: $token"),            
    )
  }

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None

}
