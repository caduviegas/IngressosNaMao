# Estratégia de testes

## Objetivo

Detectar regressões nos fluxos e nos estados visíveis ao usuário, preservar as regras críticas
de compra e fornecer uma medição reproduzível da cobertura do código de produção.

## Escopo

- **Unitários JVM:** regras de domínio, validações, cálculo monetário, use cases, repositórios,
  parsing, ViewModels, cancelamento e tradução de erros. Usar JUnit, MockK, `coroutines-test`,
  fakes ou fontes em memória; não acessar Firebase, rede nem Cielo reais.
- **Android na JVM com Robolectric:** persistência Room em memória, comportamento de componentes
  Android e telas Compose. Os testes usam `src/test`, `RobolectricTestRunner`, APIs de testes
  Compose e Robot Pattern: cada robot concentra ações e verificações semânticas da tela.
- **Instrumentados em dispositivo/emulador:** `src/androidTest` com AndroidJUnitRunner para
  integrações que exigem Android real. Robolectric não é um teste instrumentado em dispositivo:
  simula o ambiente Android na JVM e não substitui testes em dispositivo, Firebase configurado,
  acessibilidade manual ou o emulador oficial da Cielo.

Cada feature mantém seus testes em `feature/<nome>/src/test`; testes de infraestrutura ficam no
core correspondente, e testes de composição e navegação em `app/src/test`. Testes de UI usam
robots próprios da feature sem introduzir dependências entre features. Os arquivos `*Robot.kt`
separam interações em `Action` e verificações de interface em `Result`; os testes em
`*RobotTest.kt` usam essas duas partes. A mesma estrutura é usada em testes Compose instrumentados
e Robolectric, sem substituir testes em dispositivo por simulação JVM. O banco de testes é em
memória e os contêineres Koin são isolados e encerrados.

Nomeie cada teste no formato `given<Contexto>When<Acao>Then<Resultado>`, usando camelCase
para manter compatibilidade com os testes JVM e instrumentados. O nome deve descrever o cenário
e o comportamento verificado; helpers, configurações e classes não precisam seguir esse formato.

## Estados críticos

| Fluxo | Comportamentos a verificar |
|---|---|
| Auth | validação, carregamento, sessão/perfil, falha de rede ou configuração, cancelamento Google, erro e recuperação |
| Events | carregando, lista, vazio, dados inválidos, indisponibilidade, cache possivelmente desatualizado e nova tentativa |
| Cart | vazio, itens e total, quantidade e remoção, persistência offline, falha de armazenamento e carrinho alterado após pagamento |
| Payment | pronto, aguardando, aprovado, cancelado/erro, desconhecido/timeout, erro de armazenamento e bloqueio de cobrança duplicada |
| Ticket | emissão somente para compra válida, idempotência, isolamento por usuário, carregando/vazio/erro, comprovante e QR local |
| Profile | dados ausentes, cache, confirmação, falha e ordem do logout, isolamento do carrinho e remoção da pilha autenticada |

## Cobertura e critérios de aceite

JaCoCo mede **linhas executadas de código de produção da variante debug**, agregadas em `:app`,
`core` e todas as features. O denominador inclui domínio, dados, apresentação, navegação e DI;
apenas artefatos gerados (por exemplo `R`, `BuildConfig`, implementação gerada pelo Room e
singletons gerados pelo compilador Compose) são excluídos. Classes sem testes continuam no
denominador. Relatórios individuais por módulo permitem identificar lacunas que a média
agregada poderia ocultar.

O objetivo de aceite é **ao menos 80% das linhas agregadas**, com todos os testes passando e
validação explícita dos estados críticos acima. Cobertura não prova correção, nem substitui
testes de segurança, integrações reais ou avaliação de acessibilidade. A verificação de 80% é
uma tarefa separada que falha quando a meta não é atingida; o relatório deve poder ser gerado
mesmo enquanto existirem lacunas.

## Estratégia para atingir 80%

1. Gerar o relatório agregado e os relatórios por módulo; registrar o resultado real sem alterar
   o denominador para esconder código não testado.
2. Priorizar os ramos críticos de domínio e dados (valores-limite, idempotência, erros e
   cancelamento); usar MockK e armazenamento em memória para casos determinísticos.
3. Ampliar robots Compose para ações e estados visíveis de cada fluxo, depois cobrir DI,
   navegação e infraestrutura. Revisar também ramificações não cobertas, não só a média de linhas.
4. Executar a verificação de 80% na integração contínua e publicar os relatórios; investigar
   regressões e lacunas de cenários mesmo quando a média estiver acima da meta.

## Execução

- `./gradlew testDebugUnitTest`: testes JVM, incluindo Robolectric.
- `./gradlew :jacocoDebugReport`: relatório agregado HTML e XML em
  `build/reports/jacoco/jacocoDebugReport/`.
- `./gradlew :feature:auth:jacocoDebugReport` (ou outro módulo): relatório individual.
- `./gradlew :jacocoDebugCoverageVerification`: executa os testes e exige cobertura agregada ≥ 80%.
- `./gradlew :app:connectedDebugAndroidTest`: testes instrumentados em dispositivo/emulador Android.

Os relatórios executam os testes JVM de todos os módulos antes da medição, inclusive para relatórios
individuais, pois um módulo pode ser exercitado por testes de outro. Como a cobertura é baseada na
JVM, ela não incorpora automaticamente a execução de `connectedDebugAndroidTest`; resultados
desses testes devem ser acompanhados separadamente.

O processo de análise estática e automação está descrito em [QUALITY.md](QUALITY.md).
