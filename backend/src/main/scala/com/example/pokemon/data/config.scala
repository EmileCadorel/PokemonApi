package com.example.pokemon.data

import io.circe.Decoder
import io.circe.generic.semiauto.*

case class DBConfig (addr: String, port: Int, user: String, password: String, name: String)
case class AppConfig (db: DBConfig, addr: String, port: Int)
object AppConfig {
  given Decoder[DBConfig] = deriveDecoder
  given Decoder[AppConfig] = deriveDecoder
}
