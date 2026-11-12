# Harness do agente

## Escopo e evidências

Este documento reúne evidências disponíveis na conversa e no repositório e
propõe um **harness** para orientar trabalhos assistidos por IA. Não descreve
uma infraestrutura de agentes automatizada e versionada. O histórico da
conversa disponível está parcialmente resumido; por isso, os pedidos citados
ou resumidos a seguir não representam necessariamente todas as interações.
Os documentos de feature e o código são a referência para o comportamento
atual; as solicitações históricas não comprovam, isoladamente, sua execução.

## Especificações

Um harness é o conjunto de contexto, instruções, ferramentas e verificações
que delimita uma execução do agente. Para este projeto, uma execução
reproduzível deve receber:

1. **Objetivo e escopo:** tarefa solicitada, arquivos afetados e critérios de
   aceite explícitos; perguntas de produto devem ser esclarecidas quando
   impedirem uma decisão segura.
2. **Contexto versionado:** `README.md`, `ARCHITECTURE.md`, a especificação da
   feature envolvida, `TESTING.md`, `QUALITY.md` e os arquivos de código e
   configuração pertinentes. O código prevalece ao investigar divergências;
   divergências relevantes devem ser registradas.
3. **Ferramentas:** leitura e busca no repositório, edição localizada e execução
   dos comandos Gradle necessários. Não são necessárias credenciais reais para
   testes unitários nem para a pipeline de qualidade.
4. **Saída:** alterações rastreáveis, justificativa das decisões e relato
   explícito de verificações executadas, limitações e bloqueios.

## Decisões arquiteturais

- Usar o repositório como fonte de evidências, preservando a divisão entre
  `app`, infraestrutura `core` e funcionalidades independentes em `feature`.
- Manter navegação e composição entre funcionalidades em `app`; evitar
  dependências diretas entre features e preservar as camadas existentes.
- Tratar documentação por feature como contrato de referência, verificando-a
  contra a implementação antes de afirmar comportamento ou compatibilidade.
- Executar a menor verificação adequada à mudança; para alterações funcionais,
  usar testes específicos e ampliar para as verificações de qualidade quando
  necessário. O workflow versionado de qualidade é
  [.github/workflows/quality.yml](.github/workflows/quality.yml).
- Separar validações automatizadas de integrações reais: testes JVM e a CI não
  comprovam funcionamento no Firebase configurado ou no emulador oficial Cielo.

Essas decisões descrevem a arquitetura observada e as orientações propostas
para o harness; não comprovam, por si só, a execução de cada etapa anterior.

## Uso da IA

O histórico disponível registra solicitações de implementação e ajustes
assistidos por IA, incluindo autenticação, eventos, carrinho, perfil, pagamento
e navegação. Nesta entrega, a IA examinou documentos e configurações do
repositório e atualizou os dois arquivos solicitados. A revisão humana continua
necessária para confirmar premissas de produto e validar integrações em
ambientes apropriados.

Como o histórico está parcialmente resumido e não há registro versionado
completo das execuções, não é possível atribuir com precisão a autoria de cada
alteração ou quantificar a participação da IA. Suas saídas devem ser
conferidas; não substituem revisão nem testes.

## Prompt e restrições

### Prompts disponíveis do chat

Os pedidos abaixo preservam o texto disponível, inclusive diferenças de
ortografia e solicitações posteriormente alteradas. Credenciais foram
substituídas por `[credencial omitida]`. Há um trecho de **12 mensagens
omitidas** no histórico recebido; não é possível transcrevê-las. As notas
recuperadas sobre alguns desses pedidos aparecem separadamente, após as
transcrições, e não devem ser tomadas como os prompts originais.

#### Autenticação

> Implemente o fluxo de autenticação do aplicativo.Antes da implementação, produza a especificação do fluxo de autenticação contendo:
>
> 1. Objetivo do fluxo.
> 2. Atores envolvidos.
> 3. Pré-condições e pós-condições.
> 4. Estados possíveis da autenticação.
> 5. Eventos relevantes.
> 6. Regras de negócio.
> 7. Validações de entrada.
> 8. Cenários de erro.
> 9. Critérios de aceite.
>
> Contexto:
> - Firebase Authentication como provedor.
> - Login por e-mail/senha.
> - Login social com Google.
> - Cadastro com nome, e-mail, endereço e senha.
> - reCAPTCHA v3 no login.
> - UI em Jetpack Compose.
> - MVVM + Clean Architecture.
> - Koin para DI.
> - Coroutines e StateFlow.
> - Isolamento total do módulo.
>
> Depois de especificar, implemente:
> - Modelos do domínio
> - Estados de UI
> - UseCases
> - ViewModel
> - Repository
> - Data Sources
> - Telas Compose de login e cadastro
> - Tratamento explícito de erros
> - Estratégia de testes com MockK
>
> Entregue tudo de forma organizada e alinhada à especificação criada.

