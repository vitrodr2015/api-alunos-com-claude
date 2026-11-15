import { useEffect, useRef, useState } from 'react'
import { alterarSituacao, atualizarAluno } from '../api/alunos'
import { ontem } from '../utils/datas'
import { emailValido } from '../utils/validacoes'
import CampoEmail from './CampoEmail'
import './EditarAlunoModal.css'

// Popup para editar um aluno. Usa o elemento nativo <dialog>, que já traz o fundo escurecido,
// prende o foco dentro do popup e fecha com a tecla Esc.
//   aluno: o aluno sendo editado;  onSalvo: chamado depois de gravar;  onCancelar: chamado ao fechar sem gravar
//   onRecarregar: pede à lista para recarregar sem fechar o popup (usado quando só uma parte foi gravada)
function EditarAlunoModal({ aluno, onSalvo, onCancelar, onRecarregar, onSessaoExpirada }) {
  const dialogoRef = useRef(null)
  // Os dados "de partida" para comparar com o que foi digitado. Mudam se uma parte da gravação der certo.
  const [original, setOriginal] = useState(aluno)
  const [nome, setNome] = useState(aluno.nome)
  const [email, setEmail] = useState(aluno.email)
  const [dataNascimento, setDataNascimento] = useState(aluno.dataNascimento)
  const [ativo, setAtivo] = useState(aluno.ativo)
  const [emailTocado, setEmailTocado] = useState(false) // true depois de sair do campo ou de tentar enviar
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  // Abre o popup assim que o componente aparece na tela
  useEffect(() => {
    const dialogo = dialogoRef.current
    dialogo.showModal()
    return () => dialogo.close()
  }, [])

  const dadosAlterados =
    nome.trim() !== original.nome || email.trim() !== original.email || dataNascimento !== original.dataNascimento
  const situacaoAlterada = ativo !== original.ativo

  const emailOk = emailValido(email)

  function cancelar() {
    if (!salvando) onCancelar() // não deixa fechar no meio de um salvamento
  }

  // A API tem duas rotas diferentes: PUT para nome, e-mail e data; PATCH para ativar ou desativar.
  // Então gravamos em até duas chamadas, uma depois da outra.
  async function aoEnviar(evento) {
    evento.preventDefault()
    if (!emailOk) {
      setEmailTocado(true) // mostra a mensagem e não envia
      return
    }
    setErro('')
    setSalvando(true)
    let gravouAlgo = false
    try {
      if (dadosAlterados) {
        await atualizarAluno(original.id, { nome: nome.trim(), email: email.trim(), dataNascimento })
        setOriginal((o) => ({ ...o, nome: nome.trim(), email: email.trim(), dataNascimento }))
        gravouAlgo = true
      }
      if (situacaoAlterada) {
        await alterarSituacao(original.id, ativo)
      }
      onSalvo()
    } catch (e) {
      if (e.status === 401) {
        onSessaoExpirada() // token expirado: volta para o login
        return
      }
      // Se os dados já foram gravados e só a situação falhou, avisa e atualiza a lista por trás do popup
      if (gravouAlgo) {
        setErro(`Os dados foram salvos, mas a situação não mudou: ${e.message}`)
        onRecarregar()
      } else {
        setErro(e.message) // ex.: 409 "Já existe um aluno cadastrado com este e-mail"
      }
      setSalvando(false)
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      className="editar-dialogo"
      aria-labelledby="editar-titulo"
      onCancel={(evento) => {
        evento.preventDefault() // Esc: quem fecha somos nós, para respeitar o "salvando"
        cancelar()
      }}
      onClick={(evento) => {
        // clique no fundo escurecido (o alvo é o próprio <dialog>, não algo dentro dele) fecha o popup
        if (evento.target === dialogoRef.current) cancelar()
      }}
    >
      <form className="editar-formulario" onSubmit={aoEnviar}>
        <h2 id="editar-titulo">Editar aluno</h2>
        <p className="editar-id">#{aluno.id}</p>

        <div className="editar-campo">
          <label htmlFor="editar-nome">Nome</label>
          <input
            id="editar-nome"
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
            minLength={3}
            maxLength={100}
            required
            autoFocus
          />
        </div>

        <CampoEmail
          id="editar-email"
          valor={email}
          onChange={setEmail}
          onBlur={() => setEmailTocado(true)}
          mostrarErro={emailTocado && email.trim() !== '' && !emailOk}
        />

        <div className="editar-campo">
          <label htmlFor="editar-nascimento">Data de nascimento</label>
          <input
            id="editar-nascimento"
            type="date"
            value={dataNascimento}
            onChange={(evento) => setDataNascimento(evento.target.value)}
            max={ontem()}
            required
          />
        </div>

        <div className="editar-campo">
          <label htmlFor="editar-situacao">Situação</label>
          {/* o select trabalha com texto ("ativo"/"inativo"); convertemos de e para o boolean que a API usa */}
          <select
            id="editar-situacao"
            value={ativo ? 'ativo' : 'inativo'}
            onChange={(evento) => setAtivo(evento.target.value === 'ativo')}
          >
            <option value="ativo">Ativo</option>
            <option value="inativo">Inativo</option>
          </select>
        </div>

        {erro && (
          <p className="editar-erro" role="alert">
            {erro}
          </p>
        )}

        <div className="editar-acoes">
          <button type="button" className="editar-cancelar" onClick={cancelar} disabled={salvando}>
            Cancelar
          </button>
          <button type="submit" className="editar-alterar" disabled={salvando || !(dadosAlterados || situacaoAlterada)}>
            {salvando ? 'Alterando...' : 'Alterar'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

export default EditarAlunoModal
