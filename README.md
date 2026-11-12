# IngressosNaMao

Aplicativo Android para consultar eventos, montar um carrinho e simular a compra
e a emissão de ingressos. O pagamento disponível integra o emulador Cielo Smart
somente em **debug**; os comprovantes e ingressos gerados são simulações, sem
validade de entrada.

## Execução

### Pré-requisitos

- Android Studio compatível com o Android Gradle Plugin do projeto e Android SDK
  com a plataforma 37, build-tools 36.0.0 e platform-tools.
- JDK 25 para o processo Gradle. O projeto usa o Gradle Wrapper e configura
  a resolução de toolchains; o código Android é compilado com compatibilidade Java 11.
- Dispositivo ou emulador com Android 7.0 (API 24) ou superior para o aplicativo.
  Para a integração Cielo, observe a faixa e os requisitos específicos abaixo.
- Acesso à internet para baixar dependências e utilizar os serviços Firebase.

### Configuração

1. Abra o projeto no Android Studio e sincronize o Gradle. Configure o caminho
   do SDK quando solicitado.
2. Para autenticação e eventos, configure um projeto Firebase para o application
   ID `com.innaval.ingresosnamao` e coloque seu `google-services.json` em `app/`.
   O repositório contém um arquivo de configuração; confira se ele corresponde
   ao projeto que você está autorizado a usar ou substitua-o pelo do seu projeto.
3. Habilite autenticação por e-mail/senha e Google. Para o Google, registre os
   certificados SHA-1/SHA-256, configure os clientes OAuth e baixe novamente o
   JSON para disponibilizar `default_web_client_id`. Crie o Firestore e publique
   as regras de `firestore.rules`, conciliando-as com regras já existentes.
4. No Remote Config, publique o parâmetro textual `IngressosDisponiveis` com
   um array JSON válido. O formato e as validações estão em
   [feature/events/EVENTS.md](feature/events/EVENTS.md). Sem configuração
   válida, a tela apresenta erro em vez de eventos fictícios.
