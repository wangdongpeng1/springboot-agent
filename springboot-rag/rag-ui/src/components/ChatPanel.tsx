import { useState, useRef, useEffect } from 'react'
import ReactMarkdown from 'react-markdown'
import type { Message, GraphData } from './types'
import './ChatPanel.css'

interface Props {
  onGraphUpdate: (data: GraphData) => void
}

function ChatPanel({ onGraphUpdate }: Props) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [conversationId, setConversationId] = useState<string>('')
  const [history, setHistory] = useState<string[]>([])
  const [showHistory, setShowHistory] = useState(false)
  const [liveTime, setLiveTime] = useState(0)
  const [streamingContent, setStreamingContent] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const startTimeRef = useRef(0)

  // 实时计时器：loading 或 streaming 时每 100ms 更新
  useEffect(() => {
    if (!loading && !streamingContent) {
      setLiveTime(0)
      return
    }
    if (loading) startTimeRef.current = performance.now()
    const timer = setInterval(() => {
      setLiveTime(Math.round(performance.now() - startTimeRef.current))
    }, 100)
    return () => clearInterval(timer)
  }, [loading, streamingContent])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    if (!input.trim() || loading || streamingContent) return

    const question = input.trim()
    setInput('')
    setShowHistory(false)
    setHistory(prev => [question, ...prev.filter(q => q !== question)].slice(0, 20))
    setMessages(prev => [...prev, { role: 'user', content: question }])
    setLoading(true)
    setStreamingContent('')
    onGraphUpdate({ nodes: [], edges: [] })  // 清空图谱

    const startTime = performance.now()

    try {
      const params = new URLSearchParams({ question })
      if (conversationId) params.append('conversationId', conversationId)

      const res = await fetch(`/rag/ask-with-graph?${params}`)
      if (!res.body) throw new Error('No response body')

      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let firstToken = true
      let accumulated = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        let separatorIdx: number
        while ((separatorIdx = buffer.indexOf('\n\n')) !== -1) {
          const eventBlock = buffer.substring(0, separatorIdx)
          buffer = buffer.substring(separatorIdx + 2)

          let eventName = ''
          const dataLines: string[] = []
          for (const line of eventBlock.split('\n')) {
            if (line.startsWith('event:')) eventName = line.substring(6).trim()
            else if (line.startsWith('data:')) dataLines.push(line.substring(5))
          }
          const eventData = dataLines.join('\n').trim()

          if (eventName === 'graph') {
            const graph: GraphData = JSON.parse(eventData)
            onGraphUpdate(graph)
          } else if (eventName === 'token') {
            if (firstToken) {
              setLoading(false)
              firstToken = false
            }
            // JSON 解码还原换行等特殊字符
            accumulated += JSON.parse(eventData)
            setStreamingContent(accumulated)
          } else if (eventName === 'done') {
            const elapsed = Math.round(performance.now() - startTime)
            try {
              const data = JSON.parse(eventData)
              setConversationId(data.conversationId)
            } catch {}
            const finalContent = accumulated  // 闭包快照，避免被后续 accumulated = '' 清空
            setMessages(prev => [
              ...prev,
              { role: 'assistant' as const, content: finalContent, duration: elapsed }
            ])
            setStreamingContent('')
            accumulated = ''
          } else if (eventName === 'error') {
            setMessages(prev => [...prev, {
              role: 'assistant', content: `请求失败：${eventData}`
            }])
            setLoading(false)
          }
        }
      }

      // 流结束但未收到 done 事件（连接中断/解析失败），兜底保留已流式内容
      if (accumulated) {
        const elapsed = Math.round(performance.now() - startTime)
        const finalContent = accumulated
        setMessages(prev => [
          ...prev,
          { role: 'assistant' as const, content: finalContent, duration: elapsed }
        ])
      }
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: '请求失败，请检查后端服务是否启动。' }])
    } finally {
      setLoading(false)
      setStreamingContent('')
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="chat-container">
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="chat-empty">
            <div className="empty-icon">◈</div>
            <p>输入问题开始对话</p>
            <p className="empty-hint">知识图谱将随对话动态更新</p>
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`message ${msg.role}`}>
            <div className="message-avatar">
              {msg.role === 'user' ? '⊙' : '◈'}
            </div>
            <div className="message-content">
              {msg.role === 'assistant' ? (
                <>
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                  {msg.duration && (
                    <div className="message-duration">{(msg.duration / 1000).toFixed(1)}s</div>
                  )}
                </>
              ) : (
                <p>{msg.content}</p>
              )}
            </div>
          </div>
        ))}
        {loading && !streamingContent && (
          <div className="message assistant">
            <div className="message-avatar">◈</div>
            <div className="message-content loading">
              <span className="dot-pulse">...</span>
              <span className="live-timer">{(liveTime / 1000).toFixed(1)}s</span>
            </div>
          </div>
        )}
        {streamingContent && (
          <div className="message assistant">
            <div className="message-avatar">◈</div>
            <div className="message-content">
              <ReactMarkdown>{streamingContent + ' ▍'}</ReactMarkdown>
              <span className="live-timer">{(liveTime / 1000).toFixed(1)}s</span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="chat-input-area">
        <div className="input-wrapper">
          {showHistory && history.length > 0 && (
            <div className="history-dropdown">
              <div className="history-title">历史问题</div>
              {history.map((q, i) => (
                <div
                  key={i}
                  className="history-item"
                  onClick={() => { setInput(q); setShowHistory(false) }}
                >
                  {q}
                </div>
              ))}
            </div>
          )}
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => !input && history.length > 0 && setShowHistory(true)}
            onBlur={() => setTimeout(() => setShowHistory(false), 150)}
            placeholder="输入你的问题..."
            disabled={loading || !!streamingContent}
          />
        </div>
        <button onClick={handleSend} disabled={loading || !!streamingContent || !input.trim()}>
          {loading ? '...' : '→'}
        </button>
      </div>
    </div>
  )
}

export default ChatPanel
