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
  useEffect(() => {
    window.wecoviReceive = (message) => {
      if (message.type === "document") setDocument(message.document);
      if (message.type === "result") setDocument(current => current && replaceNode(current, message.nodeId, message.document.nodes));
      if (message.type === "error") setError(message.message);
    };
    post({ type: "ready" });
  }, []);
  return <main>{error && <p role="alert">{error}</p>}{document ? <><h1>{document.root.title}</h1><small>{document.root.signature}</small>{document.nodes.map(node => <Node key={node.id} node={node} />)}</> : "Loading flow…"}</main>;
}

function Node({ node }: { node: FlowNode }) {
  return <section className={node.boundaryKind ? "boundary" : ""}><button onClick={event => event.metaKey || event.ctrlKey ? post({ type: "openSource", nodeId: node.id }) : undefined}>{node.label}</button><code>{node.codeExpression}</code>{node.expandable && <button onClick={() => post({ type: "expandNode", nodeId: node.id })}>Expand</button>}{node.boundaryKind && <small>{node.boundaryKind}</small>}{node.children.map(child => <Node key={child.id} node={child} />)}</section>;
}

function replaceNode(document: FlowDocument, nodeId: string, children: FlowNode[]): FlowDocument {
  const replace = (node: FlowNode): FlowNode => node.id === nodeId ? { ...node, children } : { ...node, children: node.children.map(replace) };
  return { ...document, nodes: document.nodes.map(replace) };
}

createRoot(document.getElementById("root")!).render(<App />);
