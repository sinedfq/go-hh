import assert from 'node:assert/strict'
import {randomBytes} from 'node:crypto'

const base=(process.argv[2]||'http://localhost:3000').replace(/\/$/,'')
const phone=process.argv[3]||'+79990000001'
const device_key=randomBytes(32).toString('hex')
async function request(path,body){
    const response=await fetch(base+path,{method:body?'POST':'GET',headers:body?{'Content-Type':'application/json'}:undefined,body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(12000)})
    const result=await response.json()
    if(!response.ok)throw new Error(result.error||`HTTP ${response.status}`)
    return result
}
try{
    const config=await request('/api/phone-verification/config')
    assert.ok(config.enabled&&config.test_mode,'This check requires explicit development OTP mode; no real SMS will be sent')
    const challenge=await request('/api/phone-verification/request',{phone,device_key})
    assert.match(challenge.development_code,/^\d{6}$/)
    const verified=await request('/api/phone-verification/verify',{challenge_id:challenge.challenge_id,code:challenge.development_code,device_key})
    assert.ok(verified.verified&&verified.verification_receipt)
    const replay=await fetch(base+'/api/phone-verification/verify',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({challenge_id:challenge.challenge_id,code:challenge.development_code}),signal:AbortSignal.timeout(12000)})
    assert.equal(replay.status,400,'A verified code must not work twice')
    console.log(JSON.stringify({phone_verified:true,code_replay_blocked:true,receipt_expires_in:verified.expires_in,real_sms_sent:false}))
}catch(error){console.error(error.message);process.exitCode=1}
