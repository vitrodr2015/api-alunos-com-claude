// Funções que falam com a API de autenticação.
// O prefixo /api é resolvido pelo proxy do Vite (vite.config.js): /api/auth/signin -> http://localhost:8080/auth/signin
import { erroDaResposta, requisicaoAutenticada } from './http'

export async function entrar(username, password) {
  const resposta = await fetch('/api/auth/signin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível entrar. Tente novamente.')

  // { tokenType, accessToken, accessExpiresAt, refreshToken, refreshExpiresAt }
  return resposta.json()
}

// Cria um usuário de acesso ao sistema. Só quem tem perfil ADMIN pode (os demais recebem 403).
// Erros: 400 (campos inválidos, com a lista na mensagem) e 409 (nome de usuário já existe).
export async function registrarUsuario({ username, fullName, password }) {
  const resposta = await requisicaoAutenticada('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, fullName, password }),
  })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível cadastrar o usuário.')
  return resposta.json() // { id, username, fullName, enabled, permissions }
}
