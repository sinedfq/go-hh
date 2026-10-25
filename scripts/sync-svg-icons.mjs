// Import the supplied real vectors. EXACT_SVG embeds raster images instead.
import { readFileSync, writeFileSync, mkdirSync, copyFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root=fileURLToPath(new URL('../',import.meta.url))
const read=relative=>readFileSync(path.join(root,relative),'utf8')
const write=(relative,text)=>writeFileSync(path.join(root,relative),text)
const source=read('android/app/src/main/java/com/example/trudomix/data/local/AppIcons.kt')
const colors=['black','blue','coral','cream','mint','navy','violet','white','transparent']
mkdirSync(path.join(root,'frontend/public/loader-icons'),{recursive:true})
function svg(design,color){return read(`icons/TRACED_VECTOR_SVG/icon_${String(design).padStart(2,'0')}/${color}/icon.svg`)}
// AAPT limits individual strings. Short relative/horizontal/vertical commands
// retain every supplied vertex and even-odd contour, without splitting holes.
function compactPath(data){
    let x=0,y=0
    const number=n=>String(Math.round(n*100)/100)
    return [...data.matchAll(/([MLZ])\s*([^MLZ]*)/g)].map(([,command,args])=>{
        if(command==='Z')return 'Z'
        const [nx,ny]=args.trim().split(/[ ,]+/).map(Number)
        let output=`${command}${number(nx)},${number(ny)}`
        if(command==='L'){
            const relative=`l${number(nx-x)},${number(ny-y)}`
            if(relative.length<output.length)output=relative
            if(ny===y)output=`h${number(nx-x)}`
            else if(nx===x)output=`v${number(ny-y)}`
        }
        x=nx;y=ny;return output
    }).join('')
}
function vector(input){
    if(/<(image|g|defs|use|clipPath)\b/.test(input))throw Error('Unsupported SVG structure')
    const paths=[...input.matchAll(/<path\s+([^>]+)\/>/g)].map(([,attributes])=>{
        const attr=Object.fromEntries([...attributes.matchAll(/([\w-]+)="([^"]*)"/g)].map(([,key,value])=>[key,value]))
        if(!attr.d||!/^#[\da-f]{6}$/i.test(attr.fill))throw Error('Unsupported SVG path')
        const data=compactPath(attr.d)
        if(data.length>32760)throw Error(`Path exceeds Android resource limit: ${data.length}`)
        return `    <path android:fillColor="${attr.fill}" android:fillType="${attr['fill-rule']==='evenodd'?'evenOdd':'nonZero'}" android:pathData="${data}"/>`
    })
    if(!paths.length)throw Error('Empty SVG')
    return `<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="1024" android:viewportHeight="1024">\n${paths.join('\n')}\n</vector>\n`
}
for(let design=1;design<=9;design++)for(const color of colors){
    write(`frontend/public/loader-icons/icon_${String(design).padStart(2,'0')}_${color}.svg`,svg(design,color))
}
let count=0
for(const [,resource,design,color] of source.matchAll(/AppIcon\(\d+,\s*"[^"]+",\s*R\.drawable\.(\w+), design=(\d+), color="(\w+)"\)/g)){
    write(`android/app/src/main/res/drawable/${resource}.xml`,vector(svg(Number(design),color)))
    count++
}
if(count!==72)throw Error(`Expected 72 launcher variants, found ${count}`)
write('android/app/src/main/res/drawable/brand_vector_transparent.xml',vector(svg(6,'transparent')))
write('android/app/src/main/res/drawable/ic_launcher_foreground.xml','<inset xmlns:android="http://schemas.android.com/apk/res/android" android:inset="18dp" android:drawable="@drawable/brand_vector_transparent"/>\n')
for(const name of ['brand-trudomix','brand-dark-waves'])copyFileSync(path.join(root,'frontend/public/loader-icons/icon_06_black.svg'),path.join(root,`frontend/public/${name}.svg`))
console.log(`Imported 81 SVGs and ${count} Android VectorDrawables without raster conversion.`)

