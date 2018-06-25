package ocaps

import org.scalatest._

class RevokerSpec extends WordSpec with Matchers {

  "Revoker.compose" should {
    "work" in {
      val r1 = Revoker()
      val r2 = Revoker()
      val r1r2 = Revoker.compose(r1, r2)

      r1r2.revoke()
      r1.revoked should be(true)
      r2.revoked should be(true)
    }
  }

  "Revoker.pair" should {
    "work" in {
      val (thunk, revoker) = Revoker.tuple("derp")
      thunk() should equal("derp")
      revoker.revoke()
      assertThrows[RevokedException] {
        thunk()
      }
    }
  }


}
