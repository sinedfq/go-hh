import test from 'node:test'
import assert from 'node:assert/strict'
import {networkFailure} from './networkState.js'

test('network feedback distinguishes offline, unreachable backend, timeout, server and auth',()=>{
    assert.equal(networkFailure({},false).state,'OFFLINE')
    assert.equal(networkFailure({},true).state,'SERVER_UNAVAILABLE')
    assert.equal(networkFailure({code:'ECONNABORTED'},true).kind,'TIMEOUT')
    assert.equal(networkFailure({response:{status:503}},false).kind,'SERVER')
    assert.equal(networkFailure({response:{status:401}},true).kind,'AUTH')
    assert.equal(networkFailure({response:{status:404}},true),null)
    assert.equal(networkFailure({response:{status:422}},true),null)
    assert.equal(networkFailure({code:'ERR_CANCELED'},true),null)
})
