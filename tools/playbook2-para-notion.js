// Converte o Playbook2.md num arquivo Markdown unico, formatado para importar no Notion.
// Uso (na raiz do repositorio): node tools/playbook2-para-notion.js "<pasta-de-saida>"
// O Playbook2.md continua sendo a FONTE: edite ele e rode este script de novo para refazer o arquivo.
const fs = require('fs');
const path = require('path');

const ENTRADA = 'Playbook2.md';
const saidaDir = process.argv[2] || '.';
const NOME = 'Playbook2 - API profissional (Spring Boot + MySQL + AWS).md';

const fonte = fs.readFileSync(ENTRADA, 'utf8').replace(/\r\n/g, '\n').split('\n');

// ---------- utilidades ----------
const MAIUSCULA = /[A-ZÁÉÍÓÚÂÊÔÃÕÇ]/;

// divide um texto em partes, respeitando `codigo` e (parenteses); sep: funcao(texto, i) -> tamanho do separador ou 0
function dividir(texto, sep) {
  const partes = []; let atual = ''; let codigo = false; let prof = 0;
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (c === '`') codigo = !codigo;
    if (!codigo) { if (c === '(') prof++; if (c === ')') prof = Math.max(0, prof - 1); }
    if (!codigo && prof === 0) {
      const n = sep(texto, i); // tamanho do separador (0 = nao e separador)
      if (n > 0) {
        // se o separador começa com "?", o ponto de interrogação fica na parte anterior
        partes.push(atual + (texto[i] === '?' ? '?' : ''));
        atual = ''; i += n - 1; continue;
      }
    }
    atual += c;
  }
  if (atual.trim()) partes.push(atual);
  return partes.map((p) => p.trim()).filter(Boolean);
}
const semPontoFinal = (s) => s.replace(/\s+$/, '').replace(/\.$/, '');
const capitaliza = (s) => (s ? s[0].toUpperCase() + s.slice(1) : s);

