import test from 'node:test'
import assert from 'node:assert/strict'
import {themeWaveOrigin} from './themeWave.js'

test('theme wave starts at the button center and covers every viewport corner',()=>{
    for(const [width,height] of [[360,800],[1280,900],[1920,1080]]){
        const rect={left:width-100,top:20,width:42,height:42}
        const {x,y,radius}=themeWaveOrigin(rect,width,height)
        assert.equal(x,rect.left+21);assert.equal(y,41)
        for(const [cx,cy] of [[0,0],[width,0],[0,height],[width,height]])assert.ok(Math.hypot(cx-x,cy-y)<=radius+1e-9)
    }
})
