// Funções que falam com a API de alunos. Toda chamada leva o token no cabeçalho Authorization.

// Erro com o status HTTP junto, para a tela decidir o que fazer (ex.: 401 = sessão expirada)
export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem)
    this.status = status
  }
}

// Altera nome, e-mail e data de nascimento (dataNascimento no formato "aaaa-mm-dd").
// A API valida e responde 400 (campos inválidos, com a lista na mensagem) ou 409 (e-mail já usado).
export async function atualizarAluno(id, { nome, email, dataNascimento }) {
  const token = localStorage.getItem('accessToken')
  const resposta = await fetch(`/api/alunos/${id}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ nome, email, dataNascimento }),
  })

  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new ErroApi(erro?.mensagem ?? 'Não foi possível alterar o aluno.', resposta.status)
  }
  return resposta.json()
}

// situacao: 'todos' | 'ativos' | 'inativos'  ->  ?ativo=true / ?ativo=false (sem o parâmetro = todos)
// campoOrdem: 'id' | 'nome' | 'dataNascimento' (nomes dos campos do aluno);  direcaoOrdem: 'asc' | 'desc'
export async function listarAlunos({
  page = 0,
  size = 10,
  nome = '',
  situacao = 'todos',
  campoOrdem = 'id',
  direcaoOrdem = 'asc',
} = {}) {
  const token = localStorage.getItem('accessToken')

  // URLSearchParams monta a query string e já escapa caracteres especiais (acentos, espaços, &...)
  const parametros = new URLSearchParams({ page, size, sort: `${campoOrdem},${direcaoOrdem}` })
  if (nome) parametros.set('nome', nome) // a API filtra por "nome contém o texto", sem diferenciar maiúsculas
  if (situacao !== 'todos') parametros.set('ativo', situacao === 'ativos')

  const resposta = await fetch(`/api/alunos?${parametros}`, {
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
