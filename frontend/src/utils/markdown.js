function inline(text) {
  return String(text).split(/(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\(https?:\/\/[^\s)]+\))/g).filter(Boolean).map((part) => {
    if (part.startsWith('`')) return { type: 'code', text: part.slice(1, -1) };
    if (part.startsWith('**')) return { type: 'strong', text: part.slice(2, -2) };
    const link = part.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/);
    return link ? { type: 'link', text: link[1], href: link[2] } : { type: 'text', text: part };
  });
}

export function markdownBlocks(content) {
  const lines = String(content || '').replace(/\r/g, '').split('\n');
  const blocks = [];
  for (let index = 0; index < lines.length;) {
    const line = lines[index];
    if (!line.trim()) { index += 1; continue; }
    if (line.startsWith('```')) {
      const code = []; const language = line.slice(3).trim(); index += 1;
      while (index < lines.length && !lines[index].startsWith('```')) code.push(lines[index++]);
      blocks.push({ type: 'code', language, text: code.join('\n') }); index += 1; continue;
    }
    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) { blocks.push({ type: 'heading', level: heading[1].length, parts: inline(heading[2]) }); index += 1; continue; }
    const list = line.match(/^[-*]\s+(.+)$/);
    const ordered = line.match(/^\d+\.\s+(.+)$/);
    if (list || ordered) {
      const type = ordered ? 'ordered-list' : 'list'; const pattern = ordered ? /^\d+\.\s+(.+)$/ : /^[-*]\s+(.+)$/; const items = [];
      while (index < lines.length && pattern.test(lines[index])) items.push(inline(lines[index++].match(pattern)[1]));
      blocks.push({ type, items }); continue;
    }
    if (line.startsWith('> ')) { blocks.push({ type: 'quote', parts: inline(line.slice(2)) }); index += 1; continue; }
    const paragraph = [line]; index += 1;
    while (index < lines.length && lines[index].trim() && !/^(#{1,3}\s|[-*]\s|\d+\.\s|> |```)/.test(lines[index])) paragraph.push(lines[index++]);
    blocks.push({ type: 'paragraph', parts: inline(paragraph.join('\n')) });
  }
  return blocks;
}
