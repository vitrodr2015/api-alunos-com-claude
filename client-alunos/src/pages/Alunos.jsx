import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { alterarSituacao, listarAlunos } from '../api/alunos'
import { useSessao } from '../auth/SessaoContext'
import EditarAlunoModal from '../components/EditarAlunoModal'
import ExcluirAlunoModal from '../components/ExcluirAlunoModal'
import NovoAlunoModal from '../components/NovoAlunoModal'
import Paginacao from '../components/Paginacao'
import { formatarData, iniciais } from '../utils/formatos'
import './Alunos.css'

const TAMANHOS_PAGINA = [10, 20, 50] // 50 é o máximo que a API aceita (acima disso ela reduz para 50)

const SITUACOES = [
  { valor: 'todos', rotulo: 'Todos' },
  { valor: 'ativos', rotulo: 'Ativos' },
  { valor: 'inativos', rotulo: 'Inativos' },
]

const ORDEM_PADRAO = { campo: 'id', direcao: 'asc' }

// Cabeçalho de coluna clicável: mostra ▲ ou ▼ na coluna que está ordenando a tabela.
function CabecalhoOrdenavel({ campo, ordem, onOrdenar, className, children }) {
  const ativo = ordem.campo === campo
  const aria = ativo ? (ordem.direcao === 'asc' ? 'ascending' : 'descending') : 'none'
  return (
    <th className={className} aria-sort={aria}>
      <button className="alunos-ordenar" onClick={() => onOrdenar(campo)}>
        {children}
        <span className={ativo ? 'alunos-seta alunos-seta-ativa' : 'alunos-seta'} aria-hidden="true">
          {ativo ? (ordem.direcao === 'asc' ? '▲' : '▼') : '↕'}
        </span>
      </button>
    </th>
  )
}

