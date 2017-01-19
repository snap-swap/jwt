package io.igl.jwt.claims

import spray.json.JsString

object Subject {
  def apply(subject: String): Sub = {
    Sub(subject)
  }
}

case class Sub(value: String) extends ClaimValue {
  override val jsValue = JsString(value)
  override val field: ClaimField = Sub
}

object Sub extends ClaimField {
  override protected def attemptApply: JsonToClaim = {
    case JsString(v) =>
      apply(v)
  }

  override val name = "sub"
}