#### Eventos

> Implemente o fluxo de eventos.Antes da implementação, produza a especificação do fluxo contendo:
>
> 1. Objetivo do fluxo.
> 2. Fonte de dados.
> 3. Estrutura contratual do JSON no Remote Config.
> 4. Entidades e modelos envolvidos.
> 5. Estados de carregamento, sucesso, vazio e erro.
> 6. Regras de negócio para listagem.
> 7. Critérios de aceite.
> 8. Possíveis falhas e comportamento esperado.
>
> Contexto:
> - Os eventos vêm do Firebase Remote Config.
> - Cada evento contém imageUrl, name e price.
> - UI em Jetpack Compose.
> - MVVM + Clean Architecture.
> - Coroutines e StateFlow.
> - Koin para DI.
> - Design System compartilhado.
>
> Depois de especificar, implemente:
> - Parsing da lista JSON
> - Modelos de domínio e UI
> - Repository e UseCases
> - ViewModel
> - Telas Compose
> - Tratamento de erro e estados vazios
> - Testes com MockK
>
> Mantenha o módulo independente e sem dependências cíclicas.

#### Carrinho

> Implemente o fluxo de carrinho. Antes da implementação, produza a especificação do fluxo contendo:
>
> 1. Objetivo do carrinho.
> 2. Regras de persistência offline.
> 3. Estados possíveis do carrinho.
> 4. Ações permitidas ao usuário.
> 5. Regras de cálculo de total.
> 6. Comportamento após reinício do app.
> 7. Tratamento de falhas do banco local.
> 8. Critérios de aceite.
>
> Contexto:
> - Persistência local com Room.
> - O carrinho deve sobreviver ao fechamento do app.
> - O usuário pode adicionar, remover e alterar quantidade.
> - UI em Jetpack Compose.
> - MVVM + Clean Architecture.
> - Coroutines e Flow.
> - Koin para DI.
>
> Depois de especificar, implemente:
> - Entidades Room
> - DAO
> - Data sources
> - Repository
> - UseCases
> - ViewModel
> - Telas Compose
> - Estratégia de restauração do carrinho
> - Testes com MockK
>
> Garanta que o módulo seja independente e livre de acoplamento desnecessário.

#### Cadastro, login social e configuração

> para o cadastro tive o mesmo problema e ainda estou tendo para o login social

> se o local esta errado tire da raiz e coloque onde se espera ou mude a espera para a raiz

#### Identidade visual

> Crie uma Logo para o Aplicativo IngressosNaMao e coloque como SplashScreen, como icone do app, e toda a tela criada terá primeiro essa Logo e depois as views embaixo. também nunca deixe views coladas umas com as outras

#### Perfil

> Implemente o fluxo de Perfil e Antes de implementar, produza a especificação completa do fluxo contendo:
>
> 1. Objetivo do fluxo de perfil.
> 2. Atores envolvidos.
> 3. Dados exibidos na tela.
> 4. Origem dos dados do usuário.
> 5. Estados possíveis da tela.
> 6. Regras de negócio.
> 7. Ações permitidas ao usuário.
> 8. Critérios de aceite.
> 9. Cenários de erro e comportamento esperado.
> 10. Impacto do logout nas sessões e dados locais.
>
> Contexto:
> - O aplicativo usa Firebase Authentication para autenticação.
> - O fluxo de perfil deve exibir os dados do usuário autenticado.
> - Os dados mínimos exibidos devem incluir:
>   - Nome
>   - E-mail
>   - Endereço, se disponível
> - A tela deve conter um botão de logout.
> - O logout deve encerrar a sessão do Firebase Authentication.
> - O logout deve apagar os dados do carrinho (caso seja feito um novo logout carregar os dados)
> - A interface deve ser feita com Jetpack Compose.
> - A arquitetura deve seguir MVVM com Clean Architecture.
> - O módulo deve ser independente e seguir o padrão de modularização por feature.
> - A solução deve usar Coroutines e StateFlow.
> - A injeção de dependências deve ser feita com Koin.
> - O código deve ser organizado, testável e de fácil manutenção.
>
> Depois de especificar, implemente:
> - Modelos de domínio relacionados ao perfil do usuário
> - Estados de UI
> - UseCases necessários
> - ViewModel do fluxo de perfil
> - Repository e Data Source, se necessários
> - Tela Compose do perfil
> - Botão de logout com tratamento explícito de erro
> - Navegação após logout
> - Estratégia de testes com MockK
>
> Requisitos adicionais:
> - O fluxo deve ser consistente com os demais módulos do aplicativo.
> - O botão de logout deve ter confirmação antes da ação, se isso fizer sentido para a experiência do usuário.
> - O fluxo deve tratar estados de carregamento, sucesso e erro de forma clara.
> - Caso algum dado do usuário não esteja disponível, a interface deve exibir um estado apropriado, sem quebrar a experiência.
>
> Entregue a especificação antes da implementação e mantenha a solução alinhada à arquitetura do projeto.

