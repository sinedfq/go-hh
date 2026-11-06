import { readFile, readdir } from 'node:fs/promises'
import { extname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export function assertSourceEncoding(bytes, filename) {
    let text
    try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes) }
    catch { throw new Error(`${filename}: invalid UTF-8`) }
    if (text.includes('\uFFFD')) throw new Error(`${filename}: Unicode replacement character found`)
    if (/\?{3,}/.test(text)) throw new Error(`${filename}: question-mark placeholders found; check text encoding`)
    return text
}

async function checkDirectory(directory) {
    let checked = 0
    for (const entry of await readdir(directory, { withFileTypes: true })) {
        const path = resolve(directory, entry.name)
        if (entry.isDirectory()) checked += await checkDirectory(path)
        else if (['.js', '.jsx', '.ts', '.tsx', '.css', '.json', '.html', '.svg'].includes(extname(path)) && !/\.test\.[jt]sx?$/.test(path)) {
            assertSourceEncoding(await readFile(path), path)
            checked++
        }
    }
    return checked
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const count = await checkDirectory(fileURLToPath(new URL('../src', import.meta.url)))
    console.log(`UTF-8 source check passed (${count} files)`)
}
