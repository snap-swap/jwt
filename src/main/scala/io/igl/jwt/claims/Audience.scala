package io.igl.jwt.claims

import spray.json.{JsArray, JsString, JsValue}
import spray.json.DefaultJsonProtocol._

object Audience {
  val field: ClaimField = Aud

  def apply(value: Either[String, Seq[String]]): Aud = {
    new Aud(value)
  }
}

case class Aud(value: Either[String, Seq[String]]) extends ClaimValue {
  override val field: ClaimField = Aud
  override val jsValue: JsValue = value match {
    case Left(single) =>
      JsString(single)
    case Right(many) =>
      JsArray(many.map(JsString(_)): _*)
  }
}

object Aud extends ClaimField {
  def apply(value: String): Aud = {
    Aud(Left(value))
  }

  def apply(value: Seq[String]): Aud = {
    Aud(Right(value))
  }

  override protected def attemptApply: JsonToClaim = {
    case JsString(v) =>
      Aud(v)
    case JsArray(v) =>
      Aud(v.map(_.convertTo[String]))
  }

  override val name = "aud"
}