#### Catálogo e quantidade disponível

> O firebase utilizado agora se chama IngressosDisponiveis e ele vem com um json dessa forma por exemplo:
>
> ```json
> [
>   {
>     "imageUrl": "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80",
>     "name": "Festival de Música ao Vivo",
>     "price": "49.90",
>     "quantity": 120
>   },
>   {
>     "imageUrl": "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=800&q=80",
>     "name": "Noite do Samba",
>     "price": "35.00",
>     "quantity": 80
>   },
>   {
>     "imageUrl": "https://images.unsplash.com/photo-1485871981521-5b1fd3805eee?auto=format&fit=crop&w=800&q=80",
>     "name": "Teatro na Praça",
>     "price": "28.50",
>     "quantity": 60
>   },
>   {
>     "imageUrl": "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=800&q=80",
>     "name": "Stand-up Comedy Night",
>     "price": "39.90",
>     "quantity": 150
>   }
> ]
> ```
>
> quero que voce veja uma biblioteca para mostrar a imagem que vem do json e leve em conta a quantidade para caso o usuário selecione mais do que vem o app não permita

O exemplo acima registra a solicitação original; o contrato atual também exige
`sku`, conforme [EVENTS.md](feature/events/EVENTS.md).

#### Pagamento Cielo e integração

> Estou com problema com a integração com o pagamento da cielo
> 1- ele jogou para o aplicativo de pagamento da cielo de testes, porém agora só da a mensagem "Simulação no emulador Cielo. Não utilize credenciais de produção. Há outra tentativa neste dispositivo. Resolva-a antes de inicar outro pagamento"
> 2 - Preciso descobrir qual o problema e como corrigir. Veja a documentação da Cielo Smart caso precisse

> Faça a correção

> consegui descartar, porém como consigo o CLient ID de teste e o Access token de teste? veja os documentos do Cielo Smart se precisar

> estou tendo problema com a volta do app da cielo. Eu faco o teste pagando no debito e ai quando clico em pagar a volta para o meu app esta com resultado desconhecido

> Isso é só um App de case de emprego, pode tirar qualquer bloqueante

> abri um PR para a master e deu conflito resolva para mim por favor

> quero que voce coloque o CLientId de teste e o Access token de teste no gradle e tire qualquer referencia a teste do app. vamos saber que é de teste se tiver em debug, nao precisar ter textos dizendo que algo é de teste. o Cliente Id de teste é [credencial omitida] e o accesstoken é [credencial omitida]. Quando o usuário clicar em finalizar compra voce faz tudo que precisa e já vai para o pagamento no fluxo do Cielo Smart

#### Testes e navegação

> Quero que voce ajuste agora todos os testes para ficarem com nomenclatura no padrao given,when,then

> sobre os testes instrumentados o padrao robot normalmente tem um Action e Result dentro dos arquivos com nome Robot e os testes tbm usam ajuste isso

> Quero criar uma navbar na parte inferior que tenha a tela de Home com os pedidos, meus pedidos com as compras, carrinho e meu perfil

> para o botao de voltar coloque ele numa tabbar na parte superior da tela. em vez de ser um texto coloque uma seta

#### Revisão de ProGuard/R8

> Faça uma revisão do projeto e veja se tem alguma classe ou arquivo que o proguard nao esteja cobrindo

#### Documentação — solicitação mais recente

> Escreva a documentação do aplicativo Android "IngressosNaMao".
>
> Requisitos:
> * Criar um README contendo:
>   - Instruções de execução
>   - Decisões de arquitetura
>   - Bibliotecas externas utilizadas e justificativas
>   - Como foi feita a integração com a Cielo Smart (ressalte a importancia de usar um device com android 7 a 19 para poder funcionar bem a integração)
>   - Trade-offs considerados
>   - Futuras melhorias ( coloque migracao para arquitetura MVI como melhoria)
> * Criar a documentação do harness do agente contendo:
>   - Especificações
>   - Decisões arquiteturais
>   - Uso da IA
>   - Prompt e restrições
>   - Resultados que orientaram a implementação
> * Escrever em português-BR, com clareza, tom profissional e sem redundâncias.
>
> Entregue:
> * Conteúdo do README
> * Conteúdo da documentação do harness do agente
> * Estrutura sugerida para ambos os documentos

Ao esclarecer a divergência com o pedido anterior, o usuário selecionou
**Android 7.1 ou 10, conforme a documentação existente**.

#### Atualização deste registro

