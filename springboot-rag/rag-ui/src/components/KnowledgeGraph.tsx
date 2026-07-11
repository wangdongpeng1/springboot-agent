import { useEffect, useRef, useMemo, useState, useCallback } from 'react'
import ForceGraph2D from 'react-force-graph-2d'
import type { GraphData } from './types'
import './KnowledgeGraph.css'

interface Props {
  data: GraphData
  hasQueried?: boolean
}

interface FgNode {
  id: string
  name: string
  type: string
  x?: number
  y?: number
  degree?: number
}

interface FgLink {
  source: string | FgNode
  target: string | FgNode
  relation: string
}

/** 安全圆角矩形（兼容不支持 roundRect 的浏览器） */
function safeRoundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  r = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function KnowledgeGraph({ data, hasQueried }: Props) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const fgRef = useRef<any>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [dimensions, setDimensions] = useState({ width: 800, height: 600 })

  // 计算节点度数（连接数）
  const degreeMap = useMemo(() => {
    const map = new Map<string, number>()
    data.edges.forEach(e => {
      map.set(e.source, (map.get(e.source) || 0) + 1)
      map.set(e.target, (map.get(e.target) || 0) + 1)
    })
    return map
  }, [data.edges])

  const graphData = useMemo(() => {
    const nodes: FgNode[] = data.nodes.map(n => ({
      ...n,
      degree: degreeMap.get(n.id) || 0,
    }))
    const links: FgLink[] = data.edges.map(e => ({
      source: e.source,
      target: e.target,
      relation: e.relation,
    }))
    return { nodes, links }
  }, [data, degreeMap])

  // 容器尺寸自适应
  const updateDimensions = useCallback(() => {
    if (containerRef.current) {
      const { clientWidth, clientHeight } = containerRef.current
      if (clientWidth > 0 && clientHeight > 0) {
        setDimensions({ width: clientWidth, height: clientHeight })
      }
    }
  }, [])

  useEffect(() => {
    updateDimensions()
    window.addEventListener('resize', updateDimensions)
    return () => window.removeEventListener('resize', updateDimensions)
  }, [updateDimensions, data])

  useEffect(() => {
    if (fgRef.current && data.nodes.length > 0) {
      const fg = fgRef.current
      fg.d3Force('charge').strength(-500).distanceMax(500)
      fg.d3Force('link').distance(150).strength(0.5)
      fg.d3Force('center').strength(0.05)
      fg.d3ReheatSimulation()
      setTimeout(() => {
        try { fg.zoomToFit(600, 60) } catch { /* ignore */ }
      }, 1200)
    }
  }, [data])

  if (data.nodes.length === 0) {
    return (
      <div className="graph-empty">
        <div className="graph-empty-icon">◈</div>
        {hasQueried ? (
          <>
            <p>未找到相关知识图谱</p>
            <p className="graph-empty-hint">当前问题不在图数据库中，请尝试重新询问</p>
          </>
        ) : (
          <>
            <p>等待对话数据...</p>
            <p className="graph-empty-hint">发送问题后将自动渲染知识图谱</p>
          </>
        )}
      </div>
    )
  }

  return (
    <div className="graph-container" ref={containerRef}>
      <ForceGraph2D
        ref={fgRef}
        graphData={graphData}
        width={dimensions.width}
        height={dimensions.height}
        backgroundColor="#0a0e14"
        warmupTicks={100}
        cooldownTicks={200}
        d3AlphaDecay={0.02}
        d3VelocityDecay={0.3}
        nodeCanvasObject={(node, ctx, globalScale) => {
          try {
            const fgNode = node as FgNode
            const isChemical = fgNode.type === 'chemical'
            const degree = fgNode.degree || 1
            // 化学品节点固定较大尺寸，属性节点根据连接数微调
            const radius = isChemical
              ? 18 / globalScale
              : (6 + Math.min(degree, 4) * 0.8) / globalScale
            const x = node.x!
            const y = node.y!

            if (isChemical) {
              // 脉冲环
              const pulseRadius = radius * 2.0
              ctx.beginPath()
              ctx.arc(x, y, pulseRadius, 0, 2 * Math.PI)
              ctx.strokeStyle = 'rgba(0, 212, 255, 0.15)'
              ctx.lineWidth = 1.5 / globalScale
              ctx.stroke()

              const pulseRadius2 = radius * 1.5
              ctx.beginPath()
              ctx.arc(x, y, pulseRadius2, 0, 2 * Math.PI)
              ctx.strokeStyle = 'rgba(0, 212, 255, 0.25)'
              ctx.lineWidth = 1 / globalScale
              ctx.stroke()

              // 径向渐变
              const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius)
              gradient.addColorStop(0, '#66eeff')
              gradient.addColorStop(0.5, '#00d4ff')
              gradient.addColorStop(1, '#0088aa')
              ctx.shadowColor = 'rgba(0, 212, 255, 0.8)'
              ctx.shadowBlur = 20 / globalScale
              ctx.beginPath()
              ctx.arc(x, y, radius, 0, 2 * Math.PI)
              ctx.fillStyle = gradient
              ctx.fill()
              ctx.shadowBlur = 0

              // 高光
              ctx.beginPath()
              ctx.arc(x - radius * 0.25, y - radius * 0.25, radius * 0.35, 0, 2 * Math.PI)
              ctx.fillStyle = 'rgba(255, 255, 255, 0.25)'
              ctx.fill()
            } else {
              // 属性节点渐变
              const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius)
              gradient.addColorStop(0, '#88ffcc')
              gradient.addColorStop(0.6, '#00ffaa')
              gradient.addColorStop(1, '#009966')
              ctx.shadowColor = 'rgba(0, 255, 170, 0.5)'
              ctx.shadowBlur = 12 / globalScale
              ctx.beginPath()
              ctx.arc(x, y, radius, 0, 2 * Math.PI)
              ctx.fillStyle = gradient
              ctx.fill()
              ctx.shadowBlur = 0

              // 高光
              ctx.beginPath()
              ctx.arc(x - radius * 0.2, y - radius * 0.2, radius * 0.3, 0, 2 * Math.PI)
              ctx.fillStyle = 'rgba(255, 255, 255, 0.2)'
              ctx.fill()
            }

            // 标签
            const label = fgNode.name
            const fontSize = Math.max(11 / globalScale, 3)
            ctx.font = `500 ${fontSize}px sans-serif`
            ctx.textAlign = 'center'
            ctx.textBaseline = 'top'
            const labelY = y + radius + 4 / globalScale
            const textWidth = ctx.measureText(label).width
            const padding = 4 / globalScale

            // 标签背景（兼容写法，不用 roundRect）
            ctx.fillStyle = 'rgba(13, 17, 23, 0.75)'
            safeRoundRect(ctx, x - textWidth / 2 - padding, labelY - padding / 2, textWidth + padding * 2, fontSize + padding, 3 / globalScale)
            ctx.fill()

            // 标签文字
            ctx.fillStyle = isChemical ? '#7ee8ff' : '#a0ffd8'
            ctx.fillText(label, x, labelY)
          } catch {
            // 静默处理，避免渲染异常导致白屏
          }
        }}
        linkCanvasObject={(link, ctx, globalScale) => {
          try {
            const fgLink = link as FgLink
            const source = fgLink.source as FgNode
            const target = fgLink.target as FgNode

            if (!source.x || !source.y || !target.x || !target.y) return

            const sx = source.x, sy = source.y
            const tx = target.x, ty = target.y

            // 渐变连线
            const gradient = ctx.createLinearGradient(sx, sy, tx, ty)
            gradient.addColorStop(0, 'rgba(0, 212, 255, 0.5)')
            gradient.addColorStop(0.5, 'rgba(0, 200, 220, 0.35)')
            gradient.addColorStop(1, 'rgba(0, 255, 170, 0.5)')
            ctx.strokeStyle = gradient
            ctx.lineWidth = 1.5 / globalScale
            ctx.beginPath()
            ctx.moveTo(sx, sy)
            ctx.lineTo(tx, ty)
            ctx.stroke()

            // 边标签
            const midX = (sx + tx) / 2
            const midY = (sy + ty) / 2
            const fontSize = Math.max(9 / globalScale, 2.5)
            ctx.font = `${fontSize}px sans-serif`
            ctx.textAlign = 'center'
            ctx.textBaseline = 'middle'
            const relText = fgLink.relation.replace(/_/g, ' ')
            const tw = ctx.measureText(relText).width
            const pad = 3 / globalScale

            // 标签背景
            ctx.fillStyle = 'rgba(13, 17, 23, 0.7)'
            safeRoundRect(ctx, midX - tw / 2 - pad, midY - fontSize / 2 - pad / 2, tw + pad * 2, fontSize + pad, 2 / globalScale)
            ctx.fill()

            // 标签文字
            ctx.fillStyle = 'rgba(139, 180, 220, 0.85)'
            ctx.fillText(relText, midX, midY)
          } catch {
            // 静默处理
          }
        }}
        linkDirectionalParticles={3}
        linkDirectionalParticleWidth={1.2}
        linkDirectionalParticleColor={() => '#00d4ff'}
        linkDirectionalParticleSpeed={0.004}
      />
    </div>
  )
}

export default KnowledgeGraph
