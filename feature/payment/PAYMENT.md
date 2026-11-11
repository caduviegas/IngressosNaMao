# Pagamento com Cielo Smart — testes

## 1. Objetivo
Simular a confirmação de uma compra no emulador oficial, sem dados reais de cartão e sem habilitar pagamentos de produção.

## 2. Integração
O deep link oficial `lio://payment` executa o checkout no aplicativo Cielo instalado no mesmo Android. Esta implementação aponta ao pacote `br.com.cielosmart.orderservice`, identificado no APK oficial do emulador 1.61.8, e não ao aplicativo do terminal. A Cielo Smart é voltada ao ambiente do lojista; isso não é um checkout de produção para o celular comum do comprador. Para vendas remotas, avaliar Cielo E-commerce e uma integração segura no servidor. O emulador requer credenciais próprias e Android 7.1 ou 10; não instalar em terminal Smart.

## 3. Entrada e saída
Entrada: usuário, fingerprint e snapshot do carrinho, itens com SKU, nome, quantidade e preço unitário em centavos, total positivo, client ID e access token de teste. Os valores são conferidos antes do envio. As credenciais são informadas na tela de debug, não persistidas nem versionadas. A saída é uma simulação aprovada, cancelada, negada quando o gateway a informar expressamente, ou resultado desconhecido/erro. O emulador documenta sucesso, cancelamento e erro genérico, não uma negativa bancária específica.

## 4. Estados
Preparação, envio, aguardando retorno, aprovação simulada, cancelamento, negativa, erro e resultado desconhecido. Timeout, retorno incompatível, erro genérico e interrupção durante o envio não comprovam que não houve cobrança. Retornos tardios são processados mesmo após reinício. O callback não é autenticado e não é prova para produção.

## 5. Idempotência
Uma referência UUID é gerada e a tentativa registrada de forma durável antes de abrir o emulador. O registro bloqueia novos envios no dispositivo enquanto pendente ou desconhecido, inclusive após reinício, troca de usuário ou mudança do carrinho. `reference` é apenas correlação: a Cielo não documenta deduplicação por essa chave. O bloqueio local não protege outros dispositivos, reinstalação ou exclusão de dados. Em caso de dúvida não reenviar automaticamente; uma conciliação confiável é necessária. Cancelamento/erros sem referência também não são correlacionáveis com segurança e não liberam automaticamente outra cobrança.

## 6. Critérios de aceite
O app monta o contrato oficial com todos os itens, não envia valor inválido, não expõe credenciais em logs, persiste antes de enviar e não envia novamente enquanto bloqueado. Aprovação simulada deve corresponder à referência e total, com transação e valor pendente zero. Somente no ambiente de debug a aprovação simulada pode acionar a limpeza do carrinho, protegida pelo fingerprint. O fluxo de release permanece desabilitado. Erros, cancelamento, ausência do app, timeout e dados inesperados não apagam o carrinho. As abstrações permitem testes com MockK.

## 7. Erros e cancelamentos
Ausência de credenciais, app Cielo indisponível e falha de persistência são exibidos explicitamente. Códigos documentados: 1 cancelamento, 2 erro genérico, 3 erro de pagamento e 4 autenticação. Código 3 não é tratado como negativa bancária. Sem correlação confiável, o cancelamento é apresentado, mas a tentativa permanece bloqueada. Retornos inválidos ou conflitantes nunca são tratados como sucesso.

## 8. Timeout e falhas
Ao expirar a espera, ou se o processo for encerrado após persistir a tentativa, o estado é desconhecido; não há reenvio automático. A documentação recomenda serviço em primeiro plano para reduzir perda do retorno, mas isso não substitui reconciliação. A consulta oficial de pedido requer identificador do pedido ou dados de transação; sem esses dados não há consulta por referência documentada. Não se garante recuperação automática caso o callback seja perdido.

Esta versão não oferece desbloqueio manual. Um cancelamento, erro ou retorno perdido pode impedir novos testes até a tentativa ser conciliada; apagar dados ou reinstalar remove a proteção e não é seguro para pagamentos reais. Após uma aprovação simulada, a tela de eventos permite retomar a limpeza do carrinho caso o app seja interrompido antes da conclusão.

O aplicativo limita a compra a 100 ingressos antes de iniciar o pagamento. Após a aprovação simulada, registra os ingressos localmente antes de limpar o carrinho e reconhecer a tentativa. Esses ingressos são identificados como testes e não têm validade real.

Fontes: [pagamento](https://docs.cielo.com.br/cielo-smart/docs/pagamento), [retorno](https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados), [erros](https://docs.cielo.com.br/cielo-smart/docs/codigos-de-erro), [consulta](https://docs.cielo.com.br/cielo-smart/docs/consulta-de-pedido-avulso), [emulador](https://docs.cielo.com.br/cielo-smart/docs/realizando-pagamentos), [FAQ](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link).
