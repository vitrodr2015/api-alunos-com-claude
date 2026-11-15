import { useCallback, useState } from 'react'
import Login from './pages/Login'
import Alunos from './pages/Alunos'

function App() {
  // Já logado se existir um token guardado (sobrevive ao F5 porque está no localStorage)
  const [logado, setLogado] = useState(() => localStorage.getItem('accessToken') !== null)

  // useCallback mantém a mesma função entre redesenhos; a tela de alunos a usa como dependência do useEffect
  const sair = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setLogado(false)
  }, [])

  if (!logado) {
    return <Login onLogin={() => setLogado(true)} />
  }

  return <Alunos onSair={sair} />
}

export default App
