import { MENSAGEM_EMAIL } from '../utils/validacoes'

// Campo de e-mail dos popups, com a mensagem de erro logo abaixo.
//   mostrarErro: o popup decide quando mostrar (depois de sair do campo ou de tentar enviar)
// Usamos type="text" com inputMode="email" (teclado certo no celular) para a validação ser só a nossa,
// em vez da do navegador, que aceita endereços incompletos como "a@b".
function CampoEmail({ id, valor, onChange, onBlur, mostrarErro }) {
  return (
    <div className="editar-campo">
      <label htmlFor={id}>E-mail</label>
      <input
        id={id}
        type="text"
        inputMode="email"
        autoComplete="email"
        value={valor}
        onChange={(evento) => onChange(evento.target.value)}
        onBlur={onBlur}
        className={mostrarErro ? 'editar-input-invalido' : ''}
        aria-invalid={mostrarErro}
        aria-describedby={mostrarErro ? `${id}-erro` : undefined}
        required
      />
      {mostrarErro && (
        <span id={`${id}-erro`} className="editar-campo-erro">
          {MENSAGEM_EMAIL}
        </span>
      )}
    </div>
  )
}

export default CampoEmail
