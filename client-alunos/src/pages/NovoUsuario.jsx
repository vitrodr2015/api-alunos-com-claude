import { useState } from 'react'
import { registrarUsuario } from '../api/auth'
import { useSessao } from '../auth/SessaoContext'
import './NovoUsuario.css'

// Cadastro de usuários de acesso ao sistema (POST /auth/register). Só ADMIN chega aqui.
function NovoUsuario() {
  const { sair } = useSessao()
  const [username, setUsername] = useState('')
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  async function aoEnviar(evento) {
    evento.preventDefault()
    setErro('')
    setSucesso('')

    // A confirmação existe só no client: a API recebe apenas a senha
    if (password !== confirmacao) {
      setErro('A confirmação da senha não confere.')
      return
    }

    setSalvando(true)
    try {
      const criado = await registrarUsuario({ username: username.trim(), fullName: fullName.trim(), password })
      setSucesso(`Usuário “${criado.username}” criado com os perfis: ${criado.permissions.join(', ') || 'nenhum'}.`)
      setUsername('')
      setFullName('')
      setPassword('')
      setConfirmacao('')
    } catch (e) {
      if (e.status === 401) {
        sair()
        return
      }
      setErro(e.message) // ex.: 409 (nome de usuário já existe) ou a lista de campos inválidos (400)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="usuario-pagina">
      <h1>Novo usuário</h1>
      <p className="usuario-subtitulo">Cria uma conta para acessar este sistema.</p>

      <form className="usuario-cartao" onSubmit={aoEnviar}>
        <div className="usuario-campo">
          <label htmlFor="usuario-login">Nome de usuário</label>
          <input
            id="usuario-login"
            value={username}
            onChange={(evento) => setUsername(evento.target.value)}
            minLength={3}
            maxLength={100}
            pattern="[A-Za-z0-9._@\-]+"
            title="Letras, números e os símbolos . _ @ -"
            autoComplete="off"
            required
          />
          <span className="usuario-dica">3 a 100 caracteres: letras, números e . _ @ -</span>
        </div>

        <div className="usuario-campo">
          <label htmlFor="usuario-nome">Nome completo</label>
          <input
            id="usuario-nome"
            value={fullName}
            onChange={(evento) => setFullName(evento.target.value)}
            minLength={3}
            maxLength={150}
            required
          />
        </div>

        <div className="usuario-campo">
          <label htmlFor="usuario-senha">Senha</label>
          <input
            id="usuario-senha"
            type="password"
            value={password}
            onChange={(evento) => setPassword(evento.target.value)}
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            required
          />
          <span className="usuario-dica">De 8 a 72 caracteres.</span>
        </div>

        <div className="usuario-campo">
          <label htmlFor="usuario-confirmacao">Confirmar senha</label>
          <input
            id="usuario-confirmacao"
            type="password"
            value={confirmacao}
            onChange={(evento) => setConfirmacao(evento.target.value)}
            autoComplete="new-password"
            required
          />
        </div>

        {sucesso && (
          <p className="usuario-sucesso" role="status">
            {sucesso}
          </p>
        )}
        {erro && (
          <p className="usuario-erro" role="alert">
            {erro}
          </p>
        )}

        <button className="usuario-botao" type="submit" disabled={salvando}>
          {salvando ? 'Cadastrando...' : 'Cadastrar usuário'}
        </button>
      </form>
    </div>
  )
}

export default NovoUsuario
