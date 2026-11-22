# SIGOS — trabalho de LP2

Desenvolvi o SIGOS para gerenciar ordens de serviço, do cadastro do cliente à conclusão das atividades. O projeto está concluído e usa Java 21, SQLite, HTML, CSS e JavaScript.

## Como compilar e rodar

### Pré-requisitos

Instale o **JDK 21 ou superior** para compilar e executar o Java e o **Maven 3.9 ou superior** para baixar as bibliotecas, rodar os testes e gerar o JAR.

Configure `JAVA_HOME` com o caminho da pasta do JDK e adicione as pastas `bin` do JDK e do Maven ao `PATH`. Em um novo terminal, confira as versões:

```text
java -version
javac -version
mvn -version
```

Confira também se `mvn -version` mostra Java 21 ou superior. O primeiro build precisa de internet para baixar as bibliotecas.

Na pasta clonada, abra o terminal onde estiver o arquivo `pom.xml`. O clone vem sem a pasta `target`. O Maven cria essa pasta e gera o JAR durante o build.

### Windows e Linux

Use o PowerShell no Windows ou o terminal no Linux.

1. Gere o JAR. O comando limpa os arquivos do build anterior, compila o código, roda os testes e cria o JAR em `target`.

   ```text
   mvn clean package
   ```

2. Depois que o Maven mostrar `BUILD SUCCESS`, execute o JAR.

   ```text
   java -jar target/sigos-1.0.0-SNAPSHOT.jar
   ```

Para só compilar o código, use `mvn compile`.

Abra [localhost:8080](http://localhost:8080) no navegador. Para parar o servidor, pressione `Ctrl+C` no terminal. O banco fica em `data/sigos.db`.

## Estrutura do projeto

```text
src/main/java/br/com/sigos/
  Main.java       conecta os componentes
  config/         banco de dados
  controller/     rotas e JSON
  service/        operações e regras de negócio
  repository/     SQL e mapeamento
  model/          entidades e regras do domínio

src/main/resources/schema.sql
src/main/resources/static/   menu, páginas HTML, CSS e JavaScript
src/test/java/br/com/sigos/CadastroApiTest.java
```

## Testar pelo Postman

Criei as telas para facilitar o uso dos endpoints pelo navegador. Para testar no Postman, use `http://localhost:8080` com os caminhos abaixo. Envie JSON nas requisições com corpo.

| Método | Endpoint |
| --- | --- |
| GET | `/api/status` |
| GET, POST | `/api/{colecao}` |
| GET, PUT, DELETE | `/api/{colecao}/{codigo}` |
| GET | `/api/ordens?codigoCliente={codigo}`, `/api/atividades?codigoOrdem={codigo}`, `/api/tecnicos?codigoTipoServico={codigo}` |
| PATCH | `/api/ordens/{codigo}/status`, `/api/atividades/{codigo}/status` |
| GET, POST | `/api/ordens/{codigo}/atividades` |
| GET | `/api/ordens/{codigo}/tecnicos`, `/api/tecnicos/{codigo}/tipos-servico` |
| PUT, DELETE | `/api/ordens/{codigo}/tecnicos/{codigoTecnico}`, `/api/tecnicos/{codigo}/tipos-servico/{codigoTipo}` |

No lugar de `{colecao}`, use `clientes`, `tecnicos`, `categorias`, `tipos-servico`, `ordens` ou `atividades`.

## Testes

```text
mvn test
```

Os 14 testes de integração usam um banco temporário e cobrem cadastros, vínculos e regras das ordens de serviço.

## Fluxo de cadastro

1. Cadastre o cliente, a categoria, o tipo de serviço e o técnico.
2. Na tela **Técnicos**, habilite o profissional para o tipo de serviço da ordem.
3. Crie a ordem escolhendo o cliente, o tipo de serviço e a prioridade.
4. Clique em **Abrir** na lista de ordens para adicionar atividades e atribuir o técnico.
5. Marque as atividades como concluídas e conclua a ordem.

Uma ordem só pode ser concluída quando não houver atividades pendentes. Depois de concluída ou cancelada, a ordem fica bloqueada para edição.

## Relacionamentos

- **1:N** — categoria e tipos de serviço, tipo de serviço e ordens, cliente e ordens, ordem e atividades.
- **N:N** — ordens e técnicos responsáveis, técnicos e tipos de serviço habilitados.

## Dados iniciais

Na primeira execução com um banco novo, o sistema carrega dados fictícios para demonstração. As ordens e atividades são cadastradas pela aplicação.