// remove ** fora de `codigo` (negrito em excesso); mantem o texto
function semNegrito(linha) {
  return linha.split(/(`[^`]*`)/).map((seg, i) => (i % 2 === 1 ? seg : seg.replace(/\*\*/g, ''))).join('');
}
const escapaCelula = (s) => s.replace(/\|/g, '\\|');

// ---------- separa o documento em blocos ----------
// 1) corta o titulo H1 e a introducao antes do primeiro "---"
let i = 0;
const saida = [];
let emFence = false;

function ehFence(l) { return /^\s*```/.test(l); }

// ---------- converte um modulo ----------
const ROTULOS = ['Objetivo', 'Por quê', 'Decisões', 'Como', 'Pronto quando', 'Sabotagem', 'Armadilhas clássicas', 'Entregáveis'];
const reRotulo = new RegExp('^\\*\\*(' + ROTULOS.join('|') + ')\\.\\*\\*\\s*(.*)$');

function converteModulo(tituloLinha, linhas) {
  // acha as secoes (so fora de fences)
  const secoes = []; let corrente = null; let dentro = false;
  for (const l of linhas) {
    if (ehFence(l)) dentro = !dentro;
    const m = !dentro && !ehFence(l) ? reRotulo.exec(l) : null;
    if (m) { corrente = { rotulo: m[1], primeira: m[2], corpo: [] }; secoes.push(corrente); }
    else if (corrente) corrente.corpo.push(l);
  }
  const get = (r) => secoes.find((s) => s.rotulo === r);
  const texto = (s) => {
    if (!s) return '';
    const t = [s.primeira, ...s.corpo].join(' ').replace(/\s+/g, ' ').trim();
    return semNegrito(t);
  };
  const out = [];
  out.push('## ' + semNegrito(tituloLinha.replace(/^###\s*/, '')));
  out.push('');
  out.push('> **Objetivo:** ' + texto(get('Objetivo')));
  out.push('>');
  out.push('> **Por quê:** ' + texto(get('Por quê')));
  out.push('');

  const dec = get('Decisões');
  if (dec) {
    out.push('### Decisões'); out.push('');
    const partes = dividir(texto(dec), (t, k) => {
      if (t[k] === ';' && t[k + 1] === ' ') return 2;
      if (t[k] === '?' && t[k + 1] === ' ' && MAIUSCULA.test(t[k + 2] || '')) { return 2; }
      if (t[k] === '.' && t[k + 1] === ' ' && MAIUSCULA.test(t[k + 2] || '')) return 2;
      return 0;
    });
    if (partes.length >= 2) {
      for (const p of partes) out.push('- ' + capitaliza(semPontoFinal(p)));
    } else out.push(texto(dec));
    out.push('');
  }

  const como = get('Como');
  if (como) {
    out.push('### Como'); out.push('');
    if (como.primeira.trim()) { out.push(semNegrito(como.primeira)); out.push(''); }
    // corpo: remove negrito fora de fences; indenta fences que seguem item de lista numerada
    let fence = false; let ultimaNaoVazia = ''; let indentando = false; const corpo = [];
    for (const l of como.corpo) {
      if (ehFence(l)) {
        if (!fence) { fence = true; indentando = /^\d+\.\s|^ {3}\S/.test(ultimaNaoVazia); }
        else { fence = false; corpo.push((indentando ? '   ' : '') + l); indentando = false; continue; }
        corpo.push((indentando ? '   ' : '') + l); continue;
      }
      if (fence) { corpo.push((indentando ? '   ' : '') + l); continue; }
      if (l.trim()) ultimaNaoVazia = l;
      corpo.push(semNegrito(l));
    }
    while (corpo.length && !corpo[0].trim()) corpo.shift();
    while (corpo.length && !corpo[corpo.length - 1].trim()) corpo.pop();
    out.push(...corpo); out.push('');
  }

  const pronto = get('Pronto quando');
  if (pronto) {
    out.push('### Pronto quando'); out.push('');
    const itens = dividir(texto(pronto), (t, k) => {
      if (t[k] === ';' && t[k + 1] === ' ') return 2;
      if (t[k] === '.' && t[k + 1] === ' ' && MAIUSCULA.test(t[k + 2] || '')) return 2;
      return 0;
    });
    for (const it of itens) out.push('- [ ] ' + capitaliza(semPontoFinal(it)));
    out.push('');
  }

  const sab = get('Sabotagem');
  if (sab) { out.push('### Sabotagem'); out.push(''); out.push(texto(sab)); out.push(''); }

  const arm = get('Armadilhas clássicas');
  if (arm) {
    out.push('### Armadilhas'); out.push('');
    const linhasTab = []; const soltas = [];
    for (const l of arm.corpo) {
      if (!l.trim()) continue;
      const lim = semNegrito(l.replace(/^- /, ''));
      const m = /^\*Sintoma:\*\s*(.*?)\s*(?:\*Causa:\*\s*(.*?))?(?:\s*\*Correção:\*\s*(.*))?$/.exec(lim);
      if (m) linhasTab.push([m[1], m[2] || '—', m[3] || '—'].map((c) => escapaCelula(semPontoFinal(c.trim()))));
      else soltas.push('- ' + lim);
    }
    if (linhasTab.length) {
      out.push('| Sintoma | Causa | Correção |'); out.push('| --- | --- | --- |');
      for (const r of linhasTab) out.push('| ' + r.join(' | ') + ' |');
      out.push('');
    }
    if (soltas.length) { out.push(...soltas); out.push(''); }
  }

  const ent = get('Entregáveis');
  if (ent) {
    out.push('### Entregáveis'); out.push('');
    const itens = dividir(texto(ent), (t, k) => (t[k] === ',' && t[k + 1] === ' ' ? 2 : 0));
    for (const it of itens) out.push('- ' + capitaliza(semPontoFinal(it)));
    out.push('');
  }
  return out;
}

// ---------- varre o arquivo ----------
// pula o titulo H1 e a linha em branco
while (i < fonte.length && !fonte[i].startsWith('# ')) i++;
i++;
while (i < fonte.length && fonte[i].trim() === '') i++;

while (i < fonte.length) {
  const l = fonte[i];
  if (ehFence(l)) { // copia o bloco inteiro sem tocar
    saida.push(l); i++;
    while (i < fonte.length && !ehFence(fonte[i])) { saida.push(fonte[i]); i++; }
    if (i < fonte.length) { saida.push(fonte[i]); i++; }
    continue;
  }
  if (l.trim() === '---') { i++; continue; }

  if (/^### Módulo /.test(l)) { // coleta o modulo ate o proximo titulo (fora de fence)
    const bloco = []; let f = false; let j = i + 1;
    for (; j < fonte.length; j++) {
      if (ehFence(fonte[j])) f = !f;
      if (!f && /^(##|###) /.test(fonte[j]) && !ehFence(fonte[j])) break;
      if (fonte[j].trim() !== '---' || f) bloco.push(fonte[j]); // o separador entre módulos não entra no corpo
    }
    saida.push(...converteModulo(l, bloco));
    i = j; continue;
  }
  if (/^## /.test(l)) { saida.push('# ' + l.replace(/^## /, '')); i++; continue; }      // Partes e Blocos viram H1
  if (/^### /.test(l)) { saida.push('## ' + l.replace(/^### /, '')); i++; continue; }   // A1.., D1.. viram H2
  // linha inteira em negrito (subtitulo do runbook) vira H3
  const mb = /^\*\*([^*]+)\*\*$/.exec(l.trim());
  if (mb) { saida.push('### ' + mb[1]); i++; continue; }
  saida.push(l); i++;
}

// colapsa linhas em branco repetidas (fora de fences)
const final = []; let f2 = false;
for (const l of saida) {
  if (ehFence(l)) f2 = !f2;
  if (!f2 && l.trim() === '' && final.length && final[final.length - 1].trim() === '') continue;
  final.push(l);
}
fs.mkdirSync(saidaDir, { recursive: true });
const destino = path.join(saidaDir, NOME);
fs.writeFileSync(destino, final.join('\n').replace(/\s+$/, '') + '\n', 'utf8');
console.log('gerado: ' + destino + ' (' + final.length + ' linhas)');
