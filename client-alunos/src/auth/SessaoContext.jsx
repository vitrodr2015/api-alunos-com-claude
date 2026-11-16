import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { guardarTokens, lerAccessToken, lerPerfil, limparTokens } from './sessao'

// Um "contexto" deixa qualquer componente ler a sessão (quem está logado, se é ADMIN, como sair) sem
// precisar receber isso por props em cada nível da árvore.
const SessaoContext = createContext(null)

export function SessaoProvider({ children }) {
  const [perfil, setPerfil] = useState(() => (lerAccessToken() ? lerPerfil() : null))

  // Depois do login: guarda os tokens e passa a saber quem está logado
  const entrar = useCallback((tokens) => {
    guardarTokens(tokens)
    setPerfil(lerPerfil())
  }, [])

  // useCallback mantém a mesma função entre redesenhos; as telas a usam como dependência de useEffect
  const sair = useCallback(() => {
    limparTokens()
    setPerfil(null)
  }, [])

  const valor = useMemo(
    () => ({ logado: perfil !== null, perfil, ehAdmin: perfil?.perfis.includes('ADMIN') ?? false, entrar, sair }),
    [perfil, entrar, sair],
  )

  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSessao() {
  const contexto = useContext(SessaoContext)
  if (!contexto) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return contexto
}
