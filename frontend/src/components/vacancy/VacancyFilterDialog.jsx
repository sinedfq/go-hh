import { useEffect, useRef } from 'react'
import ModalPortal from '../common/ModalPortal'

export default function VacancyFilterDialog({ children, onClose }) {
    const dialog = useRef(null)
    useEffect(() => {
        const previous = document.activeElement
        dialog.current.querySelector('button')?.focus()
        return () => previous?.focus?.()
    }, [])
    function keyDown(event) {
        if (event.key === 'Escape') { event.stopPropagation(); onClose() }
        if (event.key !== 'Tab') return
        const nodes = [...dialog.current.querySelectorAll('button:not(:disabled),input,select')]
        const first = nodes[0], last = nodes[nodes.length - 1]
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
    }
    return <ModalPortal><div className="vf-overlay" onClick={event => { if (event.target === event.currentTarget) onClose() }}>
        <section className="vf-dialog" role="dialog" aria-modal="true" aria-labelledby="vf-dialog-title" ref={dialog} onKeyDown={keyDown}>
            <header className="vf-dialog-header"><h2 id="vf-dialog-title">Фильтры поиска</h2><button type="button" aria-label="Закрыть фильтры" onClick={onClose}>×</button></header>
            {children}
        </section>
    </div></ModalPortal>
}
