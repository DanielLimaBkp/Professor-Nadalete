const form = document.getElementById('form-ordem');
const campoCodigo = document.getElementById('codigo');
const comboCliente = document.getElementById('cliente');
const comboTipo = document.getElementById('tipo');
const filtroCliente = document.getElementById('filtro-cliente');
const lista = document.getElementById('lista-ordens');
let ocupado = false;
let listagemCliente = [];
let listagemTipoServico = [];


function bloquear(valor) {
    ocupado = valor;
    document.getElementById('campos-formulario').disabled = valor;
    document.getElementById('btn-atualizar').disabled = valor;
    filtroCliente.disabled = valor;
    lista.querySelectorAll('button').forEach(botao => botao.disabled = valor);
}

function preencherCombo(combo, listagemRegistro, textoInicial) {
    const selecionado = combo.value;
    combo.replaceChildren(new Option(textoInicial, ''));
    listagemRegistro.forEach(item => combo.add(new Option(item.nome, String(item.codigo))));
    combo.value = selecionado;
}

async function atualizarTabela() {
    const listagemClienteAtual = await requisicao('/api/clientes');
    const listagemTipoServicoAtual = await requisicao('/api/tipos-servico');
    const filtro = filtroCliente.value;
    const listagemOrdemServico = await requisicao('/api/ordens' + (filtro ? '?codigoCliente=' + filtro : ''));
    listagemCliente = listagemClienteAtual;
    listagemTipoServico = listagemTipoServicoAtual;
    preencherCombo(comboCliente, listagemCliente, 'Selecione um cliente');
    preencherCombo(comboTipo, listagemTipoServico, 'Selecione um tipo de serviço');
    preencherCombo(filtroCliente, listagemCliente, 'Todos os clientes');
    lista.replaceChildren();
    if (!listagemOrdemServico.length) {
        const linha = document.createElement('tr');
        const celula = document.createElement('td');
        celula.colSpan = 7;
        celula.textContent = 'Nenhuma ordem encontrada.';
        linha.appendChild(celula);
        lista.appendChild(linha);
    }
    listagemOrdemServico.forEach(ordem => {
        const linha = document.createElement('tr');
        const cliente = listagemCliente.find(item => item.codigo === ordem.codigoCliente);
        const tipo = listagemTipoServico.find(item => item.codigo === ordem.codigoTipoServico);
        adicionarCelula(linha, ordem.codigo);
        adicionarCelula(linha, ordem.titulo);
        adicionarCelula(linha, cliente ? cliente.nome : ordem.codigoCliente);
        adicionarCelula(linha, tipo ? tipo.nome : ordem.codigoTipoServico);
        adicionarCelula(linha, ordem.prioridade);
        adicionarCelula(linha, ordem.status.replaceAll('_', ' '));
        const acoes = document.createElement('td');
        const abrir = document.createElement('a');
        abrir.textContent = 'Abrir';
        abrir.href = 'ordem.html?codigo=' + ordem.codigo;
        acoes.appendChild(abrir);
        if (ordem.status !== 'CONCLUIDA' && ordem.status !== 'CANCELADA') {
            const editar = document.createElement('button');
            editar.textContent = 'Editar';
            editar.className = 'secundario';
            editar.addEventListener('click', () => prepararEdicao(ordem));
            acoes.appendChild(editar);
        }
        const excluir = document.createElement('button');
        excluir.textContent = 'Excluir';
        excluir.className = 'excluir';
        excluir.addEventListener('click', () => excluirOrdem(ordem.codigo));
        acoes.appendChild(excluir);
        linha.appendChild(acoes);
        lista.appendChild(linha);
    });
}

async function executar(acao = null) {
    if (ocupado) return;
    bloquear(true);
    mostrarMensagem('Aguarde...');
    let alterado = false;
    try {
        if (acao) { await acao(); alterado = true; limparFormulario(); }
        await atualizarTabela();
        mostrarMensagem(alterado ? 'Operação realizada com sucesso.' : '', alterado ? 'sucesso' : '');
    } catch (erro) {
        mostrarMensagem((alterado ? 'Alteração realizada, mas a lista não foi atualizada. ' : '') + erro.message, 'erro');
    } finally { bloquear(false); }
}

function prepararEdicao(ordem) {
    if (ocupado) return;
    campoCodigo.value = ordem.codigo;
    comboCliente.value = String(ordem.codigoCliente);
    comboTipo.value = String(ordem.codigoTipoServico);
    document.getElementById('titulo').value = ordem.titulo;
    document.getElementById('descricao').value = ordem.descricao;
    document.getElementById('prioridade').value = ordem.prioridade;
    document.getElementById('titulo-formulario').textContent = 'Editar ordem ' + ordem.codigo;
    document.getElementById('titulo').focus();
}

function limparFormulario() {
    form.reset();
    campoCodigo.value = '';
    document.getElementById('titulo-formulario').textContent = 'Abrir ordem de serviço';
}
form.addEventListener('submit', evento => {
    evento.preventDefault();
    const codigo = campoCodigo.value;
    const dadoFormulario = {
        codigoCliente: Number(comboCliente.value),
        codigoTipoServico: Number(comboTipo.value),
        titulo: document.getElementById('titulo').value.trim(),
        descricao: document.getElementById('descricao').value.trim(),
        prioridade: document.getElementById('prioridade').value
    };
    executar(() => requisicao('/api/ordens' + (codigo ? '/' + codigo : ''), codigo ? 'PUT' : 'POST', dadoFormulario));
});

function excluirOrdem(codigo) {
    if (ocupado || !confirm('Excluir a ordem ' + codigo + '? Suas atividades e vínculos com técnicos também serão removidos.')) return;
    executar(() => requisicao('/api/ordens/' + codigo, 'DELETE'));
}
document.getElementById('btn-cancelar').addEventListener('click', limparFormulario);
document.getElementById('btn-atualizar').addEventListener('click', () => executar());
filtroCliente.addEventListener('change', () => executar());
document.addEventListener('DOMContentLoaded', () => executar());
