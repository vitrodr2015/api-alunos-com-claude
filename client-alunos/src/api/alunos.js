// Funções que falam com a API de alunos. Toda chamada leva o token no cabeçalho Authorization.

// Erro com o status HTTP junto, para a tela decidir o que fazer (ex.: 401 = sessão expirada)
export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem)
    this.status = status
  }
}

export async function listarAlunos({ page = 0, size = 10 } = {}) {
  const token = localStorage.getItem('accessToken')
  const resposta = await fetch(`/api/alunos?page=${page}&size=${size}`, {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new ErroApi(erro?.mensagem ?? 'Não foi possível carregar os alunos.', resposta.status)
  }

  // A API devolve HAL: os alunos ficam em _embedded.alunos e a paginação em page.
  // Quando a lista está vazia o Spring omite o _embedded, por isso o "?? []".
  const dados = await resposta.json()
  return {
    alunos: dados._embedded?.alunos ?? [],
    pagina: dados.page, // { number, size, totalElements, totalPages }
  }
}
