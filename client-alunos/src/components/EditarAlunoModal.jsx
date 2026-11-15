import { useEffect, useRef, useState } from 'react'
import { atualizarAluno } from '../api/alunos'
import './EditarAlunoModal.css'

// A data de nascimento precisa ser no passado: o máximo do campo é ontem (no fuso local, "aaaa-mm-dd").
function ontem() {
  const data = new Date()
  data.setDate(data.getDate() - 1)
  return data.toLocaleDateString('sv-SE')
}

// Popup para editar um aluno. Usa o elemento nativo <dialog>, que já traz o fundo escurecido,
// prende o foco dentro do popup e fecha com a tecla Esc.
//   aluno: o aluno sendo editado;  onSalvo: chamado depois de gravar;  onCancelar: chamado ao fechar sem gravar
function EditarAlunoModal({ aluno, onSalvo, onCancelar, onSessaoExpirada }) {
  const dialogoRef = useRef(null)
  const [nome, setNome] = useState(aluno.nome)
  const [email, setEmail] = useState(aluno.email)
  const [dataNascimento, setDataNascimento] = useState(aluno.dataNascimento)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  // Abre o popup assim que o componente aparece na tela
  useEffect(() => {
    const dialogo = dialogoRef.current
    dialogo.showModal()
    return () => dialogo.close()
  }, [])

  // Botão Alterar só vale quando algo realmente mudou
  const alterado =
    nome.trim() !== aluno.nome || email.trim() !== aluno.email || dataNascimento !== aluno.dataNascimento

  function cancelar() {
    if (!salvando) onCancelar() // não deixa fechar no meio de um salvamento
  }

  async function aoEnviar(evento) {
    evento.preventDefault()
    setErro('')
    setSalvando(true)
    try {
      await atualizarAluno(aluno.id, { nome: nome.trim(), email: email.trim(), dataNascimento })
      onSalvo()
    } catch (e) {
      if (e.status === 401) {
        onSessaoExpirada() // token expirado: volta para o login
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

        <div className="editar-campo">
          <label htmlFor="editar-email">E-mail</label>
          <input
            id="editar-email"
            type="email"
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
            required
          />
        </div>

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

        {erro && (
          <p className="editar-erro" role="alert">
            {erro}
          </p>
        )}

        <div className="editar-acoes">
          <button type="button" className="editar-cancelar" onClick={cancelar} disabled={salvando}>
            Cancelar
          </button>
          <button type="submit" className="editar-alterar" disabled={salvando || !alterado}>
            {salvando ? 'Alterando...' : 'Alterar'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

export default EditarAlunoModal
