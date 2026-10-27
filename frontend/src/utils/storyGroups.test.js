import test from 'node:test'
import assert from 'node:assert/strict'
import {groupStories,nextStoryExpiry} from './storyGroups.js'
test('stories expire at their deadline and remain grouped in publishing order',()=>{
    const items=[{id:3,company_id:1,expires_at:'2026-10-25T12:00:00Z'},{id:1,company_id:1,expires_at:'2026-10-25T12:00:00Z'},{id:2,company_id:2,expires_at:'2026-10-25T13:00:00Z'},{id:4,company_id:3,expires_at:'invalid'}]
    const noon=Date.parse('2026-10-25T12:00:00Z')
    assert.deepEqual(groupStories(items,noon-1).map(g=>g.items.map(i=>i.id)),[[1,3],[2]])
    assert.deepEqual(groupStories(items,noon).map(g=>g.id),[2])
    assert.equal(nextStoryExpiry(items,noon-1),noon)
    assert.equal(nextStoryExpiry(items,noon),noon+3600000)
    assert.equal(nextStoryExpiry(items,noon+3600000),null)
})
