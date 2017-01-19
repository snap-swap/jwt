package io.igl.jwt.claims

import spray.json.{JsString, JsValue}

object JwtId {
  def apply(value: String): Jti = {
    Jti(value)
  }
}

case class Jti(value: String) extends ClaimValue {
  override val field: ClaimField = Jti
  override val jsValue: JsValue = JsString(value)
}

object Jti extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsString(v) =>
      apply(v)
  }

  override val name = "jti"
}