// Tudo sobre a sessão do usuário: guardar e ler os tokens, e saber quem está logado.
// Os tokens ficam no localStorage: simples para estudar, mas expostos a XSS (script malicioso na página).

export function guardarTokens({ accessToken, refreshToken }) {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}

export function limparTokens() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

export function lerAccessToken() {
  return localStorage.getItem('accessToken')
}

export function lerRefreshToken() {
  return localStorage.getItem('refreshToken')
}

// O JWT tem três partes separadas por ponto: cabeçalho.corpo.assinatura. O corpo é JSON em base64url.
// Ler o corpo NÃO valida o token (quem valida é a API); serve só para mostrar o nome e esconder botões.
export function lerPerfil() {
  const token = lerAccessToken()
  if (!token) return null
  try {
    const corpo = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const dados = JSON.parse(atob(corpo))
    return { usuario: dados.sub, perfis: dados.roles ?? [] } // ex.: { usuario: 'admin', perfis: ['ADMIN', 'USER'] }
  } catch {
    return null
  }
}
