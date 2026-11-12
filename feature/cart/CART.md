# Especificação do fluxo de carrinho

## 1. Objetivo

Permitir adicionar eventos, alterar quantidades, remover itens e consultar o
total, inclusive offline. O carrinho pertence à conta Firebase autenticada e
fica isolado de carrinhos de outras contas no mesmo dispositivo.

## 2. Persistência offline

O Room persiste os itens em `ingressos.db`; a chave composta é UID da conta e
ID do evento. Cada item guarda nome, preço unitário em centavos e quantidade.
As alterações são transacionais e só são consideradas concluídas após a
gravação; não dependem de rede. O carrinho é local, não sincronizado entre
dispositivos, e não constitui reserva de estoque ou preço garantido.

O banco compartilhado migra da versão 1 para a 2 sem excluir ingressos ou
usar migração destrutiva. `Flow` do Room é a fonte de verdade da tela.

## 3. Estados

Carregando, vazio, conteúdo, operação em andamento e erro recuperável. Falhas
na leitura não são apresentadas como carrinho vazio; erros de escrita mantêm
os dados confirmados e são informados ao usuário. Após pagamento confirmado,
há uma etapa de limpeza com possibilidade de repetição se o banco falhar.

## 4. Ações

- Adicionar evento selecionado; se já existir, somar uma unidade e atualizar
  nome e preço unitário para os valores selecionados mais recentes.
- Aumentar ou diminuir quantidade, mantendo mínimo de uma unidade. Remoção
  exige ação própria.
- Consultar o carrinho a partir da listagem, mesmo se eventos estiverem
  indisponíveis; voltar às compras e iniciar pagamento apenas quando não vazio.
- Se o total for zero, informar que a finalização gratuita ainda não é
  suportada e não iniciar pagamento.
- Após pagamento confirmado, limpar o carrinho da conta antes de prosseguir,
  desde que corresponda à versão enviada ao pagamento. Se houver alterações
  concorrentes, preservar os itens e avisar para revisão antes de outra compra.
- No fluxo de simulação, emitir os ingressos no banco antes de limpar o
  carrinho. A compra deve conter até 100 ingressos; o limite é verificado antes
  de iniciar o pagamento. Falha de emissão permite repetição sem nova cobrança.

## 5. Cálculo do total

Subtotal = preço unitário em centavos × quantidade; total = soma dos subtotais,
sem ponto flutuante, frete ou taxas. São aceitos apenas preço não negativo,
quantidade positiva e identificadores/nome não vazios. Operações que excedam
`Long.MAX_VALUE` em centavos ou o limite de quantidade de `Int` são rejeitadas
sem alterar o banco. Valores são formatados em reais pelo design system.

## 6. Reinício do aplicativo

Ao entrar novamente com a mesma conta, a observação do Room restaura os dados
persistidos; outra conta vê apenas os seus. Para acesso offline após reinício,
é necessária sessão Firebase existente e perfil previamente confirmado no
cache local do Firestore, sem gravações pendentes. Se não existir cache válido,
o login continua exigindo conexão. A sessão em cache não confirma revogação ou
alterações de conta enquanto offline.

## 7. Falhas do banco

Falhas de abertura, migração, leitura ou gravação exibem erro explícito e
permitem tentar novamente; nunca se apagam dados para recuperar o banco.
Operações inválidas, item inexistente e estouro de valores são informados sem
modificar o carrinho. Cancelamento de coroutine é propagado. Se o pagamento
for confirmado mas a limpeza falhar, não se solicita novo pagamento: o usuário
deve repetir somente a limpeza.

Pagamento remoto e banco local não compartilham transação. Se o processo for
interrompido entre a confirmação remota e o registro local da próxima etapa, a
reconciliação automática exige suporte a idempotência/consulta no backend de
pagamentos, ainda inexistente no projeto. O valor calculado pelo cliente deve
ser revalidado pelo servidor antes de cobrar.

## 8. Critérios de aceite

1. Adicionar, alterar quantidade e remover atualiza a tela e permanece após
   reinício; adicionar novamente incrementa e atualiza o preço.
2. Contas distintas não leem nem modificam os carrinhos umas das outras.
3. Carrinho vazio, carregamento, conteúdo e erros são distinguíveis; operações
   e totais inválidos não provocam escrita nem transbordamento.
4. Migração 1→2 preserva ingressos existentes; falhas não apagam dados.
5. Sessão e perfil em cache previamente confirmados permitem restauração offline;
   sem cache válido, falha de autenticação permanece explícita.
6. Pagamento confirmado limpa o carrinho; falha de limpeza oferece repetição
   sem repetir o pagamento.
7. Testes com MockK cobrem regras, falhas, isolamento, restauração observável e
   cancelamento. Testes com Robolectric e Room verificam migração, concorrência,
   isolamento e reabertura do banco. A validação em dispositivo deve cobrir
   reinício offline e integração com Firebase e pagamentos reais.

## Verificação

Execute:

```shell
./gradlew :feature:cart:testDebugUnitTest :feature:auth:testDebugUnitTest :feature:events:testDebugUnitTest :feature:payment:testDebugUnitTest :app:assembleDebug
```

Para testar a restauração offline no aplicativo, primeiro é necessário
configurar o Firebase, autenticar e confirmar o perfil online.
