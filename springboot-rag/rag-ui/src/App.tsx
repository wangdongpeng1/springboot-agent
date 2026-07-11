import { useState } from 'react'
import KnowledgeGraph from './components/KnowledgeGraph'
import ChatPanel from './components/ChatPanel'
import type { GraphData } from './components/types'
import './App.css'

function App() {
  const [graphData, setGraphData] = useState<GraphData>({ nodes: [], edges: [] })
  const [hasQueried, setHasQueried] = useState(false)

  const handleGraphUpdate = (data: GraphData) => {
    setHasQueried(true)
    setGraphData(data)
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="logo">
          <span className="logo-icon">◈</span>
          <span className="logo-text">RAG 知识图谱</span>
        </div>
        <div className="header-status">
          <span className="status-dot" />
          <span className="status-text">System Online</span>
        </div>
      </header>

      <main className="app-main">
        <section className="graph-panel">
          <div className="panel-header">
            <span className="panel-title">KNOWLEDGE GRAPH</span>
            <span className="node-count">
              {graphData.nodes.length} nodes / {graphData.edges.length} edges
            </span>
          </div>
          <KnowledgeGraph data={graphData} hasQueried={hasQueried} />
        </section>

        <section className="chat-panel">
          <ChatPanel onGraphUpdate={handleGraphUpdate} />
        </section>
      </main>
    </div>
  )
}

export default App
