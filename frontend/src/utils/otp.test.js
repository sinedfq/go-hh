import test from 'node:test'
import assert from 'node:assert/strict'
import {otpDigits,OTP_LENGTH} from './otp.js'
test('OTP keeps leading zeroes, accepts paste and limits ASCII digits to four',()=>{
 assert.equal(OTP_LENGTH,4)
 assert.equal(otpDigits('0 1-23 45'),'0123')
 assert.equal(otpDigits('１２３４abc'),'')
 assert.equal(otpDigits(''),'')
})
