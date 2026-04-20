package com.example


/*!
* ====================================================================================================
 * ====================================================================================================
 * =====================================          MODEL          ======================================
 * ====================================================================================================
 * ====================================================================================================
 */

final case class Model (
  page: Page,
  token: Option[String],
  username: Option[String]  
)

/**
* Three pages:
* Workflow =
*   1. On HOME : (if logged -> PokemonSearch else Login)
*   2. On LOGIN : (if LoginSuccess -> PokemonSearch else if LoginFailed -> Login else if subscribe.click -> Subscribe)
*   3. On PokemonSearch : (if Logout -> Login else if Search -> PokemonSearch)
*   4. On Subscribe: (if SubscribeSuccess -> HOME else Subscribe)
*/
enum Page:
  case Login (form: LoginForm)
  case Subscribe (form: SubscribeForm)
  case PokemonSearch (form: PokemonSearchForm)

final case class LoginForm (username: String = "", password: String = "", failed: Boolean = false)
final case class SubscribeForm (username: String = "", password: String = "", failed: Boolean = false)
final case class PokemonSearchForm (name: String = "", pokemon: Option[Pokemon] = None, suggestions: List[String] = List (), failed: Boolean = false)

final case class Pokemon(id : Int, name: String, weight: Int, sprite: String, types: List[String])

/*!
 * ====================================================================================================
 * ====================================================================================================
 * ====================================          MESSAGES          ====================================
 * ====================================================================================================
 * ====================================================================================================
 */
 
enum Msg:
  case LMsg (log: LoginMessage)
  case SMsg (sub: SubscribeMessage)
  case PMsg (pok: PokemonMessage)
  case NoOp

enum LoginMessage:  
  case UsernameChanged(value: String)
  case PasswordChanged(value: String)
  case Succeeded(name: String, token: String)
  case Failed(error: String)
  case SubmitForm

enum SubscribeMessage:  
  case UsernameChanged(value: String)
  case PasswordChanged(value: String)
  case Succeeded(name: String, token: String)
  case Failed(error: String)
  case SubmitForm

enum PokemonMessage:  
  case NameChanged(value: String)  
  case InfoSucceeded(pokemon: Pokemon)
  case InfoFailed(error: String)
  case SuggestionSelected(suggestion: String)
  case SuggestionSucceeded(lst: List[String])
  case SuggestionFailed(error: String)
  case SubmitForm
  case SubmitLogout
  
