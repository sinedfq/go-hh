import test from 'node:test'
import assert from 'node:assert/strict'
import { salaryLabel } from './vacancyTerms.js'
test('salary display distinguishes undisclosed, open-ended and full ranges',()=>{
    assert.equal(salaryLabel({}),'Зарплата не указана')
    assert.match(salaryLabel({salary_from:100000}),/^от 100\s000 ₽ \/ мес\. · на руки$/)
    assert.match(salaryLabel({salary_to:200000,currency:'USD',salary_gross:true}),/^до 200\s000 \$ \/ мес\. · до вычета налогов$/)
    assert.match(salaryLabel({salary_from:100000,salary_to:150000,currency:'EUR'}),/100\s000 – 150\s000 €/)
})
