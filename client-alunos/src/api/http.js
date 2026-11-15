import { guardarTokens, lerAccessToken, lerRefreshToken } from '../auth/sessao'

// Erro com o status HTTP junto, para a tela decidir o que fazer (ex.: 401 = sessão expirada)
export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem)
    this.status = status
  }
}

// Troca o refresh token (dura 24 h) por um par novo de tokens. Devolve true se deu certo.
// Se várias chamadas recebem 401 ao mesmo tempo, só uma renovação acontece e todas esperam por ela
// (o refresh token vale uma vez só: duas renovações ao mesmo tempo derrubariam a sessão).
let renovacaoEmAndamento = null

export function renovarSessao() {
  if (!renovacaoEmAndamento) {
    renovacaoEmAndamento = tentarRenovar().finally(() => {
      renovacaoEmAndamento = null
    })
  }
  return renovacaoEmAndamento
}

async function tentarRenovar() {
  const refreshToken = lerRefreshToken()
  if (!refreshToken) return false
  try {
    const resposta = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!resposta.ok) return false
    guardarTokens(await resposta.json())
    return true
  } catch {
    return false // API fora do ar: tratamos como "não renovou"
  }
}

function enviar(url, opcoes) {
  return fetch(url, {
    ...opcoes,
    headers: { ...opcoes.headers, Authorization: `Bearer ${lerAccessToken()}` },
  })
}

// fetch que leva o token e, se a API responder 401 (token vencido, dura 15 min), renova a sessão e repete
// a chamada uma vez. Se nem a renovação funcionar, devolve o 401 e a tela manda o usuário para o login.
export async function requisicaoAutenticada(url, opcoes = {}) {
  const resposta = await enviar(url, opcoes)
  if (resposta.status === 401 && (await renovarSessao())) {
    return enviar(url, opcoes)
  }
  return resposta
}

// Lê a mensagem de erro que a API manda ({ status, erro, mensagem }) e a transforma em ErroApi.
export async function erroDaResposta(resposta, mensagemPadrao) {
  const corpo = await resposta.json().catch(() => null)
  return new ErroApi(corpo?.mensagem ?? mensagemPadrao, resposta.status)
}
