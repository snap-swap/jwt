package io.igl.jwt.claims

import spray.json.{JsNumber, JsValue}

object NotBefore {
  val field: ClaimField = Nbf

  def apply(value: Long): Nbf = {
    Nbf(value)
  }
}

case class Nbf(value: Long) extends ClaimValue {
  override val field: ClaimField = Nbf
  override val jsValue: JsValue = JsNumber(value)
}

object Nbf extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsNumber(v) =>
      apply(v.toLong)
  }

  override val name = "nbf"
}