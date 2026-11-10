# Pediu Patrao: Sistema de Gestao de Pizzaria e Dashboard Administrativo

Evolucao do sistema web Pediu Patrao desenvolvido em Java com Spring Boot e MongoDB Atlas, implementando controle de acesso por perfis (RBAC), gestao do ciclo de vida de pedidos, calculos no servidor, trilha de auditoria persistente, transacoes atomicas, suite de testes automatizados e Dashboard Administrativo com API REST assincrona.

## Demonstracao em Video
O video de demonstracao da Dashboard do projeto esta disponivel no repositorio:
- https://www.youtube.com/watch?v=XoE6NPIQ2mM

---

## Tecnologias utilizadas

- Linguagem: Java 21 (LTS)
- Framework: Spring Boot 3.4.4
  - Spring Web (MVC para paginas e REST para endpoints de dados)
  - Spring Security (autenticacao, autorizacao RBAC e protecao CSRF com cookies)
  - Spring Data MongoDB (persistencia e transacoes com MongoTransactionManager)
  - Thymeleaf (templates das paginas web)
- Banco de dados: MongoDB Atlas (Cluster na nuvem)
- Front-end: Bootstrap 5.3, Bootstrap Icons, Chart.js e JavaScript Vanilla (consumo assincrono com fetch)
- Testes automatizados: JUnit 5, Mockito e Spring Boot Test

---

## Perfis de acesso e permissoes

O sistema conta com tres perfis de acesso distintos, validados nas rotas e nos servicos do backend:

| Perfil | Usuario padrao | Senha | Responsabilidades e permissoes |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Gerenciar usuarios e seus perfis, consultar pedidos, visualizar a auditoria e acessar o Dashboard Administrativo. |
| **GERENTE** | `gerente` | `gerente123` | Gerenciar pedidos, exclusividade para desconto (ate 20%) e cancelamento (com justificativa obrigatoria), consultar clientes, visualizar auditoria e acessar o Dashboard Administrativo. |
| **ATENDENTE** | `atendente` | `atendente123` | Cadastrar e atualizar clientes, registrar novos pedidos e acompanhar o andamento. Nao tem acesso a desconto, cancelamento, gestao de produtos, usuarios, auditoria ou Dashboard (retorno HTTP 403). |

*** As restricoes de acesso foram configuradas diretamente no `SecurityConfig.java` e validadas tambem no `PedidoService.java`. Ocultar botoes na tela serve apenas para a experiencia visual: se um atendente tentar chamar diretamente rotas como `/usuarios`, `/pedidos/cancelar` ou `/dashboard`, o Spring Security bloqueia a requisicao e retorna erro 403 (Acesso Negado).

---

## Como executar o projeto

### Pre-requisitos
- Repositorio clonado localmente
- Java 21 instalado e apontado nas variaveis de ambiente (ou configurado via JAVA_HOME)
- Instancia do MongoDB ativa no MongoDB Atlas
- O projeto inclui o Maven Wrapper (`mvnw.cmd` e `mvnw`), dispensando instalacao manual do Maven

### Configuracao do Banco de Dados
No arquivo `src/main/resources/application.properties`, informe a URL de conexao na propriedade `spring.data.mongodb.uri`:

```properties
spring.data.mongodb.uri=sua_string_de_conexao_do_mongodb_aqui
spring.data.mongodb.database=pizzaria
```

O nome do banco de dados deve permanecer definido como `pizzaria`.

### Execucao no Windows
A partir da pasta raiz do projeto (`PediuPatrao\PediuPatrao`), execute por qualquer uma das opcoes:

1. Dando dois cliques no arquivo `executar.bat`
2. Pelo PowerShell:
   ```powershell
   .\executar.ps1
   ```
3. Pelo Maven Wrapper direto no terminal:
   ```powershell
   $env:JAVA_HOME = "C:\Users\fabricio.souza\tools\jdk-21.0.6+7"
   .\mvnw.cmd spring-boot:run
   ```

Apos inicializar, acesse no navegador: `http://localhost:8080`

A classe `DataInitializer.java` verifica a base no MongoDB na primeira execucao e garante a criacao automatica dos usuarios padrao (`admin`, `gerente`, `atendente`), alem dos produtos iniciais do cardapio.

