package com.example.trudomix.data.models

import kotlinx.serialization.Serializable

@Serializable data class PhoneOtpConfig(val enabled:Boolean=false,val test_mode:Boolean=false,val delivery:String="off")
@Serializable data class PhoneOtpRequest(val phone:String,val device_key:String)
@Serializable data class PhoneOtpChallenge(val challenge_id:String,val expires_in:Int,val retry_after:Int,val development_code:String?=null)
@Serializable data class PhoneOtpVerify(val challenge_id:String,val code:String,val device_key:String)
@Serializable data class PhoneOtpReceipt(val verified:Boolean,val verification_receipt:String,val expires_in:Int)
@Serializable data class PhoneLoginRequest(val phone:String,val phone_receipt:String,val device_key:String)
