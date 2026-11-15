// A data de nascimento precisa ser no passado: o máximo do campo de data é ontem, no fuso local ("aaaa-mm-dd").
// toLocaleDateString('sv-SE') é um truque para obter exatamente o formato aaaa-mm-dd.
export function ontem() {
  const data = new Date()
  data.setDate(data.getDate() - 1)
  return data.toLocaleDateString('sv-SE')
}
