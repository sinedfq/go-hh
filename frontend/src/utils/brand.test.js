import test from 'node:test'
import assert from 'node:assert/strict'
import { brandImage, readBrand } from './brand.js'

test('loader uses the selected design and automatic theme without copying demo settings',()=>{
    globalThis.localStorage={getItem:key=>key==='trudomix-loader-settings'?'{"variant":"2","theme":"dark"}':null}
    assert.deepEqual(readBrand(),{design:6,color:'auto'})
    assert.equal(brandImage({design:6,color:'auto'},'light'),'/loader-icons/icon_06_white.png')
    assert.equal(brandImage({design:6,color:'auto'},'dark'),'/loader-icons/icon_06_black.png')
})
test('explicit background and corrupt settings have predictable results',()=>{
    assert.equal(brandImage({design:3,color:'mint'},'dark'),'/loader-icons/icon_03_mint.png')
    globalThis.localStorage={getItem:()=>'{bad'}
    assert.deepEqual(readBrand(),{design:6,color:'auto'})
    globalThis.localStorage={getItem:()=>'{"design":99,"color":"../secret"}'}
    assert.deepEqual(readBrand(),{design:6,color:'auto'})
})