### Execucao dos Testes Automatizados
Para executar a suite de testes unitarios:

```powershell
$env:JAVA_HOME = "C:\Users\fabricio.souza\tools\jdk-21.0.6+7"
.\mvnw.cmd test
```

---

## Requisitos do Sistema Base

### RF01: Controle de usuarios e permissoes
- Perfis `ADMIN`, `GERENTE` e `ATENDENTE` persistidos no MongoDB na colecao `usuarios`.
- Criptografia de senhas com `BCryptPasswordEncoder`.
- Regras de autorizacao por perfil no `SecurityConfig` para paginas e APIs.
- Bloqueio contra autoelevacao: somente o perfil `ADMIN` tem acesso ao cadastro e edicao de usuarios e papeis.
- Tratamento de CSRF nos formularios e tela personalizada para HTTP 403 (Acesso Negado).

### RF02: Cadastro e calculo de pedidos
- Entidade `Pedido` contendo sabor, tipo de pizza, quantidade, preco unitario, subtotal, desconto percentual, valor do desconto, valor total, atendente responsavel e data e hora de abertura.
- Calculo de valores executado exclusivamente no backend pelo `PedidoService`. O cliente nao consegue enviar totais arbitrarios pelo navegador ou requisicoes HTTP.

### RF03: Controle de entrada, saida e ciclo de vida do pedido
- Fluxo de status com validacao rigida no `PedidoService`:
  1. `RECEBIDO`: registra o horario de entrada e o usuario autenticado.
  2. `EM_PREPARACAO`: encaminhado para a producao na cozinha.
  3. `PRONTO`: pronto na bancada, aguardando saida.
  4. `SAIU_PARA_ENTREGA` ou `RETIRADO`: registra obrigatoriamente a data e hora e o responsavel pelo despacho.
  5. `FINALIZADO`: ciclo concluido.
- Bloqueio de transicoes incoerentes (por exemplo, tentar pular de `RECEBIDO` direto para `FINALIZADO`).
- Bloqueio total de alteracoes apos o pedido estar cancelado ou finalizado.

### RF04: Cancelamentos e descontos exclusivos do gerente
- Operacoes permitidas apenas para o perfil `GERENTE`, com trava no Spring Security e no servico.
- Desconto: percentual aplicado sobre o valor do pedido, limitado a no maximo 20%, e permitido apenas antes da saida do pedido. Tambem aceita 0% caso seja necessario remover um desconto aplicado anteriormente.
- Cancelamento: permitido apenas antes da saida, exigindo justificativa textual obrigatoria, preservando os dados originais e impedindo transicoes posteriores.
- Ambas as operacoes registram evento detalhado na auditoria com valores anteriores e novos.

### RF05: Auditoria persistente no MongoDB
- Colecao `auditoria` gravando inclusoes, alteracoes cadastrais e movimentacoes de pedidos.
- Monitoramento de dados de clientes campo a campo: alteracoes de logradouro, numero, bairro, telefone e nome gravam o valor antes e o valor depois.
- Cada registro armazena usuario responsavel e seu perfil, data e hora da operacao, acao executada e entidade afetada com seu ID, campos modificados com valor anterior e novo, e justificativa informada nos cancelamentos.
- Consulta restrita aos perfis `ADMIN` e `GERENTE`. A interface e as APIs nao fornecem endpoints para editar ou excluir dados da auditoria.

---

## Ajustes e Melhorias Tecnicas Implementadas

A partir do feedback da primeira etapa, as seguintes correcoes foram integradas ao nucleo do sistema:

