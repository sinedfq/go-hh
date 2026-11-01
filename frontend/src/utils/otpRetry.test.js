import test from 'node:test'
import assert from 'node:assert/strict'
import {otpRetryAfter} from './otp.js'
test('OTP honors the server hourly cooldown instead of retrying every minute',()=>{
 assert.equal(otpRetryAfter('3500'),3500)
 assert.equal(otpRetryAfter('60'),60)
 assert.equal(otpRetryAfter(undefined),60)
 assert.equal(otpRetryAfter('-1'),60)
})
