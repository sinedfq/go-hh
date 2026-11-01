package com.example.trudomix
import com.example.trudomix.data.models.PhoneOtpConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
class OtpConfigTest{
 @Test fun legacyServerMustNotBeMistakenForFourDigitProtocol(){
  val json=Json{ignoreUnknownKeys=true}
  assertEquals(0,json.decodeFromString<PhoneOtpConfig>("""{"enabled":true,"delivery":"firebase"}""").code_length)
  assertEquals(4,json.decodeFromString<PhoneOtpConfig>("""{"enabled":true,"delivery":"firebase","code_length":4}""").code_length)
 }
}
