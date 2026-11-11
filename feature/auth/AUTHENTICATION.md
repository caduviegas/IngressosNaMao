# Especificação do fluxo de autenticação

## 1. Objetivo

Autenticar usuários com Firebase Authentication, por e-mail e senha ou Google, e
manter o perfil de cadastro privado no Cloud Firestore. O módulo de autenticação
não depende de outras features; navegação entre features pertence ao aplicativo.

## 2. Atores

Usuário, aplicativo Android, Firebase Authentication, Google Identity e Cloud
Firestore. O operador do Firebase configura provedores, regras e credenciais.

## 3. Pré-condições e pós-condições

- Projeto Firebase configurado no aplicativo; provedores de e-mail/senha e
  Google habilitados; cliente OAuth Android e cliente Web configurados.
- Firestore disponível e regras de `firestore.rules` publicadas. As operações
  remotas exigem conectividade.
- Ao concluir o fluxo há uma sessão Firebase válida e um perfil completo em
  `users/{uid}`. Em caso de erro não se navega para a área autenticada.
- Se a conta for criada mas a gravação do perfil falhar, ela permanece no
  Firebase. Um login posterior permite completar o perfil, sem criar outra conta.

## 4. Estados

Verificando sessão, desautenticado, operação em andamento, perfil incompleto,
autenticado e erro recuperável. Campos e erros de validação compõem o estado da
tela; falhas não são representadas como sucesso.

## 5. Eventos

Inicialização e restauração de sessão, edição de campos, envio de login ou cadastro,
seleção/cancelamento de conta Google, conclusão do perfil, repetição após erro e
saída da conta. Navegação para fora do módulo ocorre apenas após autenticação e
confirmação do perfil.

## 6. Regras de negócio

- E-mail é normalizado removendo espaços nas extremidades; senha não é alterada.
- Cadastro cria conta por e-mail e persiste nome, e-mail e endereço associados
  ao UID. Nunca armazenar senha no Firestore ou em logs.
- Google autentica com token de identidade obtido pelo Credential Manager. Na
  primeira entrada, caso não exista perfil, solicitar nome e endereço antes de
  concluir. Contas já existentes não são vinculadas automaticamente.
- Uma sessão existente é verificada ao abrir o aplicativo. A leitura do perfil
  consulta o servidor; indisponibilidade não deve ser confundida com ausência.
- O Firestore autoriza leitura e escrita apenas ao próprio UID. Campos do
  documento têm tipos e tamanho validados também nas regras.
- Apenas uma operação de autenticação pode estar ativa por vez. Erros podem ser
  corrigidos e a operação repetida; cancelamento de coroutine é propagado.
- reCAPTCHA não faz parte desta entrega por decisão do solicitante e é melhoria
  futura; não há proteção adicional contra automação implementada pelo app.

## 7. Validações de entrada

Nome e endereço obrigatórios, após remoção de espaços, com no máximo 100 e 300
caracteres respectivamente. E-mail obrigatório, com formato válido e no máximo
254 caracteres. Senha de cadastro contém pelo menos 6 caracteres, conforme
requisito mínimo do Firebase, e no máximo 128. No login, senha é obrigatória.
Regras do provedor podem exigir uma política de senha mais forte.

## 8. Cenários de erro

Campos inválidos; credenciais incorretas; e-mail já cadastrado; senha rejeitada;
conta desabilitada; limite de tentativas; rede indisponível; cancelamento ou
falha do seletor Google; configuração Firebase/Google ausente; falha de leitura
ou gravação do perfil; e erro inesperado. Cada cenário apresenta mensagem
explícita sem revelar senha. Uma falha após criação da conta informa que o
usuário deve entrar novamente para completar o cadastro.

## 9. Critérios de aceite

1. Login válido por e-mail e por Google acessa a área autenticada somente com
   perfil confirmado; credenciais inválidas e cancelamento exibem erro.
2. Cadastro válido cria conta e perfil; dados inválidos não chamam o provedor.
3. Falha de gravação não produz sucesso e o perfil pode ser completado após
   novo login; Google sem perfil também solicita dados obrigatórios.
4. Sessão existente é restaurada e a ausência ou erro do perfil é diferenciada.
5. Operações em andamento bloqueiam envios duplicados e erros permitem nova
   tentativa. Senhas são ocultadas e não persistidas.
6. Regras do Firestore impedem acesso a documentos de outros usuários.
7. Testes unitários com MockK cobrem validação, sucesso, erros, recuperação de
   perfil, concorrência e propagação de cancelamento. Integrações Firebase,
   regras e Google devem adicionalmente ser verificadas com emulador/dispositivo
   e credenciais de projeto configuradas.

## Configuração e testes

1. Adicionar `app/google-services.json` do projeto Firebase Android
   (`com.innaval.ingresosnamao`); sem ele o app compila, mas informa erro de
   configuração ao iniciar a autenticação. O plugin Google Services é aplicado
   quando o arquivo existe.
2. Ativar os provedores de e-mail/senha e Google no Firebase Authentication.
   Registrar os certificados SHA-1/SHA-256 do aplicativo, configurar o cliente
   OAuth e baixar novamente o JSON para gerar `default_web_client_id`.
3. Criar o Firestore e publicar `firestore.rules` no projeto correto antes de
   disponibilizar o app. As regras propostas restringem a coleção `users`;
   integre-as às demais regras existentes, se houver.
4. Executar `./gradlew :feature:auth:testDebugUnitTest`. Validar adicionalmente
   com emuladores Firebase, incluindo tentativa de acessar outro UID e falhas
   de rede, e com dispositivo real para o seletor de conta Google.

Cadastro e escrita do perfil não formam uma transação entre serviços. Se a
gravação falhar ou for interrompida, a conta criada permanece; o formulário
permite repetir a gravação ou completar o perfil após uma nova entrada.