> atualize os prompts com todos os prompts feitos neste chat

### Contexto recuperado de mensagens omitidas

O histórico resumido menciona pedidos para emissão de ingressos e QR Code,
Design System, especificação e configuração de injeção Koin, estratégia de
testes e cobertura, automação de qualidade e otimização ProGuard/R8. Os textos
integrais desses pedidos não estão disponíveis neste contexto. As especificações
das features registram as decisões resultantes; por exemplo, a autenticação
documenta que o reCAPTCHA não foi implementado.

### Prompts de documentação registrados anteriormente neste arquivo

**1. Solicitação da documentação**

> Escreva a documentação do aplicativo Android "IngressosNaMao".
>
> Requisitos:
> - Criar um README contendo:
>   - Instruções de execução
>   - Decisões de arquitetura
>   - Bibliotecas externas utilizadas e justificativas
>   - Como foi feita a integração com a Cielo Smart ( ressalte a importancia de usar um device com android 7 a 10 para poder funcionar bem a integração)
>   - Trade-offs considerados
>   - Futuras melhorias ( coloque migração para arquitetura MVI como melhoria)
> - Criar a documentação do harness do agente contendo:
>   - Especificações
>   - Decisões arquiteturais
>   - Uso da IA
>   - Prompt e restrições
>   - Resultados que orientaram a implementação
> - Escrever em português-BR, com clareza, tom profissional e sem redundâncias.
>
> Entregue:
> - Conteúdo do README
> - Conteúdo da documentação do harness do agente
> - Estrutura sugerida para ambos os documentos

**2. Solicitação de registro dos prompts**

> veja todos os prompts desse chat e coloque no HARNESS_AGENTE

Na solicitação atual de documentação, foi inicialmente mencionada a faixa
Android 7 a 19. Após esclarecimento, adotou-se a orientação **Android 7.1 ou
Android 10** para o emulador Cielo, conforme a documentação existente, sem
afirmar suporte às demais versões.

### Restrições propostas para futuras execuções

- Não inventar fatos, resultados de testes, histórico de prompts ou garantias
  de segurança; distinguir evidência, hipótese e proposta.
- Não incluir credenciais, dados pessoais ou dados reais de pagamento em
  prompts, logs, documentos ou arquivos versionados.
- Preservar alterações preexistentes e limitar mudanças ao escopo solicitado.
- Não apresentar a integração Cielo de debug como pagamento de produção nem
  ingressos simulados como válidos para entrada.
- Documentar comandos e limitações de execução com base na configuração
  existente e relatar falhas ou verificações não realizadas.

## Resultados que orientaram a implementação e esta documentação

As observações abaixo são resultados da leitura do repositório nesta entrega;
não afirmam que comandos, testes ou pagamentos tenham sido executados.

| Evidência | Decisão de documentação |
| --- | --- |
| `settings.gradle.kts` e `ARCHITECTURE.md` definem módulos core e features. | Descrever as fronteiras, MVVM e a composição em `app`. |
| `app/build.gradle.kts`, o catálogo Gradle e as especificações Firebase definem SDK, dependências e configuração. | Registrar pré-requisitos, bibliotecas e passos de configuração. |
| `CieloGateway.kt` e `feature/payment/PAYMENT.md` descrevem deep link, callback e limitações de debug. | Explicar o fluxo de simulação e destacar Android 7.1 ou 10 para o emulador oficial. |
| `feature/ticket/TICKET.md` descreve ingressos locais simulados. | Não apresentá-los como comprovante de pagamento ou ingresso válido. |
| `TESTING.md`, `QUALITY.md` e o workflow distinguem testes locais, CI e integrações reais. | Informar comandos e limites da validação automatizada, sem alegar resultados de execução. |

As especificações de autenticação, eventos, carrinho, perfil, pagamento e
ingressos registram as regras e os critérios que orientam os respectivos
fluxos. O checkout conserva a tentativa antes da chamada à Cielo e distingue
resultado desconhecido de aprovação; o catálogo usa SKU e preços em centavos;
o logout prevê limpeza do carrinho. Esses contratos devem ser confrontados com
o código e validados nas integrações reais antes de qualquer publicação.

## Estrutura sugerida

Os documentos foram organizados nesta estrutura, que também pode orientar
alterações futuras:

```text
README.md
  Visão geral
  Execução: pré-requisitos, configuração e comandos
  Arquitetura
  Bibliotecas e serviços
  Integração Cielo Smart
  Trade-offs
  Melhorias futuras
  Documentação complementar

HARNESS_AGENTE.md
  Escopo e evidências
  Especificações
  Decisões arquiteturais
  Uso da IA
  Solicitações disponíveis, prompts e restrições
  Resultados que orientaram a implementação e a documentação
  Estrutura sugerida
```
