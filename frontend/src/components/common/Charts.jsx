// ============================================
// ВСТРОЕННЫЕ ГРАФИКИ БЕЗ ЗАВИСИМОСТЕЙ (SVG)
// ============================================

// ====== КРУГОВОЙ ПРОГРЕСС (конверсия) ======
export function CircularProgress({ value, max = 100, size = 140, strokeWidth = 12, label, color = 'var(--accent)' }) {
    const radius = (size - strokeWidth) / 2
    const circumference = 2 * Math.PI * radius
    const percentage = Math.min(100, Math.max(0, (value / max) * 100))
    const offset = circumference - (percentage / 100) * circumference

    return (
        <div className="circular-progress-wrapper">
            <svg width={size} height={size} className="circular-progress">
                {/* Фоновый круг */}
                <circle
                    className="circular-progress-bg"
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    strokeWidth={strokeWidth}
                    fill="none"
                />
                {/* Прогресс */}
                <circle
                    className="circular-progress-value"
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    strokeWidth={strokeWidth}
                    fill="none"
                    stroke={color}
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    transform={`rotate(-90 ${size / 2} ${size / 2})`}
                />
            </svg>
            <div className="circular-progress-content">
                <div className="circular-progress-value-text">
                    {percentage.toFixed(1)}%
                </div>
                {label && <div className="circular-progress-label">{label}</div>}
            </div>
        </div>
    )
}

// ====== DONUT CHART (распределение статусов) ======
export function DonutChart({ data, size = 200, strokeWidth = 28 }) {
    // data = [{ label, value, color }]
    const total = data.reduce((sum, item) => sum + (item.value || 0), 0)
    const radius = (size - strokeWidth) / 2
    const circumference = 2 * Math.PI * radius

    if (total === 0) {
        return (
            <div className="donut-chart-wrapper">
                <svg width={size} height={size} className="donut-chart">
                    <circle
                        cx={size / 2}
                        cy={size / 2}
                        r={radius}
                        strokeWidth={strokeWidth}
                        fill="none"
                        className="donut-chart-empty"
                    />
                </svg>
                <div className="donut-chart-center">
                    <div className="donut-chart-total">0</div>
                    <div className="donut-chart-label">откликов</div>
                </div>
            </div>
        )
    }

    let currentOffset = 0
    const segments = data
        .filter(item => item.value > 0)
        .map(item => {
            const percentage = (item.value / total) * 100
            const segmentLength = (percentage / 100) * circumference
            const gap = 4 // промежуток между сегментами

            const segment = {
                ...item,
                offset: currentOffset,
                length: Math.max(0, segmentLength - gap),
            }
            currentOffset += segmentLength
            return segment
        })

    return (
        <div className="donut-chart-wrapper">
            <svg width={size} height={size} className="donut-chart" viewBox={`0 0 ${size} ${size}`}>
                {segments.map((seg, i) => (
                    <circle
                        key={i}
                        cx={size / 2}
                        cy={size / 2}
                        r={radius}
                        strokeWidth={strokeWidth}
                        fill="none"
                        stroke={seg.color}
                        strokeDasharray={`${seg.length} ${circumference - seg.length}`}
                        strokeDashoffset={-seg.offset}
                        strokeLinecap="round"
                        transform={`rotate(-90 ${size / 2} ${size / 2})`}
                        className="donut-segment"
                        style={{ animationDelay: `${i * 0.1}s` }}
                    />
                ))}
            </svg>
            <div className="donut-chart-center">
                <div className="donut-chart-total">{total}</div>
                <div className="donut-chart-label">всего</div>
            </div>
        </div>
    )
}

// ====== ГОРИЗОНТАЛЬНЫЕ БАРЫ (сравнение метрик) ======
export function HorizontalBars({ items }) {
    // items = [{ label, value, maxValue, color, unit }]
    const maxValue = Math.max(...items.map(i => i.value || 0), 1)

    return (
        <div className="horizontal-bars">
            {items.map((item, i) => {
                const percentage = ((item.value || 0) / maxValue) * 100
                return (
                    <div key={i} className="horizontal-bar-row">
                        <div className="horizontal-bar-label">
                            <span className="bar-label-text">{item.label}</span>
                            <span className="bar-label-value">
                                {item.value || 0}
                                {item.unit && <span className="bar-label-unit">{item.unit}</span>}
                            </span>
                        </div>
                        <div className="horizontal-bar-track">
                            <div
                                className="horizontal-bar-fill"
                                style={{
                                    width: `${percentage}%`,
                                    background: item.color || 'var(--accent)',
                                    animationDelay: `${i * 0.1}s`
                                }}
                            />
                        </div>
                    </div>
                )
            })}
        </div>
    )
}