# Especificação do fluxo de eventos

## 1. Objetivo

Exibir a lista de eventos disponível no Firebase Remote Config, com imagem,
nome, preço em reais e quantidade disponível. A feature é independente de outras features e informa
ao aplicativo ID, nome e preço em centavos por callback; a navegação permanece no `:app`.

## 2. Fonte de dados

O parâmetro de texto `IngressosDisponiveis` do Firebase Remote Config contém a lista.
Ao abrir a tela ou tentar novamente, o aplicativo executa `fetchAndActivate()`
e lê o valor ativado. O SDK usa seu intervalo mínimo padrão de atualização
(normalmente 12 horas); uma busca bem-sucedida dentro desse intervalo pode usar
o valor já ativado.

Se a busca falhar, o último valor remoto ativado, quando existente e válido, é
exibido com aviso de possível desatualização e opção de atualizar. Sem valor
remoto ativado não há fallback de lista vazia: é exibido erro com nova tentativa.
Não há dados de demonstração nem valor padrão local que mascare configuração
ausente. Remote Config é público para o cliente e não deve conter segredos.
O contrato anterior `events_json` e seus IDs textuais não são mais utilizados: após
publicar o novo parâmetro, itens antigos do carrinho que não correspondam a um SKU devem
ser removidos e adicionados novamente.

## 3. Contrato JSON

`IngressosDisponiveis` é um array JSON, inclusive `[]` para uma lista vazia:

```json
[
  {
    "sku": 123,
    "imageUrl": "https://example.com/evento.jpg",
    "name": "Show de exemplo",
    "price": "125.00",
    "quantity": 120
  }
]
```

`sku` é inteiro JSON não negativo e único, limitado a `Long`, e deve permanecer
inalterado para o mesmo produto. É convertido em string para a chave do carrinho.
`imageUrl` e `name` são strings obrigatórias não vazias; a URL deve ser HTTPS
absoluta com host. `quantity` é inteiro JSON não negativo até `Int.MAX_VALUE`;
zero indica esgotado. `price` é número JSON ou string numérica, não negativo, em
reais, exatamente representável em centavos e dentro do limite de `Long`.
Por exemplo, `"49.90"`, `1.2`, `1.20` e `0` são válidos; `1.234`, números
negativos e valores fora do limite são inválidos. Campos adicionais
são ignorados para permitir evolução. Qualquer item inválido invalida a lista
inteira; nenhuma lista parcial é apresentada como sucesso.

## 4. Entidades e modelos

- Documento JSON: array de objetos no contrato acima.
- Domínio `Event`: ID, nome, URL e preço inteiro em centavos, sem dependência
  do Firebase ou da UI. A lista inclui indicador de possível desatualização.
- Apresentação `EventUiModel`: ID, nome, URL e preço formatado em `pt-BR`.
  A conversão decimal para centavos é exata e a formatação usa utilitário de
  moeda compartilhado; não se usa ponto flutuante para dinheiro.

## 5. Estados

Carregando, sucesso com lista não vazia, vazio e erro. Sucesso e vazio também
podem indicar cache possivelmente desatualizado. Há ação de nova tentativa no
erro e no aviso de cache. Falha de imagem afeta apenas a imagem do cartão.

## 6. Regras de negócio

- Preservar a ordem publicada e não filtrar ou inventar eventos.
- Aceitar somente a lista integralmente válida. Um array vazio válido é vazio,
  diferente de parâmetro ausente, JSON inválido ou erro de rede.
- Mostrar imagem com Coil, nome, quantidade e preço em reais; por exemplo, `125.00` vira
  `R$ 125,00`. Falhas de carregamento da imagem exibem uma alternativa visual.
- Evitar buscas simultâneas pela mesma ViewModel. Cancelamentos de coroutine
  são propagados, não transformados em erro de rede.
- Cartões esgotados não podem ser selecionados. O clique envia os dados ao `:app`, que adiciona o evento ao carrinho antes
  de navegar. A lista também oferece acesso ao carrinho sem adicionar item.

## 7. Critérios de aceite

1. Lista válida gera cartões na ordem recebida, com imagem, nome, preço e ID
   de callback corretos. Valores monetários não sofrem arredondamento implícito.
2. `[]` exibe estado vazio; parâmetros ausentes ou listas inválidas exibem erro
   recuperável, não uma lista vazia nem uma lista parcial.
3. Falha de busca usa somente valor remoto ativado válido, acompanhado de aviso
   e ação de atualizar; sem cache válido, mostra erro com nova tentativa.
4. Loading bloqueia solicitações duplicadas; falha de uma imagem não impede
   exibição dos demais dados.
5. Testes com MockK cobrem sucesso, vazio, falhas, cache, concorrência e
   cancelamento; testes de parsing cobrem contrato e limites monetários.

## 8. Falhas e comportamento esperado

Firebase não configurado, chave ausente, busca indisponível ou limitada, JSON
malformado, campo obrigatório ausente, ID duplicado, URL inválida ou preço
inválido apresentam mensagens de erro apropriadas e opção de nova tentativa.
Se houver cache remoto válido após falha de busca, ele é exibido com aviso.
Uma falha de imagem exibe indicação no cartão sem esconder o evento.

## Configuração e verificação

Configure o Firebase do aplicativo conforme `feature/auth/AUTHENTICATION.md`,
crie e publique o parâmetro `IngressosDisponiveis` como texto no Remote Config e execute
`./gradlew :feature:events:testDebugUnitTest`. Teste também em dispositivo com
Firebase configurado: publicação, atualização, cache offline, falha de imagem
e comportamento após expiração do intervalo de busca. A exibição não implica
que o Remote Config seja uma fonte confiável para autorizar valores de compra.