1. **Preco unitario puxado do catalogo:** O backend nao aceita mais o preco unitario vindo diretamente no corpo da requisicao. O `PedidoService` consulta o produto no `ProdutoRepository` e aplica o preco oficial cadastrado no banco de dados.
2. **Bloqueio de desconto e cancelamento apos a saida:** A verificacao foi estendida para bloquear operacoes de desconto e cancelamento quando o pedido estiver com status `SAIU_PARA_ENTREGA` ou `RETIRADO`, ou quando a data de saida ja tiver sido registrada.
3. **Transacoes atomicas no MongoDB:** Criacao da configuracao `MongoTransactionConfig` com o bean `MongoTransactionManager` e adicao da anotacao `@Transactional` nos metodos de escrita do `PedidoService`. Isso garante que o salvamento do pedido e o registro na auditoria ocorram na mesma transacao atomica.
4. **Protecao CSRF com Cookie Repository:** No `SecurityConfig.java`, o CSRF utiliza `CookieCsrfTokenRepository.withHttpOnlyFalse()`, disponibilizando o token via cookie `XSRF-TOKEN` para consumo pelo front-end em requisicoes de modificacao.
5. **Credenciais dinamicas do administrador:** As credenciais iniciais foram desacopladas do codigo estatico no `DataInitializer.java` e vinculadas a propriedades `@Value("${app.admin.username:admin}")` e `@Value("${app.admin.password:admin123}")`.
6. **Suite de testes automatizados:** Implementacao de testes de unidade com JUnit 5 e Mockito cobrindo regras de precos, limites de desconto, bloqueios de status e justificativa obrigatoria.

---

## Modulo: Dashboard Administrativo

O Dashboard Administrativo foi projetado para que o responsavel pelo estabelecimento acompanhe o desempenho das vendas, visualize informacoes sobre os pedidos e identifique tendencias da operacao para apoio na tomada de decisoes.

A solucao desacopla a apresentacao visual da camada de dados: a tela web consome uma API REST propria atraves de requisicoes assincronas (`fetch`) em JavaScript e renderiza graficos dinamicos com a biblioteca Chart.js.

### Requisitos Funcionais do Dashboard

- RF01: Indicadores gerais de vendas. Apresenta cards com faturamento total, quantidade total de pedidos no periodo, pedidos faturados, ticket medio por pedido concluido, total de descontos concedidos e taxa percentual de cancelamento.
- RF02: Analise de vendas em graficos. Grafico de linha com a evolucao diaria do faturamento ao longo do intervalo de datas selecionado.
- RF03: Situacao dos pedidos. Grafico doughnut (rosca) apresentando a quantidade de pedidos distribuida por status operacional (recebido, preparacao, pronto, entrega, retirado, finalizado e cancelado).
- RF04: Produtos mais vendidos. Tabela ranqueando os produtos mais pedidos por volume, exibindo a quantidade de unidades vendidas, a receita acumulada por item e o percentual de participacao sobre as vendas totais.
- RF05: Filtros de consulta. Atalhos rapidos de periodo (Hoje, Ultimos 7 dias, Ultimos 30 dias e Mes Atual) e campos para selecao de data inicial e data final personalizadas.
- RF06: Integracao com a API. Interface integrada consumindo exclusivamente dados disponibilizados pelos endpoints REST de `/api/dashboard/**`.
- RF07: Atualizacao e tratamento de dados. Desconsideracao de pedidos cancelados no calculo da receita, arredondamento de valores monetarios em duas casas decimais e tratamento de erros no cliente.

### Requisitos Nao Funcionais do Dashboard

- RNF01: Seguranca de Acesso (RBAC). O acesso a tela do Dashboard e aos endpoints da API e restrito aos perfis ADMIN e GERENTE. O perfil ATENDENTE recebe status HTTP 403 (Acesso Negado).
- RNF02: Desempenho. Resposta dos endpoints de agregacao em tempo inferior a 500ms utilizando consultas diretas no MongoDB Atlas.
- RNF03: Padrao REST. Comunicacao estruturada integralmente no formato JSON UTF-8.
- RNF04: Interface Responsiva. Grade visual adaptavel para dispositivos desktop e mobile com Bootstrap 5.

---

## Documentacao dos Endpoints da API REST

Caminho base: `/api/dashboard`

### 1. Resumo Completo do Dashboard
- Rota: `GET /api/dashboard/resumo-completo`
- Perfil autorizado: ADMIN e GERENTE
- Parametros de consulta (Query Params):
  - `inicio` (String, formato YYYY-MM-DD, opcional): Data inicial do filtro. Padrao: 30 dias atras.
  - `fim` (String, formato YYYY-MM-DD, opcional): Data final do filtro. Padrao: data atual.
- Descricao: Consolida todos os dados necessarios para renderizar o painel em uma unica requisicao HTTP.

