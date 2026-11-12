# Processo de qualidade

## Objetivo

A análise estática identifica problemas de manutenção, possíveis erros e tratamento inadequado
de exceções antes da integração. A cobertura indica quanto do código de produção foi exercitado
pelos testes; não é uma medida de correção nem substitui revisão, acessibilidade ou testes de
integração em dispositivos e serviços reais.

## Regras

- Detekt analisa os arquivos Kotlin de produção, testes JVM e instrumentados em `app`, `core`
  e features, sem código gerado ou saídas de build. Usam-se as regras padrão, incluindo
  complexidade, nomenclatura, blocos vazios, exceções e problemas de código; funções `@Composable`
  seguem a convenção Compose de nomes iniciados por maiúscula. Regras que requerem
  resolução de tipos não são garantidas pela análise sintática.
- As violações preexistentes ficam registradas no baseline versionado. Novas violações ativas
  não incluídas nele quebram a verificação. O baseline não é regenerado automaticamente na CI:
  cada entrada deve ser removida quando o problema for corrigido, e alterações no baseline
  precisam de revisão. Uma assinatura já registrada pode ocultar recorrências equivalentes.
- JaCoCo mede linhas do código de produção debug agregadas entre todos os módulos, inclusive
  domínio, dados, apresentação, navegação e DI. Apenas classes geradas são excluídas; veja
  [TESTING.md](TESTING.md) para escopo, comandos e limitações.
- Dependências, SDK e ferramentas têm versões explícitas. A CI não requer credenciais Firebase,
  Cielo ou outros segredos e não executa integrações reais.

## Critérios de aceite e falhas

Em push e pull request, a pipeline deve instalar o SDK Android necessário, executar Detekt,
testes JVM/Robolectric, relatório e verificação JaCoCo, além de compilar o aplicativo debug e
o release otimizado com R8.
O resultado deve falhar para erros de configuração ou compilação, testes com falha, novas
violações Detekt, dados de cobertura ausentes ou **cobertura agregada de linhas inferior a 80%**.
Relatórios de testes, Detekt e JaCoCo são disponibilizados como artefatos mesmo em caso de falha,
quando existirem. A verificação não deve ignorar falhas nem diminuir o denominador para passar.

Testes instrumentados em dispositivo/emulador, acessibilidade manual, Firebase real e o emulador
oficial Cielo não fazem parte deste workflow; continuam sendo verificações complementares.

## Uso e manutenção

- `./gradlew :detekt`: análise e relatórios em `build/reports/detekt/`.
- `./gradlew :qualityCheck`: análise, testes, relatório, limite de cobertura e compilações debug e release.
- `./gradlew :jacocoDebugReport :jacocoDebugCoverageVerification`: relatório e limite de 80%.
- Para isolar as saídas de build, como na CI: `./gradlew --init-script config/ci-build.init.gradle :qualityCheck`.
  Os relatórios ficam em `build/quality/root/reports/`; isso evita reutilizar artefatos antigos
  presentes nos diretórios `build` dos módulos.
- Em caso de violação, corrigir a causa. O baseline inicial documenta dívida existente; não deve
  ser atualizado para aceitar automaticamente problemas novos.
