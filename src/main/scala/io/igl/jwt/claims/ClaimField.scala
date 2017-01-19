package io.igl.jwt.claims

import io.igl.jwt.JwtField
import spray.json.JsValue

trait ClaimField extends JwtField {
  type JsonToClaim = PartialFunction[JsValue, ClaimValue]

  def attemptApply(value: JsValue): Option[ClaimValue] = jsonToOptionClaim(value)

  protected def attemptApply: JsonToClaim

  private[this] def jsonToOptionClaim: PartialFunction[JsValue, Option[ClaimValue]] = {
    attemptApply.andThen(Some(_)) orElse defaultNone
  }

  private[this] val defaultNone: PartialFunction[JsValue, Option[ClaimValue]] = {
    case _: JsValue =>
      None
  }
}