// Funções que falam com a API de autenticação.
// O prefixo /api é resolvido pelo proxy do Vite (vite.config.js): /api/auth/signin -> http://localhost:8080/auth/signin

export async function entrar(username, password) {
  const resposta = await fetch('/api/auth/signin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (!resposta.ok) {
    // A API devolve { status, erro, mensagem } nos erros (ex.: 401 "Credenciais inválidas")
    const erro = await resposta.json().catch(() => null)
    throw new Error(erro?.mensagem ?? 'Não foi possível entrar. Tente novamente.')
  }

  // { tokenType, accessToken, accessExpiresAt, refreshToken, refreshExpiresAt }
  return resposta.json()
}
