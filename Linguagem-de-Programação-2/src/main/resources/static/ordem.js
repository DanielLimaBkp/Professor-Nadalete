const parametro = new URLSearchParams(location.search).get('codigo');
const codigoOrdem = Number(parametro);
const codigoValido = /^[0-9]+$/.test(parametro || '') && Number.isSafeInteger(codigoOrdem) && codigoOrdem > 0;
const rota = '/api/ordens/' + codigoOrdem;
let ocupado = false;
let encerrada = true;


function bloquear(valor) {
    ocupado = valor;
    document.getElementById('btn-atualizar').disabled = valor || !codigoValido;
    ['campos-status', 'campos-atividade', 'campos-tecnico'].forEach(id => {
        document.getElementById(id).disabled = valor || encerrada;
    });
    document.querySelectorAll('.edicao').forEach(botao => botao.disabled = valor || encerrada);
}

function dataFormatada(data) {
    return data ? new Date(data).toLocaleString('pt-BR') : '—';
}

function botaoAcao(texto, acao) {
    const botao = document.createElement('button');
    botao.textContent = texto;
    botao.type = 'button';
    botao.className = 'secundario edicao';
    botao.addEventListener('click', acao);
    return botao;
}

function linhaVazia(lista, colunas, texto) {
    const linha = document.createElement('tr');
    const celula = document.createElement('td');
    celula.colSpan = colunas;
    celula.textContent = texto;
    linha.appendChild(celula);
    lista.appendChild(linha);
}

async function carregarDetalhes() {
    const ordem = await requisicao(rota);
    const listagemAtividade = await requisicao(rota + '/atividades');
    const listagemTecnicoResponsavel = await requisicao(rota + '/tecnicos');
    const listagemTecnicoHabilitado = await requisicao('/api/tecnicos?codigoTipoServico=' + ordem.codigoTipoServico);
    const cliente = await requisicao('/api/clientes/' + ordem.codigoCliente);
    const tipo = await requisicao('/api/tipos-servico/' + ordem.codigoTipoServico);
    encerrada = ordem.status === 'CONCLUIDA' || ordem.status === 'CANCELADA';
    document.getElementById('titulo-ordem').textContent = 'OS ' + ordem.codigo + ' — ' + ordem.titulo;
    document.getElementById('descricao-ordem').textContent = ordem.descricao;
    document.getElementById('resumo-ordem').textContent = 'Cliente: ' + cliente.nome + ' | Serviço: ' + tipo.nome + ' | Prioridade: ' + ordem.prioridade;
    document.getElementById('data-abertura').textContent = 'Abertura: ' + dataFormatada(ordem.dataAbertura);
    document.getElementById('status-ordem').value = ordem.status;
    const tabelaAtividade = document.getElementById('lista-atividades');
    tabelaAtividade.replaceChildren();
    if (!listagemAtividade.length) linhaVazia(tabelaAtividade, 5, 'Nenhuma atividade cadastrada.');
    listagemAtividade.forEach(atividade => {
        const linha = document.createElement('tr');
        adicionarCelula(linha, atividade.codigo);
        adicionarCelula(linha, atividade.descricao);
        const celulaStatus = document.createElement('td');
        const combo = document.createElement('select');
        combo.className = 'edicao';
        combo.setAttribute('aria-label', 'Status da atividade ' + atividade.codigo);
        ['PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA'].forEach(status => {
            combo.add(new Option(status.replaceAll('_', ' '), status));
        });
        combo.value = atividade.status;
        celulaStatus.append(combo, botaoAcao('Aplicar', () => {
            const status = combo.value;
            executar(() => requisicao('/api/atividades/' + atividade.codigo + '/status', 'PATCH', { status }));
        }));
        linha.appendChild(celulaStatus);
        adicionarCelula(linha, dataFormatada(atividade.dataConclusao));
        const acoes = document.createElement('td');
        acoes.appendChild(botaoAcao('Editar descrição', () => {
            document.getElementById('codigo-atividade').value = atividade.codigo;
            document.getElementById('descricao-atividade').value = atividade.descricao;
            document.getElementById('descricao-atividade').focus();
        }));
        acoes.appendChild(botaoAcao('Excluir', () => {
            if (confirm('Excluir esta atividade?')) {
                executar(() => requisicao('/api/atividades/' + atividade.codigo, 'DELETE'), true);
            }
        }));
        linha.appendChild(acoes);
        tabelaAtividade.appendChild(linha);
    });
    const comboTecnico = document.getElementById('tecnico');
    comboTecnico.replaceChildren(new Option('Selecione um técnico', ''));
    listagemTecnicoHabilitado.filter(tecnico => !listagemTecnicoResponsavel.some(item => item.codigo === tecnico.codigo))
        .forEach(tecnico => comboTecnico.add(new Option(tecnico.nome, String(tecnico.codigo))));
    const tabelaTecnicoResponsavel = document.getElementById('lista-tecnicos');
    tabelaTecnicoResponsavel.replaceChildren();
    if (!listagemTecnicoResponsavel.length) linhaVazia(tabelaTecnicoResponsavel, 4, 'Nenhum técnico atribuído.');
    listagemTecnicoResponsavel.forEach(tecnico => {
        const linha = document.createElement('tr');
        adicionarCelula(linha, tecnico.codigo);
        adicionarCelula(linha, tecnico.nome);
        adicionarCelula(linha, tecnico.especialidade);
        const acoes = document.createElement('td');
        acoes.appendChild(botaoAcao('Remover vínculo', () => {
            if (confirm('Remover este técnico da ordem?')) {
                executar(() => requisicao(rota + '/tecnicos/' + tecnico.codigo, 'DELETE'));
            }
        }));
        linha.appendChild(acoes);
        tabelaTecnicoResponsavel.appendChild(linha);
    });
}

