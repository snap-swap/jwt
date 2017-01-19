package io.igl.jwt.claims

import spray.json.{JsNumber, JsValue}

object IssuedAt {
  def apply(value: Long): Iat = {
    Iat(value)
  }
}

case class Iat(value: Long) extends ClaimValue {
  override val field: ClaimField = Iat
  override val jsValue: JsValue = JsNumber(value)
}

object Iat extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsNumber(v) =>
      apply(v.toLong)
  }

  override val name = "iat"
}