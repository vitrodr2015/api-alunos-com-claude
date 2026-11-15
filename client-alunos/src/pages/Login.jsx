import { useState } from 'react'
import { entrar } from '../api/auth'
import './Login.css'

// Um componente é uma função que devolve o que aparece na tela (JSX).
// "onLogin" é uma função que o componente pai (App) nos entrega; chamamos quando o login dá certo.
function Login({ onLogin }) {
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
      // localStorage é simples para estudar, mas fica exposto a XSS (veja a discussão sobre onde guardar o token)
      localStorage.setItem('accessToken', tokens.accessToken)
      localStorage.setItem('refreshToken', tokens.refreshToken)
      onLogin()
    } catch (e) {
      setErro(e.message)
    } finally {
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
