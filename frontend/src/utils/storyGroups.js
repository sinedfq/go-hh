export function groupStories(items,now){
    const groups=new Map()
    for(const item of items){
        if(!(Date.parse(item.expires_at)>now))continue
        if(!groups.has(item.company_id))groups.set(item.company_id,{id:item.company_id,items:[]})
        groups.get(item.company_id).items.push(item)
    }
    return [...groups.values()].map(group=>({...group,items:group.items.sort((a,b)=>a.id-b.id)}))
}
export function nextStoryExpiry(items,now){
    let next=Infinity
    for(const item of items){const expires=Date.parse(item.expires_at);if(expires>now&&expires<next)next=expires}
    return Number.isFinite(next)?next:null
}
