import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export default function MarkdownContent({ children }) {
    return <div className="markdown-content"><Markdown remarkPlugins={[remarkGfm]} skipHtml components={{img:({node:_node,...props})=><img {...props} loading="lazy"/>,a:({node:_node,...props})=><a {...props} target="_blank" rel="noopener noreferrer"/>}}>{children||''}</Markdown></div>
}
