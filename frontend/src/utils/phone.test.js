import test from 'node:test'
import assert from 'node:assert/strict'
import {normalizePhone,formatPhone} from './phone.js'
test('Russian phone accepts pasted national, local and formatted numbers',()=>{
    for(const value of ['89991234567','9991234567','+7 (999) 123-45-67']){
        assert.equal(normalizePhone(value),'+79991234567')
        assert.equal(formatPhone(value),'+7 (999) 123-45-67')
    }
    assert.equal(formatPhone('+7'),'+7')
    assert.equal(formatPhone('+7999'),'+7 (999')
    assert.equal(formatPhone('+442071234567'),'+442071234567')
    assert.equal(normalizePhone(''), '')
})
