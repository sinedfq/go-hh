import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {runInNewContext} from 'node:vm'
const context={}
runInNewContext(readFileSync(new URL('../../public/splash.js',import.meta.url),'utf8'),context)
const {frame,trajectories,lineWindow}=context.TrudomixSplashMath
test('HTML contour, orb and impact finish once; reduced motion skips the sequence',()=>{
    assert.ok(frame(.1).trace>0)
    assert.equal(frame(0).trace,0)
    assert.ok(frame(1).trace>0&&frame(1).trace<1)
    assert.equal(frame(1.5).fill,0)
    assert.equal(frame(1.1).orbTrace,0)
    assert.ok(frame(1.5).orbTrace>0)
    assert.equal(frame(1.5).orb,0)
    assert.equal(frame(2.6).wordmark,0)
    assert.ok(frame(2.5).flash>0)
    assert.ok(frame(2.5).ringAlpha>0)
    assert.equal(frame(3.7).done,true)
    for(const t of [3.7,20]){assert.equal(frame(t).wordmark,1);assert.equal(frame(t).flash,0);assert.equal(frame(t).ringAlpha,0);assert.equal(frame(t).scale,1)}
    assert.equal(frame(0,true).wordmark,1);assert.equal(frame(0,true).done,true)
})

test('twelve curved paths start beyond four viewport corners and converge at center',()=>{
    for(const [w,h] of [[360,800],[430,932],[768,1024],[1024,768],[1280,900],[1920,1080]]){
        const lines=trajectories(w,h);assert.equal(lines.length,12)
        const corners=[0,0,0,0]
        for(const {points} of lines){
            const a=points[0],b=points.at(-1)
            assert.ok(a.x<0||a.x>w);assert.ok(a.y<0||a.y>h)
            corners[(a.y>h?2:0)+(a.x>w?1:0)]++
            assert.equal(b.x,w/2);assert.equal(b.y,h/2)
            assert.ok(Math.abs(a.x-(a.x<0?0:w))<48)
            assert.ok(Math.abs(a.y-(a.y<0?0:h))<48)
            const dx=b.x-a.x,dy=b.y-a.y,length=Math.hypot(dx,dy)
            let forward=-Infinity,maxSwing=0
            for(const point of points){
                const progress=((point.x-a.x)*dx+(point.y-a.y)*dy)/length
                assert.ok(progress>=forward-1e-7)
                forward=progress
                maxSwing=Math.max(maxSwing,Math.abs((point.x-a.x)*dy-(point.y-a.y)*dx)/length)
            }
            assert.ok(maxSwing>Math.min(w,h)*.1)

        }
        assert.deepEqual(corners,[3,3,3,3])
    }
})

test('light threads have a delayed following tail, shrink and disappear by the end of the line stage',()=>{
    for(const {delay} of trajectories(430,932)){
        let previousHead=0,previousTail=0,previousSpan=1
        for(let t=0;t<=3.31;t+=.01){
            const {headProgress:head,tailProgress:tail}=lineWindow(t,delay)
            assert.ok(head>=previousHead&&tail>=previousTail&&tail<=head)
            if(t>delay+1.7){assert.ok(head-tail<=previousSpan+1e-10);previousSpan=head-tail}
            previousHead=head;previousTail=tail
        }
        assert.ok(lineWindow(delay+1.3,delay).headProgress-lineWindow(delay+1.3,delay).tailProgress>.55)
        assert.equal(lineWindow(delay+.1,delay).tailProgress,0)
        assert.ok(lineWindow(delay+1.8,delay).tailProgress>0)
        assert.equal(lineWindow(2.8,delay).headProgress,1)
        assert.equal(lineWindow(2.8,delay).tailProgress,1)
        assert.equal(lineWindow(2.8,delay).alpha,0)
    }
    assert.equal(frame(2.8).lines,0)
})

test('random seed changes broad flights without changing them between frames or resizes',()=>{
    const first=trajectories(430,932,123),same=trajectories(430,932,123),another=trajectories(430,932,321)
    assert.equal(JSON.stringify(first),JSON.stringify(same))
    assert.notEqual(JSON.stringify(first),JSON.stringify(another))
})
