const API_URL = '/api/tecnicos';
const form = document.getElementById('form-cadastro');
const lista = document.getElementById('lista-cadastros');
const campoCodigo = document.getElementById('codigo');
const campoNome = document.getElementById('nome');
const campoEmail = document.getElementById('email');
const campoEspecialidade = document.getElementById('especialidade');
const comboTecnicoHabilitado = document.getElementById('tecnico-habilitado');
const comboTipoServicoHabilitado = document.getElementById('tipo-servico-habilitado');
const listaTipoServicoHabilitado = document.getElementById('lista-tipo-servico-habilitado');
let ocupado = false;


function bloquear(valor) {
    ocupado = valor;
    document.getElementById('campos-formulario').disabled = valor;
    document.getElementById('btn-atualizar').disabled = valor;
    lista.querySelectorAll('button').forEach(botao => botao.disabled = valor);
    document.getElementById('campo-tipo-servico-habilitado').disabled = valor;
    listaTipoServicoHabilitado.querySelectorAll('button').forEach(botao => botao.disabled = valor);
}


async function atualizarTabela() {
    const listagemRegistro = await requisicao(API_URL);
    const selecionado = comboTecnicoHabilitado.value;
    comboTecnicoHabilitado.replaceChildren(new Option('Selecione um técnico', ''));
    listagemRegistro.forEach(tecnico => comboTecnicoHabilitado.add(new Option(tecnico.nome, String(tecnico.codigo))));
    comboTecnicoHabilitado.value = selecionado;
    await atualizarTipoServicoHabilitado();
    lista.replaceChildren();
    if (listagemRegistro.length === 0) {
        const linha = document.createElement('tr');
        const celula = document.createElement('td');
        celula.colSpan = 5;
        celula.textContent = 'Nenhum cadastro encontrado.';
        linha.appendChild(celula);
        lista.appendChild(linha);
        return;
    }
    listagemRegistro.forEach(registro => {
        const linha = document.createElement('tr');
        adicionarCelula(linha, registro.codigo);
        adicionarCelula(linha, registro.nome);
        adicionarCelula(linha, registro.email);
        adicionarCelula(linha, registro.especialidade);
        const acoes = document.createElement('td');
        const editar = document.createElement('button');
        editar.type = 'button';
        editar.textContent = 'Editar';
        editar.className = 'secundario';
        editar.disabled = ocupado;
        editar.addEventListener('click', () => prepararEdicao(registro));
        const excluir = document.createElement('button');
        excluir.type = 'button';
        excluir.textContent = 'Excluir';
        excluir.className = 'excluir';
        excluir.disabled = ocupado;
        excluir.addEventListener('click', () => excluirCadastro(registro.codigo));
        const botaoTipoServicoHabilitado = document.createElement('button');
        botaoTipoServicoHabilitado.type = 'button';
        botaoTipoServicoHabilitado.textContent = 'Tipo de serviço habilitado';
        botaoTipoServicoHabilitado.disabled = ocupado;
        botaoTipoServicoHabilitado.addEventListener('click', () => {
            comboTecnicoHabilitado.value = String(registro.codigo);
            carregarTipoServicoHabilitado();
            document.getElementById('titulo-tipo-servico-habilitado').scrollIntoView();
        });
        acoes.append(editar, excluir, botaoTipoServicoHabilitado);
        linha.appendChild(acoes);
        lista.appendChild(linha);
    });
}


async function carregarCadastro() {
    if (ocupado) return;
    bloquear(true);
    mostrarMensagem('Carregando cadastros...');
    try {
        await atualizarTabela();
        mostrarMensagem('');
    } catch (erro) {
        mostrarMensagem(erro.message, 'erro');
    } finally {
        bloquear(false);
    }
}


async function salvarCadastro(evento) {
    evento.preventDefault();
    if (ocupado) return;
    const codigo = campoCodigo.value;
    const dadoFormulario = {
        nome: campoNome.value.trim(),
        email: campoEmail.value.trim(),
        especialidade: campoEspecialidade.value.trim()
    };
    bloquear(true);
    mostrarMensagem('Salvando...');
    let salvo = false;
    try {
        const rota = codigo ? API_URL + '/' + codigo : API_URL;
        await requisicao(rota, codigo ? 'PUT' : 'POST', dadoFormulario);
        salvo = true;
        limparFormulario();
        await atualizarTabela();
        mostrarMensagem('Cadastro salvo com sucesso.', 'sucesso');
    } catch (erro) {
        const aviso = salvo ? 'Cadastro salvo, mas a lista não foi atualizada. ' : '';
        mostrarMensagem(aviso + erro.message, 'erro');
    } finally {
        bloquear(false);
    }
}


