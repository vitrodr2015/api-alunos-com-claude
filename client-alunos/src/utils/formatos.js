// "01/12/2000" a partir de "2000-12-01". Não usamos new Date() de propósito: ele converteria
// o texto para UTC e, em fusos como o do Brasil, mostraria o dia anterior.
export function formatarData(iso) {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

// "Ana Souza" -> "AS" (primeira letra do primeiro e do último nome)
export function iniciais(nome) {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}

// Idade em anos completos a partir de "aaaa-mm-dd", usando só os números (sem fuso horário).
export function calcularIdade(iso) {
  const [ano, mes, dia] = iso.split('-').map(Number)
  const hoje = new Date()
  let idade = hoje.getFullYear() - ano
  const jaFezAniversario = hoje.getMonth() + 1 > mes || (hoje.getMonth() + 1 === mes && hoje.getDate() >= dia)
  if (!jaFezAniversario) idade -= 1
  return idade
}
