// Use only with a disposable database: creates a synthetic employer and company.
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'

const base = (process.argv[2] || 'http://127.0.0.1:3000').replace(/\/$/,'')
async function request(path,options={}) {
    const response=await fetch(`${base}${path}`,options)
    assert.ok(response.ok,`${options.method||'GET'} ${path}: ${response.status}`)
    return response
}
assert.equal((await (await request('/health')).json()).status,'ok')
assert.match(await (await request('/')).text(),/id="root"/)
assert.match(await (await request('/vacancies/1')).text(),/id="root"/)
assert.ok(Array.isArray((await (await request('/api/vacancies/search')).json()).vacancies))
const email=`docker-${randomUUID()}@example.test`
const auth=await (await request('/api/auth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password:randomUUID(),role:'employer'})})).json()
const headers={Authorization:`Bearer ${auth.token}`,'Content-Type':'application/json'}
const company=await (await request('/api/companies',{method:'POST',headers,body:JSON.stringify({name:`Docker smoke ${randomUUID()}`,city:'Новосибирск',description:'Изолированная проверка контейнеров'})})).json()
assert.ok(company.id)
const photo=new FormData()
photo.append('photo',new Blob([await readFile(new URL('../uploads/demo_north_logo.png',import.meta.url))],{type:'image/png'}),'logo.png')
const uploaded=await (await request('/api/my-company/logo',{method:'POST',headers:{Authorization:headers.Authorization},body:photo})).json()
assert.match(uploaded.logo_url,/^\/uploads\//)
assert.match((await request(uploaded.logo_url)).headers.get('content-type'),/image\//)
assert.equal((await (await request('/api/my-company',{headers})).json()).logo_url,uploaded.logo_url)
console.log('Docker smoke passed: SPA, API proxy, migrations, authentication, company logo and upload storage.')
