import { useEffect, useRef, useState } from 'react'
import { excluirAluno } from '../api/alunos'
import './EditarAlunoModal.css' // usa o mesmo visual de popup do editar (editar-dialogo, editar-acoes...)

// Popup de confirmação antes de apagar um aluno. Apagar é irreversível, por isso nada é excluído sem este "sim".
function ExcluirAlunoModal({ aluno, onExcluido, onCancelar, onSessaoExpirada }) {
  const dialogoRef = useRef(null)
  const [excluindo, setExcluindo] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    const dialogo = dialogoRef.current
    dialogo.showModal()
    return () => dialogo.close()
  }, [])

  function cancelar() {
    if (!excluindo) onCancelar()
  }

  async function confirmar() {
    setErro('')
    setExcluindo(true)
    try {
      await excluirAluno(aluno.id)
      onExcluido()
    } catch (e) {
      if (e.status === 401) {
        onSessaoExpirada()
        return
      }
      // 403: o usuário não é ADMIN. 404: alguém já apagou. A mensagem vem da própria API.
      setErro(e.message)
      setExcluindo(false)
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      className="editar-dialogo"
      aria-labelledby="excluir-titulo"
      onCancel={(evento) => {
        evento.preventDefault()
        cancelar()
      }}
      onClick={(evento) => {
        if (evento.target === dialogoRef.current) cancelar()
      }}
    >
      <div className="editar-formulario">
        <h2 id="excluir-titulo">Excluir aluno</h2>
        <p className="editar-texto">
          Tem certeza que deseja excluir <strong>{aluno.nome}</strong>? Esta ação não pode ser desfeita.
        </p>

        {erro && (
          <p className="editar-erro" role="alert">
            {erro}
          </p>
        )}

        <div className="editar-acoes">
          {/* autoFocus no Cancelar: o botão seguro é o que recebe o Enter por engano */}
          <button type="button" className="editar-cancelar" onClick={cancelar} disabled={excluindo} autoFocus>
            Cancelar
          </button>
          <button type="button" className="editar-excluir" onClick={confirmar} disabled={excluindo}>
            {excluindo ? 'Excluindo...' : 'Excluir'}
          </button>
        </div>
      </div>
    </dialog>
  )
}

export default ExcluirAlunoModal
