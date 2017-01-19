package io.igl.jwt.claims

import spray.json.{JsString, JsValue}

object Issuer {
  def apply(value: String): Iss = {
    new Iss(value)
  }
}

case class Iss(value: String) extends ClaimValue {
  override val field: ClaimField = Iss
  override val jsValue: JsValue = JsString(value)
}

object Iss extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsString(v) =>
      apply(v)
  }

  override val name = "iss"
}