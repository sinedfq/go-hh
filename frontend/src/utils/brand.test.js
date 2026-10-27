import test from 'node:test'
import assert from 'node:assert/strict'
import { brandImage, readBrand } from './brand.js'

test('loader uses the selected design and automatic theme without copying demo settings',()=>{
    globalThis.localStorage={getItem:key=>key==='trudomix-loader-settings'?'{"variant":"2","theme":"dark"}':null}
    assert.deepEqual(readBrand(),{design:10,color:'auto'})
    assert.equal(brandImage({design:10,color:'auto'},'light'),'/brand/wave-light.png')
    assert.equal(brandImage({design:10,color:'auto'},'dark'),'/brand/wave-night.png')
})
test('explicit background and corrupt settings have predictable results',()=>{
    assert.equal(brandImage({design:10,color:'white'},'dark'),'/brand/wave-light.png')
    globalThis.localStorage={getItem:()=>'{bad'}
    assert.deepEqual(readBrand(),{design:10,color:'auto'})
    globalThis.localStorage={getItem:()=>'{"design":99,"color":"../secret"}'}
    assert.deepEqual(readBrand(),{design:10,color:'auto'})
    globalThis.localStorage={getItem:()=>'{"design":3,"color":"mint"}'}
    assert.deepEqual(readBrand(),{design:10,color:'auto'})
    globalThis.localStorage={getItem:()=>'{"design":10,"color":"black"}'}
    assert.deepEqual(readBrand(),{design:10,color:'black'})
})
