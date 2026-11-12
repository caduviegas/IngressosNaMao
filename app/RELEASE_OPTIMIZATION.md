# Especificação de otimização e proteção de release

## 1. Objetivo

Ativar R8 no aplicativo release para remover código e recursos não utilizados, otimizar e
obfuscar nomes, reduzindo exposição de detalhes de implementação e tamanho. Obfuscação não
impede engenharia reversa nem protege credenciais: segredos, autorização e reconciliação de
pagamentos devem permanecer fora do aplicativo e sob controle do servidor.

## 2. Bibliotecas e contratos relevantes

| Componente | Risco e tratamento |
|---|---|
| Firebase Auth, Firestore, Remote Config e Google Identity | Preservar regras de consumidor distribuídas pelos SDKs. O perfil é lido e gravado por nomes explícitos de campos, não por reflexão de POJO. O recurso `default_web_client_id` é consultado por nome e exige regra de recurso específica. |
| Room | Preservar regras de consumidor da biblioteca para a implementação gerada e migrações; não manter todas as entidades ou desativar otimização. Validar restauração e migrações em instalação release. |
| Retrofit / OkHttp | Usar regras de consumidor, inclusive anotações e assinaturas necessárias para interfaces dinâmicas. Não existem interfaces Retrofit declaradas atualmente; futuras interfaces e DTOs exigem validação. |
| Koin | O grafo é registrado em código; não há regra de consumidor específica do Koin na configuração R8 resolvida atualmente. Não preservar indiscriminadamente todas as classes dos módulos; validar a inicialização em runtime. |
| Compose | Usar regras de consumidor e código gerado pelo compilador; preservar recursos acessados dinamicamente individualmente. |
| kotlinx.serialization | Preservar regras de consumidor e serializadores gerados. O JSON da tentativa de pagamento precisa continuar compatível com dados já persistidos. |
| Android | O manifest e regras padrão do AGP preservam componentes necessários. A activity de callback e o serviço Cielo existem apenas no manifest debug e não devem ser ativados em release. |

As regras empacotadas pelas dependências e as regras padrão do AGP são cumulativas. O projeto
mantém apenas regras adicionais justificadas; proibir `-dontwarn` ou `-keep` abrangente como
solução genérica para erros sem investigação.

O arquivo `app/src/main/keepRules/rules.keep` preserva os atributos `SourceFile` e
`LineNumberTable` para permitir a interpretação de stack traces com o mapping. Em
`feature/auth/src/release/res/raw/com_innaval_auth_keep.xml`, `tools:keep` conserva apenas
`default_web_client_id` quando existir, porque o login Google o procura com `getIdentifier`.
Essa regra de recursos não cria um ID nem substitui a configuração do Firebase.

## 3. Riscos

Reflexão, nomes de classes ou atributos e consultas dinâmicas de recursos podem ser removidos
ou renomeados; isso pode produzir erro apenas em runtime. Mudanças na serialização ou nos
schemas locais podem impedir restauração. O mapeamento é necessário para interpretar stack
traces ofuscados. Uma compilação bem-sucedida não comprova o funcionamento em dispositivo.

## 4. Critérios de aceite

1. Release usa R8 para todo o aplicativo e dependências, com otimização de recursos e sem alterar
   o comportamento debug. São produzidos APK/AAB e arquivo `mapping.txt` para o artefato.
2. A configuração combina regras padrão do AGP, regras das bibliotecas e exceções explícitas
   do projeto. Compilação falha se houver erros de R8 ou regras inválidas.
3. Na validação do artefato release instalado, iniciar a aplicação e conferir autenticação
   (incluindo Google), Remote Config, navegação/Koin, restauração Room, telas Compose e acesso
   à rede quando configurada. Testar migrações e atualização de uma versão anterior.
4. O pagamento Cielo permanece desabilitado em release; callback e serviço de debug não aparecem
   no manifest release. Ingressos simulados não são promovidos a comprovantes reais.
5. O `default_web_client_id` gerado pelo plugin Google Services não é removido quando presente;
   sem configuração Firebase, o erro de configuração continua explícito.

## 5. Regras que não podem quebrar o app

Não desativar otimização ou obfuscação globalmente, não remover regras de consumidor e não
aplicar regras abrangentes para todo Firebase, Room, Retrofit, Koin, Compose ou código do app.
Não adicionar classes de callback de debug ao manifest release. Não remover o recurso de Google
obtido dinamicamente nem o suporte às regras existentes de Room/serialização.

## Recomendações para publicação

- Manter `mapping.txt`, arquivo de símbolos nativos quando aplicável e artefato assinado
  associados a cada versão em local seguro; usar R8 retrace ou publicar o mapping no serviço
  de relatórios de crashes configurado, sem expor dados sensíveis.
- Assinar o AAB com chave de upload mantida fora do repositório, verificar a configuração
  Firebase/OAuth de release e usar um canal interno antes da distribuição.
- Instalar e testar o **mesmo artefato otimizado** em dispositivo/emulador, inclusive upgrade
  de uma instalação anterior e os fluxos com dependências reais de teste. Cobertura JVM debug
  não valida o APK release.
- `./gradlew :qualityCheck` também compila o APK release otimizado, mas não testa esse artefato
  em execução nem substitui o teste de integração com serviços configurados.
- Pagamento atual é apenas simulação no emulador e não é adequado para checkout mobile real;
  consultar [PAYMENT.md](../feature/payment/PAYMENT.md) antes de qualquer publicação.

Fontes: [otimização AGP 9.3+](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[regras de bibliotecas](https://developer.android.com/topic/performance/app-optimization/choose-libraries-wisely)
e [recursos dinâmicos](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep).
