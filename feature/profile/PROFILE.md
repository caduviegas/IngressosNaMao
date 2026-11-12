# Especificação do fluxo de perfil

## 1. Objetivo

Permitir que a pessoa autenticada consulte seu perfil e encerre a sessão com segurança,
apagando definitivamente seu carrinho local antes do logout.

## 2. Atores

Pessoa usuária, aplicativo, Firebase Authentication, Cloud Firestore e banco local Room.

## 3. Dados exibidos

Nome, e-mail e endereço. Campos ausentes ou vazios são identificados como não informados;
nenhuma senha, token ou informação de pagamento é exibida. O perfil é somente leitura.

## 4. Origem dos dados

O UID e os dados básicos vêm da sessão atual do Firebase Authentication. O documento
`users/{uid}` no Firestore fornece nome e endereço e, quando necessário, e-mail. A leitura
consulta o servidor; em indisponibilidade ou timeout pode usar um documento previamente
confirmado no cache, sem escritas pendentes. Nesse caso a tela informa que os dados podem
estar desatualizados. Documento inexistente ou campos ausentes não impedem a exibição dos
dados disponíveis; dados de tipos inválidos ou e-mail conflitante são tratados como erro.

## 5. Estados

Carregando, perfil disponível, confirmação de logout, logout em andamento, erro recuperável
de leitura ou logout e sessão ausente/encerrada. Em erro de leitura a pessoa pode tentar
novamente, voltar ou sair; ao sair é exibida confirmação.

## 6. Regras de negócio

- Usar sempre o UID da sessão atual, nunca um UID informado pela interface.
- Antes do logout, apagar todos os itens Room apenas do UID autenticado; uma falha impede
  encerrar a sessão e permite nova tentativa. A operação é idempotente para carrinho vazio.
- A saída nas telas de recuperação do cadastro usa a mesma operação; não existe atalho de
  interface que encerre a sessão sem passar pela limpeza do carrinho.
- Conferir a identidade antes da limpeza e imediatamente antes de encerrar a sessão; uma
  mudança de usuário interrompe a operação sem desconectar a nova conta.
- A limpeza e o Firebase Authentication não formam uma transação: se o logout falhar após
  a limpeza, o carrinho já estará vazio e o erro será apresentado. Cancelamento de
  corrotinas é propagado. Nenhuma falha é apresentada como sucesso.
- Bloquear ações concorrentes durante a leitura ou logout e evitar confirmações duplicadas.

## 7. Ações

Abrir o perfil a partir de eventos, voltar, repetir leitura, solicitar logout, cancelar ou
confirmar logout. Após confirmação bem-sucedida, limpar a pilha de navegação autenticada e
abrir login; voltar pelo sistema não retorna à área autenticada.

## 8. Critérios de aceite

1. Perfil do usuário atual apresenta nome, e-mail e endereço disponível, com fallback para
   dados ausentes e indicador explícito quando usa cache confirmado.
2. Leitura, falha e repetição são visíveis; ausência de sessão retorna ao login.
3. Logout exige confirmação, remove o carrinho do UID atual e encerra Firebase Authentication.
   No próximo login o carrinho desse usuário começa vazio; carrinhos de outros UIDs não mudam.
4. Falha de limpeza mantém a sessão, informa o erro e permite repetir. Falha após limpeza
   informa que o carrinho já foi apagado e não apresenta logout como concluído.
5. Sessão alterada durante o processo não desconecta outra conta. Logout bem-sucedido não
   permite retornar às telas autenticadas usando a pilha anterior.
6. A feature depende de abstrações para limpeza de carrinho e não depende de outras features;
   a composição e navegação entre módulos permanecem em `:app`.
7. Testes com MockK cobrem estados, ausência de dados, falhas, confirmação, concorrência,
   cancelamento e ordem de limpeza/logout; testes Room verificam isolamento por usuário.

## 9. Falhas e comportamento

Erros de configuração Firebase, rede, leitura/permissão Firestore, dados inválidos,
armazenamento local e saída do Firebase são explícitos e recuperáveis quando possível.
Não substituir erros de permissão ou dados inválidos por um perfil vazio. Uma troca de
sessão durante uma operação informa que a operação foi interrompida. Cache é usado
somente em indisponibilidade ou timeout, não em erro de autorização.

## 10. Sessão e dados locais

O logout encerra a sessão local do Firebase Authentication, mas não exclui a conta remota,
não revoga tokens em todos os dispositivos e não desconecta a conta Google do aparelho.
O carrinho local do usuário é apagado; documentos Firestore, ingressos simulados e registro
de tentativa de pagamento permanecem, para preservar histórico e proteção contra duplicidade.
O cache Firestore do SDK não é apagado; regras de segurança continuam protegendo o servidor.
Se a sessão for encerrada fora deste fluxo ou o processo for interrompido antes da limpeza,
não há garantia de limpeza automática do carrinho.

## Estratégia de testes

MockK e testes de coroutine verificam casos de uso, repositório, erros, cancelamento e
ViewModel; Robolectric/Compose valida confirmação, estados e ações. Testes locais de Room
verificam remoção por UID. Integração real de Firebase/Firestore, navegação e Google deve
ser exercitada em ambiente configurado; testes JVM não substituem esse teste.
