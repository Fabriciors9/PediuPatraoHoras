# Pediu Patrão: Sistema de Gestão de Pizzaria

Evolução do sistema web Pediu Patrão desenvolvido em Java com Spring Boot e MongoDB Atlas, implementando controle de acesso por perfis (RBAC), gestão do ciclo de vida de pedidos, cálculos no servidor e trilha de auditoria persistente.

---

## Tecnologias utilizadas

- Linguagem: Java 21 (LTS)
- Framework: Spring Boot 3.4.4
  - Spring Web (MVC e REST)
  - Spring Security (autenticação e autorização por perfis)
  - Spring Data MongoDB (persistência no MongoDB Atlas)
  - Thymeleaf (templates das páginas web)
- Banco de dados: MongoDB Atlas
- Front-end: Bootstrap 5.3, Bootstrap Icons e JavaScript Vanilla

---

## Perfis de acesso e permissões

O sistema conta com três perfis de acesso distintos, validados nas rotas e nos serviços do backend:

| Perfil | Usuário padrão | Senha | Responsabilidades e permissões |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Gerenciar usuários e seus perfis, consultar pedidos e visualizar a auditoria. Não aplica desconto e não cancela pedidos. |
| **GERENTE** | `gerente` | `gerente123` | Gerenciar pedidos, exclusividade para desconto (até 20%) e cancelamento (com justificativa obrigatória), consultar clientes e visualizar a auditoria. |
| **ATENDENTE** | `atendente` | `atendente123` | Cadastrar e atualizar clientes, registrar novos pedidos e acompanhar o andamento. Não tem acesso a desconto, cancelamento, gestão de produtos, usuários e auditoria. |

*** As restrições de acesso foram configuradas diretamente no `SecurityConfig.java` e validadas também no `PedidoService.java`. Ocultar botões na tela serve apenas para a experiência visual: se um atendente tentar chamar diretamente rotas como `/usuarios` ou `/pedidos/cancelar`, o Spring Security bloqueia a requisição e retorna erro 403 (Acesso Negado).

---

## Como executar o projeto

### Pré-requisitos
- Repositório clonado localmente
- Java 21 instalado e apontado nas variáveis de ambiente
- Instância do MongoDB ativa (seja local ou no MongoDB Atlas)
- O projeto inclui o Maven Wrapper (`mvnw.cmd` e `mvnw`), dispensando instalação manual do Maven

### Configuração do Banco de Dados
Antes de rodar a aplicação, abra o arquivo `src/main/resources/application.properties` e informe a sua URL de conexão na propriedade `spring.data.mongodb.uri`:

```properties
spring.data.mongodb.uri=sua_string_de_conexao_do_mongodb_aqui
spring.data.mongodb.database=pizzaria
```

O nome do banco deve permanecer definido como `pizzaria`.

### Execução no Windows
Você pode rodar por qualquer uma das opções abaixo a partir da pasta raiz do projeto:

1. Dando dois cliques no arquivo `executar.bat`
2. Pelo PowerShell:
   ```powershell
   .\executar.ps1
   ```
3. Pelo Maven Wrapper direto no terminal:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

Após inicializar, acesse no navegador: `http://localhost:8080`

A classe `DataInitializer.java` verifica a base no MongoDB na primeira execução e cria automaticamente o usuário inicial `admin` (senha `admin123`), além dos produtos iniciais do cardápio.

---

## Requisitos implementados

### RF01: Controle de usuários e permissões
- Perfis `ADMIN`, `GERENTE` e `ATENDENTE` persistidos no MongoDB na coleção `usuarios`.
- Criptografia de senhas com `BCryptPasswordEncoder`.
- Regras de autorização por perfil no `SecurityConfig` para páginas e APIs.
- Bloqueio contra autoelevação: somente o perfil `ADMIN` tem acesso ao cadastro e edição de usuários e papéis.
- Tratamento de CSRF nos formulários e tela personalizada para HTTP 403 (Acesso Negado).

### RF02: Evolução do cadastro de pedidos
- Entidade `Pedido` atualizada com sabor, tipo de pizza (tamanho fixo em 8 pedaços), quantidade, preço unitário, subtotal, desconto percentual, valor do desconto, valor total, atendente responsável e data/hora de abertura.
- Cálculo de valores executado exclusivamente no backend pelo método `calcularValores` do `PedidoService`. Com isso, o cliente não consegue enviar totais arbitrários pelo navegador ou requisições HTTP.

### RF03: Controle de entrada, saída e ciclo de vida do pedido
- Fluxo de status com validação rígida no `PedidoService`:
  1. `RECEBIDO`: registra o horário de entrada e o usuário autenticado.
  2. `EM_PREPARACAO`: encaminhado para a produção na cozinha.
  3. `PRONTO`: pronto na bancada, aguardando saída.
  4. `SAIU_PARA_ENTREGA` ou `RETIRADO`: registra obrigatoriamente a data/hora e o responsável pelo despacho.
  5. `FINALIZADO`: ciclo concluído.
