package com.example.trudomix

import com.example.trudomix.ui.screens.auth.otpDigits
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpDigitsTest{
 @Test fun pasteRetainsLeadingZeroesAndCapsFourAsciiDigits(){
  assertEquals("0123",otpDigits("0 1-23 45"))
  assertEquals("",otpDigits("１２３４abc"))
  assertEquals("",otpDigits(""))
 }
}
