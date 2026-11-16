// Validação de e-mail do client. O type="email" do navegador aceita "a@b" (sem ponto nem extensão),
// então conferimos nós mesmos: parte local, @, domínio com pelo menos um ponto e extensão de 2+ letras.
// Aceita: ana@escola.com, ana.souza+aluno@mail.escola.com.br    Recusa: ana, ana@, ana@escola, ana@escola., ana@@x.com
const PADRAO_EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/

export const MENSAGEM_EMAIL = 'Informe um e-mail válido, como nome@dominio.com.'

export function emailValido(email) {
  return PADRAO_EMAIL.test(email.trim())
}
