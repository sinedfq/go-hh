import test from 'node:test'
import assert from 'node:assert/strict'
import { assertSourceEncoding } from '../../scripts/source-encoding.mjs'

test('source guard preserves Cyrillic and allows ordinary JS question-mark syntax', () => {
    const source = 'const label = "Личное"; const name = user?.name ?? "Профиль"; const query = "?q=работа"'
    assert.equal(assertSourceEncoding(Buffer.from(source, 'utf8'), 'menu.jsx'), source)
})

test('source guard rejects bytes damaged by a non-UTF-8 encoding', () => {
    assert.throws(() => assertSourceEncoding(Uint8Array.from([0xd0, 0x28]), 'menu.jsx'), /invalid UTF-8/)
    assert.throws(() => assertSourceEncoding(Buffer.from(String.fromCharCode(0xfffd)), 'menu.jsx'), /replacement character/)
})

test('source guard catches destructive question-mark substitution even in valid UTF-8', () => {
    const damaged = '<span>' + '?'.repeat(6) + '</span>'
    assert.throws(() => assertSourceEncoding(Buffer.from(damaged), 'menu.jsx'), /question-mark placeholders/)
})
