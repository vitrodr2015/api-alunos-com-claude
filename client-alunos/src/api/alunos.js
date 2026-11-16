// Funções que falam com a API de alunos. Todas usam requisicaoAutenticada: levam o token no cabeçalho
// Authorization e, se ele estiver vencido, renovam a sessão sozinhas antes de desistir.
import { erroDaResposta, requisicaoAutenticada } from './http'

const JSON_HEADERS = { 'Content-Type': 'application/json' }

// Cadastra um aluno novo (a API o cria sempre como ativo). dataNascimento no formato "aaaa-mm-dd".
// Erros: 400 (campos inválidos, com a lista na mensagem) e 409 (e-mail já cadastrado).
export async function criarAluno({ nome, email, dataNascimento }) {
  const resposta = await requisicaoAutenticada('/api/alunos', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ nome, email, dataNascimento }),
  })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível cadastrar o aluno.')
  return resposta.json() // o aluno criado, já com o id
}

// Busca um aluno pelo id (a API guarda essa consulta no cache Redis). 404 = não existe.
export async function buscarAluno(id) {
  const resposta = await requisicaoAutenticada(`/api/alunos/${id}`)
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível carregar o aluno.')
  return resposta.json()
}

// Altera nome, e-mail e data de nascimento (dataNascimento no formato "aaaa-mm-dd").
// A API valida e responde 400 (campos inválidos, com a lista na mensagem) ou 409 (e-mail já usado).
export async function atualizarAluno(id, { nome, email, dataNascimento }) {
  const resposta = await requisicaoAutenticada(`/api/alunos/${id}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify({ nome, email, dataNascimento }),
  })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível alterar o aluno.')
  return resposta.json()
}

// Ativa ou desativa o aluno (rotas separadas do PUT): PATCH /alunos/{id}/enable ou /disable
export async function alterarSituacao(id, ativo) {
  const resposta = await requisicaoAutenticada(`/api/alunos/${id}/${ativo ? 'enable' : 'disable'}`, {
    method: 'PATCH',
  })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível alterar a situação do aluno.')
  return resposta.json()
}

// Remove o aluno de vez (irreversível). Só quem tem perfil ADMIN pode: os demais recebem 403.
// A API responde 204 sem corpo, por isso não há resposta.json() aqui.
export async function excluirAluno(id) {
  const resposta = await requisicaoAutenticada(`/api/alunos/${id}`, { method: 'DELETE' })
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível excluir o aluno.')
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
  // URLSearchParams monta a query string e já escapa caracteres especiais (acentos, espaços, &...)
  const parametros = new URLSearchParams({ page, size, sort: `${campoOrdem},${direcaoOrdem}` })
  if (nome) parametros.set('nome', nome) // a API filtra por "nome começa com o texto", sem diferenciar maiúsculas
  if (situacao !== 'todos') parametros.set('ativo', situacao === 'ativos')

  const resposta = await requisicaoAutenticada(`/api/alunos?${parametros}`)
  if (!resposta.ok) throw await erroDaResposta(resposta, 'Não foi possível carregar os alunos.')

  // A API devolve HAL: os alunos ficam em _embedded.alunos e a paginação em page.
  // Quando a lista está vazia o Spring omite o _embedded, por isso o "?? []".
  const dados = await resposta.json()
  return {
    alunos: dados._embedded?.alunos ?? [],
    pagina: dados.page, // { number, size, totalElements, totalPages }
  }
}
