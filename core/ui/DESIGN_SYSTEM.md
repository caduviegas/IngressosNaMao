# Especificação do Design System

## 1. Objetivo
Fornecer uma linguagem visual e componentes Compose compartilhados, consistentes e acessíveis às features, sem dependência de navegação, regras de negócio ou outros módulos do projeto.

## 2. Tokens visuais
- Cores semânticas Material 3: primária vermelha, secundária roxa, superfícies, fundos, bordas e erro, com papéis `on*` correspondentes e esquemas claro/escuro. Sucesso e suas cores de conteúdo e container são tokens adicionais.
- Tipografia do sistema usando escalas Material em `sp`, com hierarquia de títulos, corpo e rótulos; sem fonte remota.
- Shapes arredondados nos tamanhos extra pequeno, pequeno, médio, grande e extra grande.
- Espaçamentos de 4, 8, 16, 24 e 32 dp; tamanho mínimo de alvo interativo de 48 dp.
- O tema segue a configuração clara/escura do sistema por padrão e permite sobrescrita explícita; não depende de cores dinâmicas do aparelho.

## 3. Componentes reutilizáveis
Botões primário, secundário tonal, contornado e textual; campo de texto com rótulo, ajuda, erro e configuração de teclado/senha; card informativo ou clicável; indicador de progresso inline, estados de carregamento, erro e vazio; e top bar com slots de navegação e ações. Componentes não executam operações ou navegam por conta própria.

## 4. Estados dos componentes
Botões suportam habilitado, desabilitado e carregando, além dos estados de foco e pressão oferecidos pelo Material. Campos suportam edição, somente leitura, desabilitado, foco e erro com mensagem. Cards clicáveis suportam habilitado, desabilitado, foco e pressão. Loading é indeterminado; erro oferece repetição opcionalmente configurada; vazio admite ação opcional. O chamador controla os estados e callbacks.

## 5. Acessibilidade
- Pares de texto e fundo definidos pelo sistema devem atingir contraste WCAG de pelo menos 4,5:1 para texto normal; ícones e indicadores relevantes, 3:1. Elementos desabilitados seguem o tratamento Material e não devem ser a única explicação de uma ação indisponível.
- Usar unidades `sp`, respeitar escala de fonte e evitar alturas fixas para texto. Controles interativos devem oferecer alvo mínimo de 48 × 48 dp.
- Rótulos permanecem visíveis/acessíveis; erros têm texto e semântica de erro, não dependem somente de cor. Loading tem descrição e semântica de progresso; mensagens de erro são anunciáveis.
- Ícones de navegação e ação devem ter descrição fornecida pelo chamador; ícones decorativos devem ser marcados como tais. O sistema não deve comunicar significado apenas por cor.
- Verificações automatizadas de contraste e semântica complementam, mas não substituem, testes com TalkBack, escala de fonte ampliada, teclado e dispositivos reais.

## 6. Uso entre módulos
`:app` aplica `IngressosTheme` uma vez na raiz. Features dependem apenas de `:core:ui`, usam `MaterialTheme.colorScheme/typography/shapes`, `IngressosTheme.spacing/extras` e os componentes compartilhados. A paleta bruta é interna. Textos específicos e decisões de navegação permanecem nas features; textos padrão dos componentes usam recursos Android. Alterações não devem criar dependências entre features nem quebrar as chamadas existentes de botão, loading e erro.

## 7. Critérios de aceite
1. Tema claro e escuro expõem cores, tipografia, shapes e espaçamentos consistentes, com contraste verificável nos pares semânticos documentados.
2. Todos os componentes listados podem ser usados isoladamente com estados controlados e callbacks; não há lógica de negócio no módulo.
3. Botão em carregamento ou desabilitado não executa ação; campo de erro expõe descrição textual; componentes interativos respeitam tamanho mínimo.
4. Loading, erro e vazio são distinguíveis e acessíveis. Cada tela escolhe a ação adequada; ausência de dados não é confundida com falha.
5. As features existentes usam os componentes compartilhados em superfícies equivalentes, preservando comportamento e fluxos existentes.
6. Testes cobrem contraste, tokens e semântica/interação quando executados em ambiente Android; builds dos módulos consumidores permanecem compatíveis.

## Uso

```kotlin
IngressosTheme {
    IngressosTextField(
        value = email,
        onValueChange = onEmailChanged,
        label = "E-mail",
        errorMessage = emailError,
    )
    IngressosButton("Continuar", onContinue, loading = submitting)
}
```

`IngressosButton` mantém largura total por padrão; use `style` para as variantes. `IngressosCard` tem sobrecargas informativa e clicável. `IngressosTopBar` recebe slots para ações e navegação, cujos ícones devem ser identificados pelo chamador. `LoadingState`, `ErrorState` e `EmptyState` ocupam a área disponível, portanto devem ser colocados em um contêiner apropriado; para progresso inline use `IngressosProgressIndicator`. O erro pode não ter ação ou receber o rótulo e callback apropriados, por exemplo `retryLabel = "Voltar"`.

Os testes locais de contraste e semântica podem ser executados com `./gradlew :core:ui:testDebugUnitTest`. A validação manual com TalkBack e fontes ampliadas continua necessária.
