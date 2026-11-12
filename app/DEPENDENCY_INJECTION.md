# Especificação de injeção de dependências

## Objetivo

Compor o aplicativo com Koin, mantendo contratos, implementação e apresentação nas respectivas
features. A raiz de composição pertence exclusivamente a `:app`; nenhuma feature conhece outra.

## Dependências compartilhadas

- `:core:common`: `DispatcherProvider` singleton.
- `:core:network`: configuração de ambiente fornecida por `:app`, `Json`, `OkHttpClient` e
  `Retrofit` singletons. A configuração vem de `BuildConfig`, não das features.
- `:core:database`: `AppDatabase` e seus DAOs singletons. O banco conserva as migrações existentes
  e é fechado quando o contêiner é encerrado.
- `:core:ui`: tema e componentes Compose; não possui serviços injetáveis e não requer módulo Koin.
- O `Application` é o contexto Android compartilhado; não reter `Activity` em singletons.

## Dependências por feature

| Feature | Singletons | Factories | ViewModels |
|---|---|---|---|
| Auth | fontes Firebase/Firestore e `AuthRepository` | use cases de autenticação e perfil | `AuthViewModel` |
| Events | fonte Remote Config, parser e `EventRepository` | `GetEventsUseCase` | `EventsViewModel` |
| Cart | fonte local e `CartRepository` | use cases do carrinho | `CartViewModel` (`userId`), `CartCleanupViewModel` (`userId`, fingerprint) |
| Payment | `PaymentGateway` e `PaymentRepository` | `StartPaymentUseCase` | `PaymentViewModel` |
| Ticket | `TicketRepository` e `QrCodeGenerator` | use cases de emissão e consulta | `TicketsViewModel` (`userId`), `ReceiptViewModel` (`userId`, referência), `TicketDetailViewModel` (`userId`, ID) |

O repositório de pagamento deve permanecer singleton, pois coordena a tentativa e o callback.
Os clientes Firebase são obtidos pelas fontes ao serem usados: sua inicialização não deve ser
antecipada, para preservar os erros explícitos quando a configuração está ausente. As definições
de ViewModel são factories; a retenção durante o ciclo de vida é responsabilidade do
`ViewModelStore` Android e de `koinViewModel`.

## Regras de acoplamento e testes

- Cada core com serviços e cada feature exporta uma função que cria seu módulo Koin. As funções
  não acessam `:app` nem outras features. A navegação e os dados de ambiente são responsabilidade
  de `:app`.
- Cada composição deve chamar essas funções para obter definições novas. Em Koin 4.1, reutilizar
  a mesma instância de `Module` entre contêineres também pode compartilhar o estado de seus
  singletons; encerrar um contêiner poderia afetar o outro.
- Produção rejeita definições duplicadas em vez de aceitar substituições acidentais.
- Testes usam `koinApplication` independente, novas instâncias dos módulos e overrides explícitos
  carregados por último, com `allowOverride(true)` apenas no contêiner de teste. Dependências
  externas devem ser substituídas antes da primeira resolução; não usar Firebase, rede, gateway
  real ou banco persistente em testes de comportamento.
- O contêiner de teste deve ser encerrado ao final. Repositórios e serviços são substituídos por
  seus contratos; dependências externas também podem ser trocadas por banco Room em memória.
  Parâmetros de navegação continuam sendo fornecidos na criação de cada ViewModel.

## Inicialização

O Android instancia `IngressosApp`, declarado no manifest. Seu `onCreate` chama `startKoin`
uma única vez, registra o contexto da aplicação e carrega uma nova composição dos módulos de
configuração, infraestrutura e features. O Koin é inicializado antes de atividades, telas ou
serviços resolverem dependências. Nenhuma feature inicia ou encerra o contêiner global.

## Critérios de aceite

1. A composição resolve os serviços, repositórios, use cases e ViewModels de todas as features,
   incluindo os parâmetros corretos dos ViewModels parametrizados.
2. Singletons são compartilhados dentro do mesmo contêiner; use cases são criados por resolução;
   composições independentes não compartilham singletons.
3. Substituições de teste são explícitas e não alteram a composição de produção ou outros testes.
4. A aplicação inicia o Koin uma vez, com contexto de aplicação e sem registros duplicados.
5. Ausência de configuração Firebase continua sendo tratada pelo fluxo apropriado, sem falhar
   antecipadamente na composição.
6. O grafo permanece acíclico; `:core:ui` não recebe módulo Koin vazio. Testes não executam
   integrações externas e demonstram tanto a resolução do grafo quanto a troca de contratos.
