import test from 'node:test'
import assert from 'node:assert/strict'
import { emptySearchFilters, readSearchFilters, searchParams, restoreSavedFilters } from './searchFilters.js'

test('all mobile filters survive browser URL reload, including office format', () => {
    const filters = { ...emptySearchFilters(),query:'Android', location:'Новосибирск', company:'A & B', experience:'Junior|1-3 года', remote:false, skills:['Kotlin','Go'], hasCoordinates:true, sort:'popular',salaryMin:100000,salaryMax:200000,currency:'RUB',salaryOnly:true,employment:'full_time',schedule:'flexible' }
    assert.deepEqual(readSearchFilters(searchParams(filters).toString()), filters)
    assert.equal(searchParams(filters, true).get('company'), 'A & B')
    assert.equal(searchParams(filters).has('company'), false, 'company is reserved for the company details route')
})
test('reset removes every filter and invalid sorting falls back to newest', () => {
    assert.equal(searchParams(emptySearchFilters()).toString(), '')
    assert.deepEqual(readSearchFilters('sort=unknown&company=123'), emptySearchFilters())
})
test('saved backend searches restore office, salary, skills and sorting',()=>{
    assert.deepEqual(restoreSavedFilters({query:'Go',company:'Team',remote:false,skills:['Go','SQL'],salary_min:100000,salary_max:200000,currency:'RUB',employment:'full_time',schedule:'flexible',salary_only:true,has_coordinates:true,sort:'popular'}),{...emptySearchFilters(),query:'Go',company:'Team',remote:false,skills:['Go','SQL'],salaryMin:100000,salaryMax:200000,currency:'RUB',employment:'full_time',schedule:'flexible',salaryOnly:true,hasCoordinates:true,sort:'popular'})
})
