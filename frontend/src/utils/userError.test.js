import test from 'node:test'
import assert from 'node:assert/strict'
import { userError } from './userError.js'

test('incorrect credentials are readable and do not expose HTTP or server HTML',()=>{
    const message=userError({response:{status:403,data:{error:'<html>internal error</html>'}}},{login:true})
    assert.equal(message,'Неверная почта или пароль.')
})
test('server errors and missing connection have separate messages',()=>{
    assert.match(userError({response:{status:500}}),/временно недоступен/)
    assert.match(userError(new Error('private internal address')),/Нет соединения/)
})
test('registration conflict and photo size are actionable',()=>{
    assert.match(userError({response:{status:409}}),/уже существует/)
    assert.match(userError({response:{status:413}}),/меньшего размера/)
})