5. Se desejar simular pagamento, configure o emulador Cielo e credenciais de
   teste conforme [Integração Cielo Smart](#integração-cielo-smart).

Sem `google-services.json`, a compilação é possível, mas a autenticação informa
erro de configuração; os recursos que dependem do Firebase exigem um projeto
configurado. Não publique credenciais nem use dados reais para testar pagamentos.

### Compilar e executar

Selecione a configuração `app`, a variante `debug` e um dispositivo no Android
Studio; depois execute **Run**. Alternativamente:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

O segundo comando requer um dispositivo conectado ou emulador iniciado. No
Windows, use `gradlew.bat`.

Para executar testes JVM e verificações de qualidade:

```bash
./gradlew testDebugUnitTest
./gradlew :qualityCheck
```

Testes instrumentados exigem dispositivo ou emulador e podem ser executados com
`./gradlew :app:connectedDebugAndroidTest`. Consulte [TESTING.md](TESTING.md)
e [QUALITY.md](QUALITY.md) para escopo, cobertura e limitações.

## Arquitetura

O projeto combina **MVVM**, fluxo de dados unidirecional e princípios de Clean
Architecture. As telas Compose observam estados expostos por ViewModels; ações
da interface são encaminhadas à camada de apresentação. Contratos e regras de
negócio ficam em `domain`, implementações em `data` e a montagem das
dependências em `di`.

- `app`: raiz de composição, navegação e integração entre funcionalidades.
- `core/common`, `core/ui`, `core/network` e `core/database`: tipos e
  infraestrutura compartilhados, design system, rede e persistência.
- `feature/auth`, `events`, `cart`, `payment`, `ticket` e `profile`:
  funcionalidades independentes, sem dependências diretas entre si.

O Koin reúne as dependências na aplicação, e a navegação entre features fica
em `app`. O Room persiste carrinho e ingressos localmente; o Firebase fornece
autenticação, perfil e catálogo de eventos. Valores monetários são
representados em centavos inteiros. Detalhes e regras de dependência estão em
[ARCHITECTURE.md](ARCHITECTURE.md).

## Bibliotecas e serviços

As versões estão centralizadas em
[gradle/libs.versions.toml](gradle/libs.versions.toml).

| Biblioteca ou serviço | Uso e justificativa |
| --- | --- |
| Jetpack Compose, Material 3, Lifecycle e Navigation | Interface declarativa, componentes consistentes, estados associados ao ciclo de vida e navegação. |
| Kotlin Coroutines e Flow | Operações assíncronas e observação de estados sem bloquear a interface. |
| Koin | Injeção de dependências por módulo e substituição de implementações em testes. |
| Room e KSP | Persistência local estruturada e geração do código de acesso ao banco. |
| Firebase Authentication, Firestore e Remote Config | Autenticação, armazenamento de perfil e distribuição do catálogo; Credential Manager e Google Identity viabilizam login Google. |
| Retrofit, OkHttp e kotlinx.serialization | Infraestrutura compartilhada para clientes HTTP e serialização; as URLs de API configuradas são exemplos, não um backend operacional. |
| Coil | Carregamento de imagens dos eventos. |
| ZXing | Geração local dos QR Codes dos ingressos simulados. |
| JUnit, MockK, Robolectric, Compose Test e Espresso | Testes de regras, substituição de dependências e verificação de componentes e fluxos de interface. |
| Detekt e JaCoCo | Análise estática e medição de cobertura de testes. |

## Integração Cielo Smart

**Para reproduzir a integração com o emulador oficial da Cielo, use
preferencialmente um dispositivo ou emulador com Android 7.1 ou Android 10.**
Essas são as versões indicadas na documentação do projeto para esse ambiente;
não se presume compatibilidade com todas as versões intermediárias ou mais
recentes. O suporte do aplicativo a outras versões Android não garante que o
emulador da Cielo funcione nelas.

1. Instale e configure o emulador oficial Cielo no mesmo ambiente Android do
   aplicativo, seguindo a [documentação da Cielo](https://docs.cielo.com.br/cielo-smart/docs/realizando-pagamentos).
   Não instale esse emulador em um terminal Smart.
2. Baixe o [modelo `cielo.local.properties.example`](cielo.local.properties.example)
   e salve uma cópia com o nome `cielo.local.properties` na **raiz do projeto**,
   ao lado do `gradlew`. Se já tiver clonado o repositório, também pode usar
   o comando abaixo, desde que o arquivo local ainda não exista:

   ```bash
   cp cielo.local.properties.example cielo.local.properties
   ```

   Abra a cópia e preencha `CIELO_CLIENT_ID` e `CIELO_ACCESS_TOKEN` com suas
   credenciais de **teste** obtidas para o emulador Cielo. O modelo não contém
   credenciais. O Gradle lê esse arquivo ao configurar o build e incorpora os
   valores no aplicativo debug para que ele possa iniciar a integração.
   Sem credenciais, o projeto ainda compila e pode ser executado, mas não
   consegue iniciar o pagamento; nesse caso, o app informa o erro.

   O arquivo local é ignorado pelo Git e não deve ser compartilhado ou
   versionado. Também é possível informar as mesmas chaves como variáveis de
   ambiente, que têm precedência sobre o arquivo. Os valores aceitam apenas
   letras, números, `_` e `-`. Após alterá-los, sincronize o Gradle e gere e
   instale novamente o APK debug.
3. Compile e execute a variante `debug`, entre no aplicativo, adicione eventos
   ao carrinho e finalize a compra para iniciar a simulação.

O aplicativo valida o carrinho e registra uma tentativa antes de abrir o deep
link `lio://payment`, direcionado ao pacote `br.com.cielosmart.orderservice`.
O pedido inclui itens, total em centavos, credenciais de teste e uma referência;
o retorno é recebido por callback e validado antes de concluir a simulação.
Uma tentativa pendente ou de resultado desconhecido não é reenviada
automaticamente. A referência serve para correlação, não garante deduplicação
pela Cielo. O descarte manual de uma tentativa remove a proteção local contra
duplicidade e não cancela nem concilia uma transação.

**Não use esse fluxo para cobranças reais:** as credenciais de debug são
incorporadas ao APK e podem ser extraídas; o callback não é autenticado e não
comprova pagamento. A variante release não recebe as credenciais e mantém o
pagamento desabilitado. A Cielo Smart é voltada ao ambiente do lojista, não a
um checkout de produção no celular comum do comprador. Para vendas remotas,
avalie Cielo E-commerce e uma integração segura no servidor.
Veja [feature/payment/PAYMENT.md](feature/payment/PAYMENT.md) para estados,
recuperação, limitações e referências oficiais.

## Trade-offs

- **Módulos por feature, camadas por pacote:** isolam funcionalidades sem
  multiplicar módulos Gradle; parte das fronteiras depende de convenções e
  visibilidade, em vez de isolamento completo entre camadas.
- **MVVM com StateFlow:** simplifica a implementação atual; não oferece por si
  só o modelo explícito de intenções e efeitos de uma arquitetura MVI.
- **Persistência local:** permite consultar carrinho e ingressos offline, mas
  não sincroniza ingressos entre dispositivos; limpar dados ou desinstalar o
  aplicativo remove os registros locais.
- **Remote Config para catálogo:** simplifica a publicação de eventos, mas não
  é fonte autoritativa para preço, estoque ou autorização de compra.
- **Simulação Cielo em debug:** permite exercitar o fluxo sem pagamento real,
  mas não resolve autenticação do callback, reconciliação ou emissão confiável.

## Melhorias futuras

- Migrar a apresentação para **MVI**, com intenções, estado e efeitos
  explícitos, preservando as fronteiras entre funcionalidades.
- Implementar backend autoritativo para catálogo, estoque, preços, pedidos,
  confirmação e reconciliação de pagamentos.
- Emitir e validar ingressos no servidor, com proteção contra cópia, uso
  duplicado e revogação; sincronizar dados entre dispositivos.
- Definir uma integração de pagamento adequada ao ambiente de produção, sem
  credenciais sensíveis no aplicativo e com confirmação confiável.
- Ampliar testes em dispositivos reais, acessibilidade e integrações Firebase
  e Cielo; evoluir a proteção contra automação no fluxo de autenticação.

## Documentação complementar

- [Harness do agente](HARNESS_AGENTE.md): contexto verificável, proposta de
  execução, solicitações disponíveis e decisões desta entrega.
- [Arquitetura](ARCHITECTURE.md), [testes](TESTING.md) e [qualidade](QUALITY.md).
- Especificações de [autenticação](feature/auth/AUTHENTICATION.md),
  [eventos](feature/events/EVENTS.md), [carrinho](feature/cart/CART.md),
  [pagamento](feature/payment/PAYMENT.md),
  [ingressos](feature/ticket/TICKET.md) e [perfil](feature/profile/PROFILE.md).
