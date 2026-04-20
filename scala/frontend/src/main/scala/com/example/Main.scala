package com.example

import cats.effect.IO

import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.*
import org.scalajs.dom

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model] {

  // No route, single page app
  def router: Location => Msg = Routing.none(Msg.NoOp)

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ======================================          INIT          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    // On home page, if the jwt is defined we are already logged, otherwise we must login
    val token = Option (dom.window.sessionStorage.getItem("jwt"))
    val name = Option (dom.window.sessionStorage.getItem("username"))

    // Home page workflow (if logged -> PokemonSearch else Login)
    val page = (token, name) match {
      case (Some (token), Some (name)) => {
        Page.PokemonSearch (PokemonSearchForm ())
      }
      case _ => {
        Page.Login (LoginForm ())
      }
    }

    (Model (page, token, name), Cmd.None)
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =====================================          UPDATE          =====================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Slot emitted when something changed on the page
    * @returns:
    *    (0): the new page model to change the view if needed
    *    (1): the command to run to launch back api commands if needed (Or Cmd.None)
    */
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = 
    case Msg.LMsg (log) => updateLogin (log, model)
    case Msg.SMsg (sub) => updateSub (sub, model)
    case Msg.PMsg (pok) => updatePok (pok, model)
    case _ => { // NoOp
      (model, Cmd.None)
    }

  /**
    * A Login message was received
    */
  def updateLogin(log: LoginMessage, model: Model): (Model, Cmd[IO, Msg]) = 
    log match {
      case LoginMessage.UsernameChanged(v) => // filling username field
        val form = extractLoginForm (model)
        (model.copy(page = Page.Login (form.copy (username = v))), Cmd.None)

      case LoginMessage.PasswordChanged(v) => // filling password field
        val form = extractLoginForm (model)
        (model.copy(page = Page.Login (form.copy (password = v))), Cmd.None)
        
      case LoginMessage.Succeeded(name, token) =>
        dom.console.log(s"Success! ${name}, ${token}")

        // Set the data into the page, to retreive them and keep the user logged through the session
        // Or localStorage to survive tab close
        dom.window.sessionStorage.setItem("jwt", token)
        dom.window.sessionStorage.setItem("username", name)

        // Redirect to the PokemonSearch page
        (Model (Page.PokemonSearch (PokemonSearchForm ("", None)), Some (token), Some (name)), Cmd.None)

      case LoginMessage.Failed(err) =>
        dom.console.log(s"Error ! ${err}")
        val form = extractLoginForm (model)

        // Redirect to clean login page with failure message
        (Model (Page.Login (LoginForm (username = form.username, failed = true)), None, None), Cmd.None)        

      case LoginMessage.SubmitForm =>
        // Just a console log test
        dom.console.log ("Submitted")

        model.page match {
          case Page.Login (form) =>
            // call to the backend API
            val cmd = LoginApi.login(form.username, form.password)

            // Stay on the page for the moment
            (model, cmd)
          case _ => // ???
            // Form not found? 
            (Model (Page.Login (LoginForm (failed = true)), None, None), Cmd.None)           
        }
    }

  /**
    * A subscribe message was received
    */
  def updateSub(sub: SubscribeMessage, model: Model): (Model, Cmd[IO, Msg]) =
    sub match {
      // TODO!!
      case _ => (model, Cmd.None)
    }

  /**
    * A pokemon message was received
    */
  def updatePok(pok: PokemonMessage, model: Model): (Model, Cmd[IO, Msg]) =
    pok match {
      case PokemonMessage.SubmitLogout =>
        // Clean credentials
        dom.window.sessionStorage.removeItem("jwt")
        dom.window.sessionStorage.removeItem("username")

        // And go back to login page
        (Model (Page.Login (LoginForm ()), None, None), Cmd.None)

      case PokemonMessage.NameChanged(name) =>
        dom.console.log(s"Changes! ${name}")        
        val cmd = if name != "" then
          PokemonApi.suggest(name)
        else
          Cmd.None

        val form = extractPokForm (model)
        (model.copy(page = Page.PokemonSearch (form.copy (name = name, suggestions = List ()))), cmd)

      case PokemonMessage.SuggestionSelected(name) =>
        val form = extractPokForm (model)
        (model.copy(page = Page.PokemonSearch (form.copy (name = name, suggestions = List ()))), Cmd.None)

      case PokemonMessage.InfoSucceeded(pokemon) =>        
        (model.copy (page = Page.PokemonSearch (PokemonSearchForm (pokemon = Some (pokemon)))), Cmd.None)

      case PokemonMessage.InfoFailed(_) =>
        (model.copy (page = Page.PokemonSearch (PokemonSearchForm (failed = true))), Cmd.None)

      case PokemonMessage.SuggestionSucceeded(lst) =>
        val form = extractPokForm (model)
        (model.copy(page = Page.PokemonSearch (form.copy (suggestions = lst))), Cmd.None)        

      case PokemonMessage.SuggestionFailed(_) =>
        val form = extractPokForm (model)
        (model.copy(page = Page.PokemonSearch (form.copy (suggestions = List ()))), Cmd.None)        

      case PokemonMessage.SubmitForm =>
        model.page match {
          case Page.PokemonSearch (form) =>
            // Call the backend api
            val cmd = PokemonApi.search(form.name)

            // Stay on the page
            (model, cmd)

          case _ => // ???

            // Form not found
            (model.copy (page = Page.PokemonSearch (PokemonSearchForm (failed = true))), Cmd.None)
        }
    }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * =====================================          VIEWS          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Construct the page content depending on the page
    */
  def view(model: Model): Html[Msg] = {
    dom.console.log (s"Page : ${model.page}, model : ${model}")
    (model.page, model.username) match {
      case (Page.Login (form), _) => loginView (form)
      case (Page.Subscribe (form), _) => subscribeView (form)
      case (Page.PokemonSearch (form), Some (name)) => pokemonView (form, name)
      case _ => { // ???
        errorView ()
      }
    }
  }

  /**
    * Construct the page for login 
    */
  def loginView(lform: LoginForm): Html[Msg] = {
    div(
      h2("Login"),
      div (
        if lform.failed then
          List(p (style(CSS.color("red")))("wrong credentials"))
        else
          List ()
      ),
      form (
        id := "form",
        onSubmit(Msg.LMsg (LoginMessage.SubmitForm))
      )(
        input(
          placeholder:= "Username",
          value := lform.username,
          onInput(s => Msg.LMsg (LoginMessage.UsernameChanged (s)))
        ),
        input(
          placeholder := "Password",
          `type` := "password",
          value := lform.password,
          onInput(s => Msg.LMsg (LoginMessage.PasswordChanged (s)))                    
        ),
        button(`type` := "submit",
          onClick(Msg.LMsg (LoginMessage.SubmitForm))
        )("Login")
      )            
    )
  }

  /**
    * Construct the page for subscription
    */
  def subscribeView(form: SubscribeForm): Html[Msg] = {
    div ()
  }

  /**
    * Construct the page for pokemon view
    */
  def pokemonView(pform: PokemonSearchForm, username: String): Html[Msg] = {
    div(
      h2(s"Welcome! $username"),
      button (onClick(Msg.PMsg (PokemonMessage.SubmitLogout)))("Logout"),
      div (
        if pform.failed then
          List(p (style(CSS.color("red")))("Not found"))
        else
          List ()
      ),
      form(
        id := "form",
        onSubmit(Msg.PMsg (PokemonMessage.SubmitForm))
      )(
        input (
          placeholder := "Pokemon Name",
          value := pform.name,
          list := "suggestions",
          onInput (s => Msg.PMsg (PokemonMessage.NameChanged (s)))
        ),
        pokemonSuggestionView (pform.suggestions),
        button (`type` := "submit",
          onClick (Msg.PMsg (PokemonMessage.SubmitForm))
        )("Search")
      ),      
      // display the pokemon
      pokemonInfoView (pform.pokemon, pform.failed)      
    )    
  }

  /**
    * Display the pokemon information
    */
  def pokemonInfoView(pokemon: Option[Pokemon], failed: Boolean): Html[Msg] =
    (pokemon, failed) match {
      case (Some (pok), false) =>
        div (
          hr (),
          h2 (s"Pokemon : ${pok.name}"),
          p (s"Id : ${pok.id}"),
          img (
            src := pok.sprite,
            alt := pok.name
          ),
          if pok.types.nonEmpty then {
            div (
              h3("types"),
              ul (
                pok.types.map { name => li(name) }
              )
            )
          } else {
            div ()
          }          
        )
      case _ => div ()      
    }

  def pokemonSuggestionView(lst: List[String]): Html[Msg] =
    datalist (
      id := "suggestions"
    )(
      lst.map { name =>
        option(
          onClick( Msg.PMsg(PokemonMessage.SuggestionSelected(name)))
        )(name)
      }
    )

  /**
    * error page (should not happen)
    */
  def errorView(): Html[Msg] = {
    div(
      h1("ERROR!!")
    )
  }

  /*!
  * ====================================================================================================
  * ====================================================================================================
  * ======================================          MISC          ======================================
  * ====================================================================================================
  * ====================================================================================================
  */

  /**
    * Unused we don't subscribe to external events
    */
  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None

  /**
    * Extract the login form from the model
    */
  def extractLoginForm(model: Model): LoginForm =
    model.page match {
      case Page.Login (form) => form
      case _ => {
        LoginForm ("", "", false)
      }
    }

  /**
    * Extract the subscribe form from the model
    */
  def extractSubForm(model: Model): SubscribeForm =
    model.page match {
      case Page.Subscribe (form) => form
      case _ => {
        SubscribeForm ("", "", false)
      }
    }

  /**
    * Extract the subscribe form from the model
    */
  def extractPokForm(model: Model): PokemonSearchForm =
    model.page match {
      case Page.PokemonSearch (form) => form
      case _ => {
        PokemonSearchForm ("", None)
      }
    }

}
