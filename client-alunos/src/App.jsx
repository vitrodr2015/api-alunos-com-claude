import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { SessaoProvider, useSessao } from './auth/SessaoContext'
import Layout from './components/Layout'
import AlunoDetalhe from './pages/AlunoDetalhe'
import Alunos from './pages/Alunos'
import Login from './pages/Login'
import NovoUsuario from './pages/NovoUsuario'

// Só deixa passar quem está logado; senão manda para o /login. "somenteAdmin" exige também o perfil ADMIN.
function RotaProtegida({ somenteAdmin = false, children }) {
  const { logado, ehAdmin } = useSessao()
  if (!logado) return <Navigate to="/login" replace />
  if (somenteAdmin && !ehAdmin) return <Navigate to="/alunos" replace />
  return children
}

function RotaLogin() {
  const { logado } = useSessao()
  return logado ? <Navigate to="/alunos" replace /> : <Login />
}

// O mapa de endereços da aplicação: qual página aparece em cada URL.
function App() {
  return (
    <SessaoProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<RotaLogin />} />

          {/* as rotas dentro deste grupo aparecem dentro do Layout (barra de cima) e exigem login */}
          <Route
            element={
              <RotaProtegida>
                <Layout />
              </RotaProtegida>
            }
          >
            <Route path="/alunos" element={<Alunos />} />
            <Route path="/alunos/:id" element={<AlunoDetalhe />} />
            <Route
              path="/usuarios/novo"
              element={
                <RotaProtegida somenteAdmin>
                  <NovoUsuario />
                </RotaProtegida>
              }
            />
          </Route>

          {/* qualquer outro endereço vai para a lista */}
          <Route path="*" element={<Navigate to="/alunos" replace />} />
        </Routes>
      </BrowserRouter>
    </SessaoProvider>
  )
}

export default App
