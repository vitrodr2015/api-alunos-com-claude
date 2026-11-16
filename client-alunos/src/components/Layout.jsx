import { NavLink, Outlet } from 'react-router'
import { useSessao } from '../auth/SessaoContext'
import './Layout.css'

// Moldura das páginas logadas: barra de cima com o menu e o usuário, e embaixo a página da vez (<Outlet />).
function Layout() {
  const { perfil, ehAdmin, sair } = useSessao()

  return (
    <>
      <header className="topo">
        <div className="topo-conteudo">
          <span className="topo-marca">Cadastro de Alunos</span>

          <nav className="topo-menu">
            {/* NavLink sabe qual rota está ativa e põe a classe "ativo" no link da página atual */}
            <NavLink to="/alunos" className={({ isActive }) => (isActive ? 'ativo' : '')}>
              Alunos
            </NavLink>
            {ehAdmin && (
              <NavLink to="/usuarios/novo" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                Novo usuário
              </NavLink>
            )}
          </nav>

          <div className="topo-usuario">
            <span className="topo-nome">
              {perfil.usuario}
              {ehAdmin && <span className="topo-selo">ADMIN</span>}
            </span>
            <button className="topo-sair" onClick={sair}>
              Sair
            </button>
          </div>
        </div>
      </header>

      <Outlet />
    </>
  )
}

export default Layout
