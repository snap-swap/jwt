package io.igl.jwt.claims

import io.igl.jwt.JwtValue

trait ClaimValue extends JwtValue {
  val field: ClaimField

  def and(nextClaim: ClaimValue): Seq[ClaimValue] = {
    Seq(this, nextClaim)
  }
}