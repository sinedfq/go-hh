import { useState, useRef } from 'react'
import { motion, useMotionValue, useTransform } from 'framer-motion'

function SwipeableCard({ vacancy, onSwipe, isTop, matchScore, onOpenVacancy, onOpenCompany }) {
    const x = useMotionValue(0)
    const [exitDirection, setExitDirection] = useState(null)
    const [isDragging, setIsDragging] = useState(false)
    const isProcessing = useRef(false) // предотвращает двойные свайпы

    const rotate = useTransform(x, [-400, 0, 400], [-25, 0, 25])
    const likeOpacity = useTransform(x, [0, 150, 250], [0, 0.6, 1])
    const nopeOpacity = useTransform(x, [0, -150, -250], [0, 0.6, 1])

    const handleDragStart = () => {
        if (!isTop || isProcessing.current) return
        setIsDragging(true)
    }

    const handleDragEnd = (event, info) => {
        if (isProcessing.current) return

        const offset = info.offset.x
        const velocity = info.velocity.x
        const swipeThreshold = 120 // уменьшил порог для мобильных
        const velocityThreshold = 600

        let direction = null
        if (offset > swipeThreshold || velocity > velocityThreshold) {
            direction = 'right'
        } else if (offset < -swipeThreshold || velocity < -velocityThreshold) {
            direction = 'left'
        }

        if (direction) {
            isProcessing.current = true
            setExitDirection(direction)
            // Используем setTimeout для завершения анимации
            setTimeout(() => {
                onSwipe(direction, vacancy.id)
                isProcessing.current = false
            }, 350)
        } else {
            setIsDragging(false)
        }
    }

    const handleButtonClick = (dir) => {
        if (!isTop || exitDirection || isProcessing.current) return
        isProcessing.current = true
        setExitDirection(dir)
        setTimeout(() => {
            onSwipe(dir, vacancy.id)
            isProcessing.current = false
        }, 350)
    }

    const handleOpenVacancy = () => {
        if (!isTop || exitDirection || isProcessing.current) return
        if (onOpenVacancy) onOpenVacancy(vacancy.id)
    }

    const getScoreColor = (score) => {
        if (score === undefined || score === null) return '#868e96'
        if (score >= 0.7) return '#2e7d32'
        if (score >= 0.4) return '#f57c00'
        return '#c62828'
    }

    const renderScoreBadge = () => {
        if (matchScore === undefined || matchScore === null) {
            return (
                <div className="match-score-badge unknown" title="Не проанализировано AI">
                    <span className="score-number">—</span>
                    <span className="score-suffix">нет данных</span>
                </div>
            )
        }

        const percent = Math.round(matchScore * 100)
        return (
            <div
                className="match-score-badge"
                style={{ '--score-color': getScoreColor(matchScore) }}
                title={`Совместимость: ${percent}%`}
            >
                <span className="score-number">{percent}</span>
                <span className="score-suffix">%</span>
            </div>
        )
    }

    const exitAnimation = exitDirection === 'right'
        ? { x: 600, opacity: 0, rotate: 30 }
        : exitDirection === 'left'
            ? { x: -600, opacity: 0, rotate: -30 }
            : { x: 0, opacity: 1, rotate: 0 }

    return (
        <motion.div
            className="swipe-card"
            style={{
                x,
                rotate,
                zIndex: isTop ? 10 : 5,
                transformOrigin: 'bottom center'
            }}
            drag={isTop && !exitDirection && !isProcessing.current ? "x" : false}
            dragConstraints={{ left: 0, right: 0 }}
            dragElastic={0.9}
            dragMomentum={false}
            dragSnapToOrigin={true}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
            animate={exitAnimation}
            transition={{
                type: exitDirection ? 'tween' : 'spring',
                duration: exitDirection ? 0.3 : undefined,
                stiffness: 300,
                damping: 30
            }}
            whileDrag={{
                scale: 1.02,
                boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)'
            }}
        >
            <div className="card-content">
                <div className="card-header">
                    <div className="card-header-content">
                        <h2>{vacancy.title}</h2>
                        <div className="company" onClick={() => onOpenCompany && onOpenCompany(vacancy.company_id)}>
                            {vacancy.company}
                        </div>
                    </div>
                    {renderScoreBadge()}
                </div>

                <div className="card-meta">
                    <div className="meta-item">
                        <span className="meta-label">Локация</span>
                        <span className="meta-value">
                            {vacancy.location}
                            {vacancy.remote && <span className="badge remote">Удалённо</span>}
                        </span>
                    </div>
                    <div className="meta-item">
                        <span className="meta-label">Опыт</span>
                        <span className="meta-value">{vacancy.experience}</span>
                    </div>
                </div>

                {vacancy.skills && vacancy.skills.length > 0 && (
                    <div className="card-section">
                        <h3>Требуемые навыки</h3>
                        <div className="skills">
                            {vacancy.skills.map((skill, i) => (
                                <span key={i} className="skill-tag">{skill}</span>
                            ))}
                        </div>
                    </div>
                )}

                {vacancy.description && (
                    <div className="card-section">
                        <h3>Описание</h3>
                        <p className="description">{vacancy.description}</p>
                    </div>
                )}
            </div>

            {isTop && !exitDirection && !isDragging && !isProcessing.current && (
                <div className="card-buttons">
                    <button
                        className="card-btn card-btn-skip"
                        onClick={() => handleButtonClick('left')}
                        type="button"
                    >
                        Пропустить
                    </button>
                    <button
                        className="card-btn card-btn-info"
                        onClick={handleOpenVacancy}
                        type="button"
                    >
                        Подробнее
                    </button>
                    <button
                        className="card-btn card-btn-like"
                        onClick={() => handleButtonClick('right')}
                        type="button"
                    >
                        Нравится
                    </button>
                </div>
            )}

            <motion.div
                className="swipe-overlay like-overlay"
                style={{ opacity: likeOpacity }}
            >
                <span className="overlay-text">НРАВИТСЯ</span>
            </motion.div>
            <motion.div
                className="swipe-overlay nope-overlay"
                style={{ opacity: nopeOpacity }}
            >
                <span className="overlay-text">ПРОПУСТИТЬ</span>
            </motion.div>
        </motion.div>
    )
}

export default SwipeableCard