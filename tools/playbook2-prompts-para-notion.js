// Converte o Playbook2-Prompts.md num arquivo Markdown unico, formatado para importar no Notion.
// Uso (na raiz do repositorio): node tools/playbook2-prompts-para-notion.js "<pasta-de-saida>"
// O Playbook2-Prompts.md continua sendo a FONTE: edite ele e rode este script de novo.
const fs = require('fs');
const path = require('path');

const saidaDir = process.argv[2] || '.';
const NOME = 'Playbook2-Prompts - um prompt por modulo (Spring Boot + MySQL + AWS).md';
const fonte = fs.readFileSync('Playbook2-Prompts.md', 'utf8').replace(/\r\n/g, '\n').split('\n');

const ehFence = (l) => /^\s*```/.test(l);
// remove ** fora de `codigo`
const semNegrito = (l) => l.split(/(`[^`]*`)/).map((seg, i) => (i % 2 === 1 ? seg : seg.replace(/\*\*/g, ''))).join('');

const out = [];
let fence = false;
let comecou = false;
for (const l of fonte) {
  if (ehFence(l)) { fence = !fence; out.push(l); continue; }
  if (fence) { out.push(l); continue; }               // dentro de bloco de codigo: intacto
  if (!comecou) { if (l.startsWith('# ')) comecou = true; continue; } // descarta o titulo H1 (o Notion usa o nome do arquivo)
  if (l.trim() === '---') continue;                    // separadores
  if (/^### /.test(l)) { out.push('## ' + semNegrito(l.slice(4))); continue; }   // modulos e prompts de apoio: H2
  if (/^## /.test(l)) { out.push('# ' + semNegrito(l.slice(3))); continue; }     // secoes e blocos: H1
  out.push(semNegrito(l));
}
// colapsa linhas em branco repetidas (fora de blocos de codigo)
const final = []; let f2 = false;
for (const l of out) {
  if (ehFence(l)) f2 = !f2;
  if (!f2 && l.trim() === '' && final.length && final[final.length - 1].trim() === '') continue;
  final.push(l);
}
// remove linha em branco no inicio
while (final.length && !final[0].trim()) final.shift();
fs.mkdirSync(saidaDir, { recursive: true });
const destino = path.join(saidaDir, NOME);
fs.writeFileSync(destino, final.join('\n').replace(/\s+$/, '') + '\n', 'utf8');
console.log('gerado: ' + destino + ' (' + final.length + ' linhas)');