function prepararEdicao(registro) {
    if (ocupado) return;
    campoCodigo.value = registro.codigo;
    campoNome.value = registro.nome;
    campoEmail.value = registro.email;
    campoEspecialidade.value = registro.especialidade;
    document.getElementById('titulo-formulario').textContent = 'Editar cadastro ' + registro.codigo;
    mostrarMensagem('Altere os campos e clique em Salvar.');
    campoNome.focus();
}


async function excluirCadastro(codigo) {
    if (ocupado || !confirm('Excluir o cadastro de código ' + codigo + '?')) return;
    bloquear(true);
    mostrarMensagem('Excluindo...');
    let excluido = false;
    try {
        await requisicao(API_URL + '/' + codigo, 'DELETE');
        excluido = true;
        if (campoCodigo.value === String(codigo)) limparFormulario();
        await atualizarTabela();
        mostrarMensagem('Cadastro excluído.', 'sucesso');
    } catch (erro) {
        const aviso = excluido ? 'Cadastro excluído, mas a lista não foi atualizada. ' : '';
        mostrarMensagem(aviso + erro.message, 'erro');
    } finally {
        bloquear(false);
    }
}


function limparFormulario() {
    form.reset();
    campoCodigo.value = '';
    document.getElementById('titulo-formulario').textContent = 'Novo cadastro';
}

form.addEventListener('submit', salvarCadastro);
document.getElementById('btn-cancelar').addEventListener('click', () => {
    limparFormulario();
    mostrarMensagem('');
});
document.getElementById('btn-atualizar').addEventListener('click', carregarCadastro);
document.addEventListener('DOMContentLoaded', carregarCadastro);


async function atualizarTipoServicoHabilitado() {
    const codigo = comboTecnicoHabilitado.value;
    const listagemTipoServico = await requisicao('/api/tipos-servico');
    const tipoServicoHabilitado = codigo ? await requisicao(API_URL + '/' + codigo + '/tipos-servico') : [];
    comboTipoServicoHabilitado.replaceChildren(new Option('Selecione um tipo de serviço', ''));
    listagemTipoServico.filter(tipo => !tipoServicoHabilitado.some(item => item.codigo === tipo.codigo))
        .forEach(tipo => comboTipoServicoHabilitado.add(new Option(tipo.nome, String(tipo.codigo))));
    listaTipoServicoHabilitado.replaceChildren();
    if (!tipoServicoHabilitado.length) {
        const linha = document.createElement('tr');
        const celula = document.createElement('td');
        celula.colSpan = 4;
        celula.textContent = codigo ? 'Nenhuma habilitação cadastrada.' : 'Selecione um técnico para consultar as habilitações.';
        linha.appendChild(celula);
        listaTipoServicoHabilitado.appendChild(linha);
    }
    tipoServicoHabilitado.forEach(tipo => {
        const linha = document.createElement('tr');
        adicionarCelula(linha, tipo.codigo);
        adicionarCelula(linha, tipo.nome);
        adicionarCelula(linha, tipo.descricao);
        const acoes = document.createElement('td');
        const remover = document.createElement('button');
        remover.type = 'button';
        remover.textContent = 'Remover habilitação';
        remover.disabled = ocupado;
        remover.addEventListener('click', () => {
            if (!ocupado && confirm('Remover a habilitação para ' + tipo.nome + '?')) {
                alterarTecnicoTipoServico(codigo, tipo.codigo, 'DELETE');
            }
        });
        acoes.appendChild(remover);
        linha.appendChild(acoes);
        listaTipoServicoHabilitado.appendChild(linha);
    });
}


async function carregarTipoServicoHabilitado() {
    if (ocupado) return;
    bloquear(true);
    mostrarMensagem('Consultando habilitações...');
    try {
        await atualizarTipoServicoHabilitado();
        mostrarMensagem('');
    } catch (erro) {
        mostrarMensagem(erro.message, 'erro');
    } finally { bloquear(false); }
}


async function alterarTecnicoTipoServico(codigoTecnico, codigoTipo, metodo) {
    if (ocupado) return;
    bloquear(true);
    mostrarMensagem('Aguarde...');
    let alterado = false;
    try {
        await requisicao(API_URL + '/' + codigoTecnico + '/tipos-servico/' + codigoTipo, metodo);
        alterado = true;
        await atualizarTipoServicoHabilitado();
        mostrarMensagem('Habilitação atualizada.', 'sucesso');
    } catch (erro) {
        mostrarMensagem((alterado ? 'Alteração realizada, mas a lista não foi atualizada. ' : '') + erro.message, 'erro');
    } finally { bloquear(false); }
}

comboTecnicoHabilitado.addEventListener('change', carregarTipoServicoHabilitado);
document.getElementById('form-tecnico-tipo-servico').addEventListener('submit', evento => {
    evento.preventDefault();
    const codigoTecnico = comboTecnicoHabilitado.value;
    const codigoTipo = comboTipoServicoHabilitado.value;
    if (codigoTecnico && codigoTipo) alterarTecnicoTipoServico(codigoTecnico, codigoTipo, 'PUT');
});