Exemplo de requisicao:
```http
GET /api/dashboard/resumo-completo?inicio=2026-10-11&fim=2026-11-10 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

Exemplo de resposta (200 OK):
```json
{
  "periodo": {
    "inicio": "2026-10-11",
    "fim": "2026-11-10"
  },
  "metricas": {
    "faturamentoTotal": 1064.00,
    "quantidadePedidos": 9,
    "pedidosFaturados": 7,
    "ticketMedio": 152.00,
    "totalDescontos": 37.00,
    "pedidosComDesconto": 2,
    "pedidosCancelados": 2,
    "taxaCancelamento": 22.20
  },
  "vendasTempo": [
    { "data": "2026-10-15", "totalVendas": 642.00, "quantidadePedidos": 3 },
    { "data": "2026-10-16", "totalVendas": 422.00, "quantidadePedidos": 4 }
  ],
  "statusPedidos": [
    { "status": "FINALIZADO", "quantidade": 7, "corHex": "#198754" },
    { "status": "CANCELADO", "quantidade": 2, "corHex": "#dc3545" }
  ],
  "topProdutos": [
    { "nome": "Pizza Calabresa Especial", "quantidade": 8, "faturamento": 370.00, "percentual": 57.14 },
    { "nome": "Calabresa", "quantidade": 3, "faturamento": 396.00, "percentual": 21.43 },
    { "nome": "Mussarela", "quantidade": 2, "faturamento": 246.00, "percentual": 14.29 },
    { "nome": "Pizza Quatro Queijos", "quantidade": 1, "faturamento": 52.00, "percentual": 7.14 }
  ]
}
```

### 2. Endpoints Modulares
- `GET /api/dashboard/metricas`: Retorna apenas o objeto de metricas gerais (faturamento, pedidos concluidos, ticket medio, descontos e cancelamentos).
- `GET /api/dashboard/vendas-tempo`: Retorna o faturamento diario para alimentacao do grafico de linha.
- `GET /api/dashboard/status-pedidos`: Retorna a contagem de pedidos por status operacional com a cor associada para o grafico de rosca.
- `GET /api/dashboard/top-produtos?limite=5`: Retorna o ranking dos produtos mais vendidos ordenados por quantidade.

### 3. Tratamento de Erros e Codigos de Retorno
- `400 Bad Request`: Formato de data invalido (diferente do padrao YYYY-MM-DD).
- `401 Unauthorized`: Requisicao sem usuario autenticado na sessao.
- `403 Forbidden`: Usuario logado sem perfil de gestao (perfil ATENDENTE).
- `500 Internal Server Error`: Erro de conexao com o banco ou excecao no processamento.

---

## Evidencias de Testes Realizados

### Testes Automatizados de Unidade
Desenvolvidos em `src/test/java/com/umc/pediupatrao/service/PedidoServiceTest.java` com JUnit 5 e Mockito:

| Metodo de Teste | Regra Validada | Resultado |
| :--- | :--- | :--- |
| `testPrecoUnitarioSemprePuxadoDoCatalogo` | Preco unitario da requisicao e descartado e valor oficial do catalogo e gravado | APROVADO |
| `testDescontoValidoGerente` | Aplicacao de desconto ate 20% com perfil GERENTE e concluida | APROVADO |
| `testDescontoAcimaDoLimiteLancaExcecao` | Tentativa de desconto superior a 20% e bloqueada com excecao | APROVADO |
| `testBloquearDescontoAposSaida` | Desconto bloqueado se o pedido estiver com status SAIU_PARA_ENTREGA ou RETIRADO | APROVADO |
| `testBloquearCancelamentoAposSaida` | Cancelamento bloqueado para pedidos que ja sairam para entrega | APROVADO |
| `testCancelamentoExigeJustificativa` | Cancelamento sem justificativa textual lanca excecao | APROVADO |

Execucao no terminal:
```text
[INFO] Running com.umc.pediupatrao.service.PedidoServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Testes de Integracao e Seguranca
- Acesso com perfil `atendente` a rota `/dashboard` e `/api/dashboard/**` bloqueado com codigo HTTP 403 (Acesso Negado).
- Acesso com perfil `gerente` e `admin` liberado normalmente.
- Integracao assincrona: chamada direta via navegador e cURL para `/api/dashboard/resumo-completo` retornando JSON consistente com os dados do MongoDB Atlas.

