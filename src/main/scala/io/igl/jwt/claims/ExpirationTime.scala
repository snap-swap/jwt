package io.igl.jwt.claims

import spray.json.{JsNumber, JsValue}

object ExpirationTime {
  val field: ClaimField = Exp

  def apply(value: Long): Exp = {
    Exp(value)
  }
}

case class Exp(value: Long) extends ClaimValue {
  override val field: ClaimField = Exp
  override val jsValue: JsValue = JsNumber(value)
}

case object Exp extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsNumber(v) =>
      apply(v.toLong)
  }

  override val name = "exp"
}
