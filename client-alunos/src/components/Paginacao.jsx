import './Paginacao.css'

// Decide quais botões mostrar. Sempre a primeira e a última página, mais a atual e uma vizinha de cada lado;
// os buracos viram "…". Ex.: página 20 de 41 -> [0, '…', 19, 20, 21, '…', 40]  (números começam em 0, como na API)
function paginasVisiveis(atual, total) {
  const itens = []
  let anterior = -1
  for (let p = 0; p < total; p++) {
    const mostrar = p === 0 || p === total - 1 || Math.abs(p - atual) <= 1
    if (!mostrar) continue
    if (p - anterior > 1) itens.push('…') // pulou páginas: marca o buraco
    itens.push(p)
    anterior = p
  }
  return itens
}

// Um componente que não sabe nada de alunos: recebe a página atual, o total e avisa quando o usuário escolhe outra.
function Paginacao({ pagina, totalPaginas, onMudar, desabilitado = false }) {
  if (totalPaginas <= 1) return null

  return (
    <nav className="paginacao" aria-label="Paginação">
      <button
        className="paginacao-seta"
        onClick={() => onMudar(pagina - 1)}
        disabled={pagina === 0 || desabilitado}
        aria-label="Página anterior"
      >
        ‹
      </button>

      {paginasVisiveis(pagina, totalPaginas).map((item, indice) =>
        item === '…' ? (
          // key precisa ser única: o índice serve aqui porque a lista é recalculada a cada troca de página
          <span key={`buraco-${indice}`} className="paginacao-buraco">
            …
          </span>
        ) : (
          <button
            key={item}
            className={item === pagina ? 'paginacao-numero paginacao-atual' : 'paginacao-numero'}
            onClick={() => onMudar(item)}
            disabled={desabilitado}
            aria-current={item === pagina ? 'page' : undefined}
          >
            {item + 1}
          </button>
        ),
      )}

      <button
        className="paginacao-seta"
        onClick={() => onMudar(pagina + 1)}
        disabled={pagina + 1 >= totalPaginas || desabilitado}
        aria-label="Próxima página"
      >
        ›
      </button>
    </nav>
  )
}

export default Paginacao