---

## Decisoes de Analise e Projeto de Software

1. **Separacao Arquitetural MVC e REST:** A rota `/dashboard` entrega a estrutura visual HTML via `DashboardWebController` (Thymeleaf), enquanto o `DashboardApiController` atua estritamente como API REST retornando JSON puro. Essa separacao desacopla a camada visual das regras de agregacao de dados.
2. **Uso de DTOs Dedicados:** Classes no pacote `dto` (`MetricasGeraisDTO`, `VendasPeriodoDTO`, `StatusPedidoDTO`, `TopProdutoDTO`, `DashboardResumoDTO`) isolam as entidades do banco de dados, entregando apenas as projecoes necessarias para a interface.
3. **Processamento Assincrono:** O front-end utiliza `fetch()` para consultar a API sempre que o usuario altera o filtro de datas. O Chart.js renderiza os graficos dinamicamente, permitindo atualizacao em tempo real sem recarregar a pagina.
4. **Transacoes Atomicas e Auditoria:** Integracao com `MongoTransactionManager` e `@Transactional` garantindo atomicidade entre o pedido e o registro na trilha de auditoria.
5. **GitFlow:** Desenvolvimento realizado na branch `feature/dashboard-administrativo` a partir da branch principal `main`.

---

## Estrutura do projeto

```
src/main/java/com/umc/pediupatrao/
├── PizzariaPedidosApplication.java
├── config/
│   ├── DataInitializer.java          # Carga de usuarios padrao e cardapio
│   ├── GlobalModelAttributes.java    # Disponibiliza perfil e usuario logado as views
│   ├── MongoTransactionConfig.java   # Configuracao de transacoes atomicas no MongoDB
│   └── SecurityConfig.java           # Regras de autorizacao por perfil, CSRF e rotas
├── controller/
│   ├── AuditoriaController.java      # Consulta da trilha de auditoria
│   ├── ClienteController.java        # API REST de clientes
│   ├── DashboardApiController.java   # API REST do Dashboard (/api/dashboard/**)
│   ├── DashboardWebController.java   # Controlador web da tela (/dashboard)
│   ├── HomeController.java           # Controladores das paginas web (pedidos, clientes)
│   ├── PedidoController.java         # API REST de pedidos
│   ├── ProdutoController.java        # Gestao de produtos do cardapio
│   └── UsuarioController.java        # Gestao de usuarios (restrito ao ADMIN)
├── dto/
│   ├── DashboardResumoDTO.java       # DTO consolidador com todos os blocos do painel
│   ├── MetricasGeraisDTO.java        # DTO com faturamento, ticket medio e cancelamentos
│   ├── StatusPedidoDTO.java          # DTO de distribuicao de pedidos por status
│   ├── TopProdutoDTO.java            # DTO de ranking de produtos mais vendidos
│   └── VendasPeriodoDTO.java         # DTO da serie temporal de vendas dia a dia
├── entity/
│   ├── Auditoria.java                # Modelo para logs de auditoria
│   ├── Cliente.java                  # Cadastro de clientes
│   ├── Pedido.java                   # Entidade do pedido e regras de calculo
│   ├── Produto.java                  # Itens do cardapio
│   └── Usuario.java                  # Usuarios e roles do sistema
├── repository/
│   ├── AuditoriaRepository.java
│   ├── ClienteRepository.java
│   ├── PedidoRepository.java
│   ├── ProdutoRepository.java
│   └── UsuarioRepository.java
└── service/
    ├── AuditoriaService.java         # Gravacao e consulta da auditoria
    ├── ClienteService.java           # Operacoes de clientes com log campo a campo
    ├── DashboardService.java         # Agregacao e calculo de metricas do dashboard
    ├── PedidoService.java            # Validacao de status, precos, descontos e cancelamentos
    ├── ProdutoService.java           # Gestao de produtos
    ├── UsuarioDetailsService.java    # Autenticacao com Spring Security
    └── UsuarioService.java           # CRUD de usuarios e criptografia BCrypt

src/test/java/com/umc/pediupatrao/service/
└── PedidoServiceTest.java            # Suite de testes automatizados com JUnit 5 e Mockito
```