function Alunos() {
  // sair: encerra a sessão (usado quando a API devolve 401 e nem a renovação do token funciona)
  // ehAdmin: só ADMIN pode excluir (a API responderia 403 para os demais)
  const { sair: onSair, ehAdmin } = useSessao()
  const [alternandoId, setAlternandoId] = useState(null) // id do aluno cuja situação está sendo trocada na linha
  const [pagina, setPagina] = useState(0) // número da página atual (começa em 0, como na API)
  const [tamanho, setTamanho] = useState(10) // alunos por página
  const [busca, setBusca] = useState('') // o que está digitado no campo (muda a cada tecla)
  const [nome, setNome] = useState('') // o termo realmente usado na busca (só muda depois de uma pausa na digitação)
  const [situacao, setSituacao] = useState('todos') // 'todos' | 'ativos' | 'inativos'
  const [ordem, setOrdem] = useState(ORDEM_PADRAO) // { campo, direcao }
  const [alunoEditando, setAlunoEditando] = useState(null) // o aluno cujo popup está aberto (null = fechado)
  const [alunoExcluindo, setAlunoExcluindo] = useState(null) // o aluno cuja confirmação de exclusão está aberta
  const [cadastrando, setCadastrando] = useState(false) // popup de novo aluno aberto?
  const [mensagem, setMensagem] = useState('') // aviso de sucesso (ex.: "Aluno cadastrado"), some sozinho
  const [versao, setVersao] = useState(0) // soma 1 para forçar o recarregamento da lista (ex.: depois de editar)
  const [alunos, setAlunos] = useState([])
  const [info, setInfo] = useState(null) // { totalElements, totalPages, ... }
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  // O aviso de sucesso desaparece depois de 5 segundos
  useEffect(() => {
    if (!mensagem) return
    const temporizador = setTimeout(() => setMensagem(''), 5000)
    return () => clearTimeout(temporizador)
  }, [mensagem])

  // Debounce: espera 400 ms sem digitar antes de buscar. Sem isso, cada tecla dispararia uma chamada à API.
  // O return limpa o temporizador anterior; se o usuário digitar de novo antes dos 400 ms, a contagem recomeça.
  useEffect(() => {
    const temporizador = setTimeout(() => {
      setNome(busca.trim())
      setPagina(0) // um novo filtro sempre começa na primeira página
    }, 400)
    return () => clearTimeout(temporizador)
  }, [busca])

  // useEffect roda depois de a tela ser desenhada, e de novo sempre que algo da lista de dependências mudar.
  useEffect(() => {
    let cancelado = false // evita gravar o resultado de uma chamada antiga se o usuário já trocou de página

    async function carregar() {
      setCarregando(true)
      setErro('')
      try {
        const resultado = await listarAlunos({
          page: pagina,
          size: tamanho,
          nome,
          situacao,
          campoOrdem: ordem.campo,
          direcaoOrdem: ordem.direcao,
        })
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
  }, [pagina, tamanho, nome, situacao, ordem, versao, onSair])

  const totalPaginas = info?.totalPages ?? 0

  // Ao mudar o tamanho, volta para a primeira página: a página 30 de 10 em 10 não existe mais de 50 em 50.
  function mudarTamanho(novoTamanho) {
    setTamanho(novoTamanho)
    setPagina(0)
  }

  function mudarSituacao(novaSituacao) {
    setSituacao(novaSituacao)
    setPagina(0)
  }

  // Clicar numa coluna: se já é a coluna da ordenação, inverte o sentido; se é outra, começa em A→Z (crescente).
  function ordenarPor(campo) {
    setOrdem((atual) =>
      atual.campo === campo
        ? { campo, direcao: atual.direcao === 'asc' ? 'desc' : 'asc' }
        : { campo, direcao: 'asc' },
    )
    setPagina(0)
  }

  const ordemPadrao = ordem.campo === ORDEM_PADRAO.campo && ordem.direcao === ORDEM_PADRAO.direcao
  const temFiltro = busca !== '' || situacao !== 'todos' || !ordemPadrao

  function limparFiltros() {
    setBusca('')
    setNome('') // sem esperar o debounce de 400 ms
    setSituacao('todos')
    setOrdem(ORDEM_PADRAO)
    setPagina(0)
  }

  // Interruptor da linha: troca a situação e recarrega a lista (se o filtro for "Ativos", a linha some, como esperado)
  async function alternarSituacao(aluno) {
    setErro('')
    setAlternandoId(aluno.id)
    try {
      await alterarSituacao(aluno.id, !aluno.ativo)
      setVersao((v) => v + 1)
    } catch (e) {
      if (e.status === 401) {
        onSair()
        return
      }
      setErro(e.message)
    } finally {
      setAlternandoId(null)
    }
  }

  // "Mostrando 11–20 de 404"
  const primeiro = info && info.totalElements > 0 ? pagina * tamanho + 1 : 0
  const ultimo = info ? Math.min((pagina + 1) * tamanho, info.totalElements) : 0

  // Texto do subtítulo: "404 alunos cadastrados" ou, com filtro, "61 resultados para “ana” · ativos"
  let subtitulo = ''
  if (info) {
    if (nome || situacao !== 'todos') {
      subtitulo = `${info.totalElements} ${info.totalElements === 1 ? 'resultado' : 'resultados'}`
      if (nome) subtitulo += ` para “${nome}”`
      if (situacao !== 'todos') subtitulo += ` · ${situacao}`
    } else {
      subtitulo = `${info.totalElements} alunos cadastrados`
    }
  }

  return (
    <div className="alunos-pagina">
      <header className="alunos-cabecalho">
        <div>
          <h1>Alunos</h1>
          {info && <p className="alunos-subtitulo">{subtitulo}</p>}
        </div>
        <div className="alunos-cabecalho-acoes">
          <button className="alunos-botao-novo" onClick={() => setCadastrando(true)}>
            + Novo aluno
          </button>
        </div>
      </header>

      {mensagem && (
        <p className="alunos-sucesso" role="status">
          {mensagem}
        </p>
      )}

      {erro && (
        <p className="alunos-erro" role="alert">
          {erro}
        </p>
      )}

      <div className="alunos-filtros">
        <div className="alunos-busca">
          <input
            type="search"
            value={busca}
            onChange={(evento) => setBusca(evento.target.value)}
            placeholder="Buscar pelo início do nome"
            aria-label="Buscar aluno pelo início do nome"
          />
          {busca && (
            <button className="alunos-busca-limpar" onClick={() => setBusca('')} aria-label="Limpar busca">
              ×
            </button>
          )}
        </div>

        <div className="alunos-situacao" role="group" aria-label="Filtrar por situação">
          {SITUACOES.map((s) => (
            <button
              key={s.valor}
              className={situacao === s.valor ? 'alunos-situacao-ativa' : ''}
              onClick={() => mudarSituacao(s.valor)}
              aria-pressed={situacao === s.valor}
            >
              {s.rotulo}
            </button>
          ))}
        </div>

        {temFiltro && (
          <button className="alunos-limpar-filtros" onClick={limparFiltros}>
            Limpar filtros
          </button>
        )}
      </div>

      <div className="alunos-cartao">
        {/* durante o carregamento de uma nova página, a tabela fica levemente apagada em vez de sumir */}
        <table className={carregando ? 'alunos-tabela alunos-tabela-carregando' : 'alunos-tabela'}>
          <thead>
            <tr>
              <CabecalhoOrdenavel campo="id" ordem={ordem} onOrdenar={ordenarPor} className="alunos-col-id">
                ID
              </CabecalhoOrdenavel>
              <CabecalhoOrdenavel campo="nome" ordem={ordem} onOrdenar={ordenarPor}>
                Aluno
              </CabecalhoOrdenavel>
              <CabecalhoOrdenavel campo="dataNascimento" ordem={ordem} onOrdenar={ordenarPor}>
                Nascimento
              </CabecalhoOrdenavel>
              <th>Situação</th>
              <th className="alunos-col-acoes">Ações</th>
            </tr>
          </thead>
          <tbody>
            {/* "Carregando..." só na primeira vez, quando ainda não há linhas para mostrar */}
            {carregando && alunos.length === 0 && (
              <tr>
                <td colSpan="5" className="alunos-aviso">
                  Carregando...
                </td>
              </tr>
            )}
            {!carregando && alunos.length === 0 && !erro && (
              <tr>
                <td colSpan="5" className="alunos-aviso">
                  {nome ? `Nenhum aluno encontrado para “${nome}”.` : 'Nenhum aluno encontrado.'}
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
                      {/* Link troca de página sem recarregar o navegador: /alunos/12 mostra o detalhe */}
                      <Link className="alunos-nome" to={`/alunos/${aluno.id}`}>
                        {aluno.nome}
                      </Link>
                      <div className="alunos-email">{aluno.email}</div>
                    </div>
                  </div>
                </td>
                <td>{formatarData(aluno.dataNascimento)}</td>
                <td>
                  <div className="alunos-situacao-celula">
                    {/* interruptor: clicar ativa ou desativa o aluno na hora (PATCH /enable ou /disable) */}
                    <button
                      role="switch"
                      aria-checked={aluno.ativo}
                      aria-label={`${aluno.ativo ? 'Desativar' : 'Ativar'} ${aluno.nome}`}
                      className={aluno.ativo ? 'alunos-switch alunos-switch-ligado' : 'alunos-switch'}
                      onClick={() => alternarSituacao(aluno)}
                      disabled={alternandoId === aluno.id}
                    >
                      <span className="alunos-switch-bolinha" />
                    </button>
                    <span className={aluno.ativo ? 'alunos-selo alunos-selo-ativo' : 'alunos-selo'}>
                      {aluno.ativo ? 'Ativo' : 'Inativo'}
                    </span>
                  </div>
                </td>
                <td className="alunos-col-acoes">
                  <button
                    className="alunos-botao-editar"
                    onClick={() => setAlunoEditando(aluno)}
                    aria-label={`Editar ${aluno.nome}`}
                  >
                    Editar
                  </button>
                  {/* só ADMIN vê o Excluir: a API recusaria (403) quem não tem esse perfil */}
                  {ehAdmin && (
                    <button
                      className="alunos-botao-excluir"
                      onClick={() => setAlunoExcluindo(aluno)}
                      aria-label={`Excluir ${aluno.nome}`}
                    >
                      Excluir
                    </button>
                  )}
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

      {/* O popup só existe na tela enquanto houver um aluno sendo editado. "key" garante campos novos a cada aluno. */}
      {alunoEditando && (
        <EditarAlunoModal
          key={alunoEditando.id}
          aluno={alunoEditando}
          onCancelar={() => setAlunoEditando(null)}
          onSalvo={() => {
            setAlunoEditando(null)
            setVersao((v) => v + 1) // recarrega a página atual para mostrar os dados alterados
          }}
          onRecarregar={() => setVersao((v) => v + 1)}
          onSessaoExpirada={onSair}
        />
      )}

      {cadastrando && (
        <NovoAlunoModal
          onCancelar={() => setCadastrando(false)}
          onCriado={(criado) => {
            setCadastrando(false)
            setMensagem(`Aluno “${criado.nome}” cadastrado com sucesso (#${criado.id}).`)
            setVersao((v) => v + 1) // recarrega a lista; o aluno novo tem o maior id (última página na ordem padrão)
          }}
          onSessaoExpirada={onSair}
        />
      )}

      {alunoExcluindo && (
        <ExcluirAlunoModal
          key={alunoExcluindo.id}
          aluno={alunoExcluindo}
          onCancelar={() => setAlunoExcluindo(null)}
          onExcluido={() => {
            setAlunoExcluindo(null)
            // Se era o único aluno da última página, essa página deixa de existir: volta uma página.
            // Senão, só recarrega a página atual.
            if (alunos.length === 1 && pagina > 0) setPagina(pagina - 1)
            else setVersao((v) => v + 1)
          }}
          onSessaoExpirada={onSair}
        />
      )}
    </div>
  )
}

export default Alunos
