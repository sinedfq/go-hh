import { useState } from 'react'
import { motion, useMotionValue, useTransform } from 'framer-motion'

function SwipeableCard({ vacancy, onSwipe, isTop, matchScore, onOpenVacancy, onOpenCompany }) {
  const x = useMotionValue(0)
  const [exitDirection, setExitDirection] = useState(null)
  const [isDragging, setIsDragging] = useState(false)

  const rotate = useTransform(x, [-400, 0, 400], [-30, 0, 30])
  const likeOpacity = useTransform(x, [0, 150, 250], [0, 0.5, 1])
  const nopeOpacity = useTransform(x, [0, -150, -250], [0, 0.5, 1])

  const handleDragStart = () => {
    if (!isTop) return
    setIsDragging(true)
  }

  const handleDragEnd = (event, info) => {
    const offset = info.offset.x
    const velocity = info.velocity.x
    const swipeThreshold = 150
    const velocityThreshold = 800

    if (offset > swipeThreshold || velocity > velocityThreshold) {
      setExitDirection('right')
      setTimeout(() => onSwipe('right', vacancy.id), 400)
    } else if (offset < -swipeThreshold || velocity < -velocityThreshold) {
      setExitDirection('left')
      setTimeout(() => onSwipe('left', vacancy.id), 400)
    } else {
      setIsDragging(false)
    }
  }

  const handleDrag = () => { }

  const handleButtonClick = (dir) => {
    if (!isTop || exitDirection) return
    setExitDirection(dir)
    setTimeout(() => onSwipe(dir, vacancy.id), 400)
  }

  const handleOpenVacancy = () => {
    if (!isTop || exitDirection) return
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
    ? { x: 600, opacity: 0, rotate: 35 }
    : exitDirection === 'left'
      ? { x: -600, opacity: 0, rotate: -35 }
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
      drag={isTop && !exitDirection ? "x" : false}
      dragConstraints={{ left: 0, right: 0 }}
      dragElastic={0.7}
      dragMomentum={false}
      onDragStart={handleDragStart}
      onDrag={handleDrag}
      onDragEnd={handleDragEnd}
      animate={exitAnimation}
      transition={{
        type: 'spring',
        stiffness: 300,
        damping: 30
      }}
    >
      <div className="card-content">
        <div className="card-header">
          <div className="card-header-content">
            <h2>{vacancy.title}</h2>
            <div className="company">{vacancy.company}</div>
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

      {isTop && !exitDirection && !isDragging && (
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