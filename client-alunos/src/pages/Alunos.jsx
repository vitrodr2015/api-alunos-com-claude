import { useEffect, useState } from 'react'
import { listarAlunos } from '../api/alunos'
import Paginacao from '../components/Paginacao'
import './Alunos.css'

const TAMANHOS_PAGINA = [10, 20, 50] // 50 é o máximo que a API aceita (acima disso ela reduz para 50)

// "01/12/2000" a partir de "2000-12-01". Não usamos new Date() de propósito: ele converteria
// o texto para UTC e, em fusos como o do Brasil, mostraria o dia anterior.
function formatarData(iso) {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

// "Ana Souza" -> "AS" (primeira letra do primeiro e do último nome)
function iniciais(nome) {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}

function Alunos({ onSair }) {
  const [pagina, setPagina] = useState(0) // número da página atual (começa em 0, como na API)
  const [tamanho, setTamanho] = useState(10) // alunos por página
  const [alunos, setAlunos] = useState([])
  const [info, setInfo] = useState(null) // { totalElements, totalPages, ... }
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  // useEffect roda depois de a tela ser desenhada, e de novo sempre que algo da lista [pagina] mudar.
  useEffect(() => {
    let cancelado = false // evita gravar o resultado de uma chamada antiga se o usuário já trocou de página

    async function carregar() {
      setCarregando(true)
      setErro('')
      try {
        const resultado = await listarAlunos({ page: pagina, size: tamanho })
        if (cancelado) return
        setAlunos(resultado.alunos)
        setInfo(resultado.pagina)
      } catch (e) {
        if (cancelado) return
        if (e.status === 401) {
          onSair() // token expirado (dura 15 min): volta para o login
          return
        }
        setErro(e.message)
      } finally {
        if (!cancelado) setCarregando(false)
      }
    }

    carregar()
    return () => {
      cancelado = true
    }
  }, [pagina, tamanho, onSair])

  const totalPaginas = info?.totalPages ?? 0

  // Ao mudar o tamanho, volta para a primeira página: a página 30 de 10 em 10 não existe mais de 50 em 50.
  function mudarTamanho(novoTamanho) {
    setTamanho(novoTamanho)
    setPagina(0)
  }

  // "Mostrando 11–20 de 404"
  const primeiro = info && info.totalElements > 0 ? pagina * tamanho + 1 : 0
  const ultimo = info ? Math.min((pagina + 1) * tamanho, info.totalElements) : 0

  return (
    <div className="alunos-pagina">
      <header className="alunos-cabecalho">
        <div>
          <h1>Alunos</h1>
          {info && <p className="alunos-subtitulo">{info.totalElements} alunos cadastrados</p>}
        </div>
        <button className="alunos-botao-sair" onClick={onSair}>
          Sair
        </button>
      </header>

      {erro && (
        <p className="alunos-erro" role="alert">
          {erro}
        </p>
      )}

      <div className="alunos-cartao">
        {/* durante o carregamento de uma nova página, a tabela fica levemente apagada em vez de sumir */}
        <table className={carregando ? 'alunos-tabela alunos-tabela-carregando' : 'alunos-tabela'}>
          <thead>
            <tr>
              <th className="alunos-col-id">ID</th>
              <th>Aluno</th>
              <th>Nascimento</th>
              <th>Situação</th>
            </tr>
          </thead>
          <tbody>
            {/* "Carregando..." só na primeira vez, quando ainda não há linhas para mostrar */}
            {carregando && alunos.length === 0 && (
              <tr>
                <td colSpan="4" className="alunos-aviso">
                  Carregando...
                </td>
              </tr>
            )}
            {!carregando && alunos.length === 0 && !erro && (
              <tr>
                <td colSpan="4" className="alunos-aviso">
                  Nenhum aluno encontrado.
                </td>
              </tr>
            )}
            {/* .map transforma cada aluno em uma linha da tabela; "key" ajuda o React a saber quem é quem.
                Ao trocar de página, as linhas antigas continuam na tela até as novas chegarem. */}
            {alunos.map((aluno) => (
              <tr key={aluno.id}>
                <td className="alunos-col-id">#{aluno.id}</td>
                <td>
                  <div className="alunos-pessoa">
                    <span className="alunos-avatar" aria-hidden="true">
                      {iniciais(aluno.nome)}
                    </span>
                    <div>
                      <div className="alunos-nome">{aluno.nome}</div>
                      <div className="alunos-email">{aluno.email}</div>
                    </div>
                  </div>
                </td>
                <td>{formatarData(aluno.dataNascimento)}</td>
                <td>
                  <span className={aluno.ativo ? 'alunos-selo alunos-selo-ativo' : 'alunos-selo'}>
                    {aluno.ativo ? 'Ativo' : 'Inativo'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {info && info.totalElements > 0 && (
          <footer className="alunos-rodape">
            <div className="alunos-rodape-esquerda">
              <span>
                Mostrando {primeiro}–{ultimo} de {info.totalElements}
              </span>
              <label className="alunos-tamanho">
                Por página
                <select value={tamanho} onChange={(evento) => mudarTamanho(Number(evento.target.value))}>
                  {TAMANHOS_PAGINA.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <Paginacao pagina={pagina} totalPaginas={totalPaginas} onMudar={setPagina} desabilitado={carregando} />
          </footer>
        )}
      </div>
    </div>
  )
}

export default Alunos
