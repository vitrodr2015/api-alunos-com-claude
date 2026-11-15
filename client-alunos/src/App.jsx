import { useState } from 'react'
import Login from './pages/Login'

function App() {
  // Já logado se existir um token guardado (sobrevive ao F5 porque está no localStorage)
  const [logado, setLogado] = useState(() => localStorage.getItem('accessToken') !== null)

  function sair() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setLogado(false)
  }

  if (!logado) {
    return <Login onLogin={() => setLogado(true)} />
  }

  return (
    <div>
      <h1>Você está logado</h1>
      <button onClick={sair}>Sair</button>
    </div>
  )
}

export default App
