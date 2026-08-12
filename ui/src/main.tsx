import { createRoot } from "react-dom/client";
import { useEffect, useState } from "react";
import "./style.css";

type FlowNode = { id: string; label: string; codeExpression: string; boundaryKind?: string; isDocumented: boolean; expandable: boolean; children: FlowNode[] };
type FlowDocument = { root: { title: string; signature?: string }; nodes: FlowNode[] };
type Message = { type: "document"; document: FlowDocument } | { type: "result"; nodeId: string; document: FlowDocument } | { type: "error"; message: string };

declare global { interface Window { wecoviReceive?: (message: Message) => void; wecoviPost?: (message: string) => void } }

const post = (message: object) => window.wecoviPost?.(JSON.stringify(message));

function App() {
  const [document, setDocument] = useState<FlowDocument | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<Set<string>>(new Set());
  useEffect(() => {
    window.wecoviReceive = (message) => {
      if (message.type === "document") { setDocument(message.document); setError(null); setPending(new Set()); }
      if (message.type === "result") { setDocument(current => current && replaceNode(current, message.nodeId, message.document.nodes)); setPending(current => without(current, message.nodeId)); setError(null); }
      if (message.type === "error") { setError(message.message); setPending(new Set()); }
    };
    post({ type: "ready" });
  }, []);
  const expand = (nodeId: string) => {
    if (pending.has(nodeId)) return;
    setPending(current => new Set(current).add(nodeId));
    setError(null);
    post({ type: "expandNode", nodeId });
  };
  return <main>{error && <p role="alert">{error}<button onClick={() => post({ type: "ready" })}>Retry</button></p>}{document ? <><h1>{document.root.title}</h1><small>{document.root.signature}</small>{document.nodes.map(node => <Node key={node.id} node={node} pending={pending} expand={expand} />)}</> : "Loading flow…"}</main>;
}

function Node({ node, pending, expand }: { node: FlowNode; pending: Set<string>; expand: (nodeId: string) => void }) {
  return <section className={node.boundaryKind ? "boundary" : ""}><button onClick={event => event.metaKey || event.ctrlKey ? post({ type: "openSource", nodeId: node.id }) : undefined}>{node.label}</button><code>{node.codeExpression}</code>{!node.isDocumented && <small>Undocumented</small>}{node.expandable && <button disabled={pending.has(node.id)} onClick={() => expand(node.id)}>{pending.has(node.id) ? "Expanding…" : "Expand"}</button>}{node.boundaryKind && <small>{node.boundaryKind}</small>}{node.children.map(child => <Node key={child.id} node={child} pending={pending} expand={expand} />)}</section>;
}

function replaceNode(document: FlowDocument, nodeId: string, children: FlowNode[]): FlowDocument {
  const replace = (node: FlowNode): FlowNode => node.id === nodeId ? { ...node, children } : { ...node, children: node.children.map(replace) };
  return { ...document, nodes: document.nodes.map(replace) };
}

function without(values: Set<string>, value: string) { const next = new Set(values); next.delete(value); return next; }

createRoot(document.getElementById("root")!).render(<App />);