- Bloqueio de transições incoerentes (por exemplo, tentar pular de `RECEBIDO` direto para `FINALIZADO`).
- Bloqueio total de novas alterações de status após o pedido estar cancelado ou finalizado.

### RF04: Cancelamentos e descontos exclusivos do gerente
- Operações permitidas apenas para o perfil `GERENTE`, com trava no Spring Security e no serviço.
- Desconto: percentual aplicado sobre o valor do pedido, limitado a no máximo 20%, e permitido apenas antes da saída do pedido (em `PRONTO`). Também aceita 0% caso seja necessário remover um desconto aplicado anteriormente.
- Cancelamento: permitido apenas antes da saída, exigindo justificativa textual obrigatória, preservando os dados originais e impedindo transições posteriores.
- Ambas as operações registram evento detalhado na auditoria com valores anteriores e novos.

### RF05: Auditoria persistente no MongoDB
- Coleção `auditoria` gravando inclusões, alterações cadastrais e movimentações de pedidos.
- Monitoramento de dados de clientes campo a campo: alterações de logradouro, número, bairro, telefone e nome gravam o valor antes e o valor depois.
- Cada registro armazena:
  - Usuário responsável e seu perfil
  - Data e hora da operação
  - Ação executada e entidade afetada com seu respectivo ID
  - Campos modificados com valor anterior e valor novo
  - Justificativa informada (nos cancelamentos)
- Consulta restrita aos perfis `ADMIN` e `GERENTE`. A interface e as APIs não fornecem endpoints para editar ou excluir dados da auditoria.
- Credenciais e senhas não são registradas nos logs de auditoria.

---

## Principais alterações realizadas

- Adequação dos perfis de acesso: substituição do modelo inicial genérico (apenas ADMIN e USER) para três papéis de negócio (ADMIN, GERENTE e ATENDENTE), com regras de autorização granulares nas rotas e bloqueio no backend.
- Ciclo de vida dos pedidos: implementação de máquina de estados controlada, impedindo transições inválidas e registrando os responsáveis e horários de entrada e saída.
- Regras de negócio e cálculo no backend: processamento de subtotais, descontos de até 20% e valor total executado no servidor.
- Ações exclusivas de gerência: permissão restrita para cancelamento com justificativa e aplicação de desconto percentual antes do despacho.
- Trilha de auditoria persistente: criação da entidade e repositório de Auditoria no MongoDB, registrando data, hora, usuário, operação e alterações cadastrais de clientes campo a campo (com valor anterior e novo).
- CRUD de produtos: módulo completo para cadastro, edição, exclusão e listagem de itens do cardápio, restrito aos perfis administrativos.

---

## Estrutura do projeto

```
src/main/java/com/umc/pediupatrao/
├── PizzariaPedidosApplication.java   # Classe principal de inicialização da aplicação
├── config/
│   ├── DataInitializer.java          # Carga inicial dos usuários e cardápio
│   ├── GlobalModelAttributes.java    # Disponibiliza perfil e usuário logado às views
│   └── SecurityConfig.java           # Regras de autorização por perfil e CSRF
├── controller/
│   ├── AuditoriaController.java      # Tela de consulta da auditoria
│   ├── ClienteController.java        # API REST de clientes
│   ├── HomeController.java           # Controladores das páginas web (pedidos, clientes)
│   ├── PedidoController.java         # API REST de pedidos
│   ├── ProdutoController.java        # Gestão de produtos do cardápio
│   └── UsuarioController.java        # Gestão de usuários (restrito ao ADMIN)
├── entity/
│   ├── Auditoria.java                # Modelo para logs de auditoria
│   ├── Cliente.java                  # Cadastro de clientes
│   ├── Pedido.java                   # Entidade do pedido e regras de cálculo
│   ├── Produto.java                  # Itens do cardápio
│   └── Usuario.java                  # Usuários e roles do sistema
├── repository/
│   ├── AuditoriaRepository.java
│   ├── ClienteRepository.java
│   ├── PedidoRepository.java
│   ├── ProdutoRepository.java
│   └── UsuarioRepository.java
└── service/
    ├── AuditoriaService.java         # Gravação e consulta da auditoria
    ├── ClienteService.java           # Operações de clientes com log campo a campo
    ├── PedidoService.java            # Validação de status, descontos e cancelamentos
    ├── ProdutoService.java           # Gestão de produtos
    ├── UsuarioDetailsService.java    # Autenticação com Spring Security
    └── UsuarioService.java           # CRUD de usuários e criptografia BCrypt
```
