package io.igl.jwt.claims

object ClaimSeq {

  implicit class RichClaimSeq(value: Seq[ClaimValue]) {
    def and(nextClaim: ClaimValue): Seq[ClaimValue] = {
      value :+ nextClaim
    }
  }

}