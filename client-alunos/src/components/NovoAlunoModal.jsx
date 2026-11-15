import { useEffect, useRef, useState } from 'react'
import { criarAluno } from '../api/alunos'
import { ontem } from '../utils/datas'
import { emailValido } from '../utils/validacoes'
import CampoEmail from './CampoEmail'
import './EditarAlunoModal.css' // usa o mesmo visual de popup do editar (editar-dialogo, editar-campo...)

// Popup para cadastrar um aluno novo (<dialog> nativo, como os outros).
//   onCriado(aluno): chamado com o aluno criado;  onCancelar: chamado ao fechar sem cadastrar
function NovoAlunoModal({ onCriado, onCancelar, onSessaoExpirada }) {
  const dialogoRef = useRef(null)
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [emailTocado, setEmailTocado] = useState(false) // true depois de sair do campo ou de tentar enviar
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    const dialogo = dialogoRef.current
    dialogo.showModal()
    return () => dialogo.close()
  }, [])

  const emailOk = emailValido(email)

  function cancelar() {
    if (!salvando) onCancelar()
  }

  async function aoEnviar(evento) {
    evento.preventDefault()
    if (!emailOk) {
      setEmailTocado(true) // mostra a mensagem e não envia
      return
    }
    setErro('')
    setSalvando(true)
    try {
      const criado = await criarAluno({ nome: nome.trim(), email: email.trim(), dataNascimento })
      onCriado(criado)
    } catch (e) {
      if (e.status === 401) {
        onSessaoExpirada()
        return
      }
      setErro(e.message) // ex.: 409 "Já existe um aluno cadastrado com este e-mail"
      setSalvando(false)
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      className="editar-dialogo"
      aria-labelledby="novo-titulo"
      onCancel={(evento) => {
        evento.preventDefault()
        cancelar()
      }}
      onClick={(evento) => {
        if (evento.target === dialogoRef.current) cancelar()
      }}
    >
      <form className="editar-formulario" onSubmit={aoEnviar}>
        <h2 id="novo-titulo">Novo aluno</h2>
        <p className="editar-id">O aluno é cadastrado como ativo.</p>

        <div className="editar-campo">
          <label htmlFor="novo-nome">Nome</label>
          <input
            id="novo-nome"
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
            minLength={3}
            maxLength={100}
            required
            autoFocus
          />
        </div>

        <CampoEmail
          id="novo-email"
          valor={email}
          onChange={setEmail}
          onBlur={() => setEmailTocado(true)}
          mostrarErro={emailTocado && email.trim() !== '' && !emailOk}
        />

        <div className="editar-campo">
          <label htmlFor="novo-nascimento">Data de nascimento</label>
          <input
            id="novo-nascimento"
            type="date"
            value={dataNascimento}
            onChange={(evento) => setDataNascimento(evento.target.value)}
            max={ontem()}
            required
          />
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
          <button type="submit" className="editar-alterar" disabled={salvando}>
            {salvando ? 'Cadastrando...' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

export default NovoAlunoModal
