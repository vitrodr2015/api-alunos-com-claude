import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { buscarAluno } from '../api/alunos'
import { useSessao } from '../auth/SessaoContext'
import EditarAlunoModal from '../components/EditarAlunoModal'
import ExcluirAlunoModal from '../components/ExcluirAlunoModal'
import { calcularIdade, formatarData, iniciais } from '../utils/formatos'
import './AlunoDetalhe.css'

// Página de um aluno só (endereço /alunos/12). O número da URL vem de useParams().
function AlunoDetalhe() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { sair, ehAdmin } = useSessao()
  const [aluno, setAluno] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [editando, setEditando] = useState(false)
  const [excluindo, setExcluindo] = useState(false)
  const [versao, setVersao] = useState(0) // soma 1 para recarregar o aluno depois de editar

  useEffect(() => {
    let cancelado = false

    async function carregar() {
      setCarregando(true)
      setErro('')
      try {
        const dados = await buscarAluno(id)
        if (!cancelado) setAluno(dados)
      } catch (e) {
        if (cancelado) return
        if (e.status === 401) {
          sair() // nem a renovação do token funcionou: volta para o login
          return
        }
        setAluno(null)
        setErro(e.status === 404 ? 'Aluno não encontrado.' : e.message)
      } finally {
        if (!cancelado) setCarregando(false)
      }
    }

    carregar()
    return () => {
      cancelado = true
    }
  }, [id, versao, sair])

  return (
    <div className="detalhe-pagina">
      <Link className="detalhe-voltar" to="/alunos">
        ← Voltar para a lista
      </Link>

      {carregando && !aluno && <p className="detalhe-aviso">Carregando...</p>}

      {erro && (
        <p className="detalhe-erro" role="alert">
          {erro}
        </p>
      )}

      {aluno && (
        <div className="detalhe-cartao">
          <div className="detalhe-topo">
            <span className="detalhe-avatar" aria-hidden="true">
              {iniciais(aluno.nome)}
            </span>
            <div>
              <h1>{aluno.nome}</h1>
              <p className="detalhe-email">{aluno.email}</p>
            </div>
            <span className={aluno.ativo ? 'detalhe-selo detalhe-selo-ativo' : 'detalhe-selo'}>
              {aluno.ativo ? 'Ativo' : 'Inativo'}
            </span>
          </div>

          <dl className="detalhe-dados">
            <div>
              <dt>ID</dt>
              <dd>#{aluno.id}</dd>
            </div>
            <div>
              <dt>Data de nascimento</dt>
              <dd>{formatarData(aluno.dataNascimento)}</dd>
            </div>
            <div>
              <dt>Idade</dt>
              <dd>{calcularIdade(aluno.dataNascimento)} anos</dd>
            </div>
            <div>
              <dt>Situação</dt>
              <dd>{aluno.ativo ? 'Ativo' : 'Inativo'}</dd>
            </div>
          </dl>

          <div className="detalhe-acoes">
            <button className="detalhe-editar" onClick={() => setEditando(true)}>
              Editar
            </button>
            {ehAdmin && (
              <button className="detalhe-excluir" onClick={() => setExcluindo(true)}>
                Excluir
              </button>
            )}
          </div>
        </div>
      )}

      {editando && aluno && (
        <EditarAlunoModal
          key={aluno.id}
          aluno={aluno}
          onCancelar={() => setEditando(false)}
          onSalvo={() => {
            setEditando(false)
            setVersao((v) => v + 1) // recarrega o aluno para mostrar os dados novos
          }}
          onRecarregar={() => setVersao((v) => v + 1)}
          onSessaoExpirada={sair}
        />
      )}

      {excluindo && aluno && (
        <ExcluirAlunoModal
          aluno={aluno}
          onCancelar={() => setExcluindo(false)}
          onExcluido={() => navigate('/alunos', { replace: true })} // o aluno não existe mais: volta para a lista
          onSessaoExpirada={sair}
        />
      )}
    </div>
  )
}

export default AlunoDetalhe
