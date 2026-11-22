

async function requisicao(rota, metodo = 'GET', dadoFormulario = null) {
    const opcaoRequisicao = { method: metodo };
    if (dadoFormulario !== null) {
        opcaoRequisicao.headers = { 'Content-Type': 'application/json' };
        opcaoRequisicao.body = JSON.stringify(dadoFormulario);
    }
    let resposta;
    try {
        resposta = await fetch(rota, opcaoRequisicao);
    } catch (erro) {
        throw new Error('Não foi possível conectar ao servidor. Confira se o SIGOS está iniciado.');
    }
    if (!resposta.ok) {
        let mensagem = 'O servidor retornou o erro ' + resposta.status + '.';
        try {
            const erro = await resposta.json();
            if (erro.erro) mensagem = erro.erro;
        } catch (erro) {
            
        }
        throw new Error(mensagem);
    }
    if (resposta.status === 204) return null;
    return resposta.json();
}


function mostrarMensagem(texto, tipo = '') {
    const mensagem = document.getElementById('mensagem');
    mensagem.textContent = texto;
    mensagem.className = tipo;
}


function adicionarCelula(linha, valor) {
    const celula = document.createElement('td');
    celula.textContent = valor;
    linha.appendChild(celula);
}
