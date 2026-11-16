import { useState } from 'react'
import { entrar } from '../api/auth'
import { useSessao } from '../auth/SessaoContext'
import './Login.css'

// Um componente é uma função que devolve o que aparece na tela (JSX).
// Quando o login dá certo, guardamos a sessão; o App percebe que há alguém logado e leva para /alunos.
function Login() {
  const sessao = useSessao()
  // useState guarda um valor e redesenha a tela quando ele muda: [valor, função para trocar o valor]
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function aoEnviar(evento) {
    evento.preventDefault() // sem isso o navegador recarregaria a página ao enviar o formulário
    setErro('')
    setCarregando(true)
    try {
      const tokens = await entrar(username, password)
      sessao.entrar(tokens) // guarda os tokens (localStorage) e passa a saber quem está logado
    } catch (e) {
      setErro(e.message)
      setCarregando(false)
    }
  }

  // className liga o elemento às regras do Login.css (em JSX é "className", não "class")
  return (
    <main className="login-pagina">
      <form className="login-cartao" onSubmit={aoEnviar}>
        <h1 className="login-titulo">Cadastro de Alunos</h1>
        <p className="login-subtitulo">Entre com seu usuário e senha</p>

        <div className="login-campo">
          <label htmlFor="username">Usuário</label>
          <input
            id="username"
            value={username}
            onChange={(evento) => setUsername(evento.target.value)}
            autoComplete="username"
            required
          />
        </div>

        <div className="login-campo">
          <label htmlFor="password">Senha</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(evento) => setPassword(evento.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        {erro && (
          <p className="login-erro" role="alert">
            {erro}
          </p>
        )}

        <button className="login-botao" type="submit" disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}

export default Login
