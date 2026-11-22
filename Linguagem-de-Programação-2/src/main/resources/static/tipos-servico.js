const API_URL = '/api/tipos-servico';
const form = document.getElementById('form-cadastro');
const lista = document.getElementById('lista-cadastros');
const campoCodigo = document.getElementById('codigo');
const campoNome = document.getElementById('nome');
const campoDescricao = document.getElementById('descricao');
const comboCategoria = document.getElementById('categoria');
let listagemCategoriaServico = [];
let ocupado = false;


function bloquear(valor) {
    ocupado = valor;
    document.getElementById('campos-formulario').disabled = valor;
    document.getElementById('btn-atualizar').disabled = valor;
    lista.querySelectorAll('button').forEach(botao => botao.disabled = valor);
}


async function atualizarTabela() {
    const listagemCategoriaServicoAtual = await requisicao('/api/categorias');
    const listagemRegistro = await requisicao(API_URL);
    listagemCategoriaServico = listagemCategoriaServicoAtual;
    const selecionada = comboCategoria.value;
    comboCategoria.replaceChildren(new Option('Selecione uma categoria', ''));
    listagemCategoriaServico.forEach(categoria => {
        comboCategoria.add(new Option(categoria.nome, String(categoria.codigo)));
    });
    comboCategoria.value = selecionada;
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
        adicionarCelula(linha, registro.descricao);
        const categoria = listagemCategoriaServico.find(item => item.codigo === registro.codigoCategoria);
        adicionarCelula(linha, categoria ? categoria.nome : 'Categoria ' + registro.codigoCategoria);
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
        acoes.append(editar, excluir);
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
        descricao: campoDescricao.value.trim(),
        codigoCategoria: Number(comboCategoria.value)
    };
    if (!Number.isSafeInteger(dadoFormulario.codigoCategoria) || dadoFormulario.codigoCategoria <= 0) {
        mostrarMensagem('Selecione uma categoria.', 'erro');
        return;
    }
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
    campoDescricao.value = registro.descricao;
    comboCategoria.value = String(registro.codigoCategoria);
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
