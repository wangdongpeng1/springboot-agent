export interface GraphNode {
  id: string
  name: string
  type: string
}

export interface GraphEdge {
  source: string
  target: string
  relation: string
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export interface ChatResponse {
  answer: string
  conversationId: string
  graph: GraphData
}

export interface Message {
  role: 'user' | 'assistant'
  content: string
  duration?: number
}