function limparAtividade() {
    document.getElementById('form-atividade').reset();
    document.getElementById('codigo-atividade').value = '';
}

async function executar(acao = null, limpar = false) {
    if (ocupado || !codigoValido) return;
    bloquear(true);
    mostrarMensagem('Aguarde...');
    let alterado = false;
    try {
        if (acao) { await acao(); alterado = true; if (limpar) limparAtividade(); }
        await carregarDetalhes();
        mostrarMensagem(alterado ? 'Operação realizada com sucesso.' : '', alterado ? 'sucesso' : '');
    } catch (erro) {
        mostrarMensagem((alterado ? 'Alteração realizada, mas os detalhes não foram atualizados. ' : '') + erro.message, 'erro');
    } finally { bloquear(false); }
}
document.getElementById('form-status').addEventListener('submit', evento => {
    evento.preventDefault();
    const status = document.getElementById('status-ordem').value;
    if (status === 'CANCELADA' && !confirm('Cancelar esta ordem? Ela ficará bloqueada para edição.')) return;
    executar(() => requisicao(rota + '/status', 'PATCH', { status }));
});
document.getElementById('form-atividade').addEventListener('submit', evento => {
    evento.preventDefault();
    const codigo = document.getElementById('codigo-atividade').value;
    const descricao = document.getElementById('descricao-atividade').value.trim();
    executar(() => requisicao(codigo ? '/api/atividades/' + codigo : rota + '/atividades', codigo ? 'PUT' : 'POST', { descricao }), true);
});
document.getElementById('form-tecnico').addEventListener('submit', evento => {
    evento.preventDefault();
    const codigo = document.getElementById('tecnico').value;
    if (codigo) executar(() => requisicao(rota + '/tecnicos/' + codigo, 'PUT'));
});
document.getElementById('btn-cancelar-atividade').addEventListener('click', limparAtividade);
document.getElementById('btn-atualizar').addEventListener('click', () => executar());
document.addEventListener('DOMContentLoaded', () => {
    bloquear(false);
    if (codigoValido) executar();
    else {
        document.getElementById('titulo-ordem').textContent = 'Ordem não selecionada';
        mostrarMensagem('Volte à lista e abra uma ordem válida.', 'erro');
    }
});
