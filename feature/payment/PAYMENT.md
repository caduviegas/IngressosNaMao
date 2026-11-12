# Pagamento com Cielo Smart — testes

## 1. Objetivo
Simular a confirmação de uma compra no emulador oficial, sem dados reais de cartão e sem habilitar pagamentos de produção.

## 2. Integração
O deep link oficial `lio://payment` executa o checkout no aplicativo Cielo instalado no mesmo Android. Esta implementação aponta ao pacote `br.com.cielosmart.orderservice`, identificado no APK oficial do emulador 1.61.8, e não ao aplicativo do terminal. A Cielo Smart é voltada ao ambiente do lojista; isso não é um checkout de produção para o celular comum do comprador. Para vendas remotas, avaliar Cielo E-commerce e uma integração segura no servidor. O emulador requer credenciais próprias e Android 7.1 ou 10; não instalar em terminal Smart.

## 3. Entrada e saída
Entrada: usuário, fingerprint e snapshot do carrinho, itens com SKU, nome, quantidade e preço unitário em centavos, total positivo, client ID e access token de teste. Os valores são conferidos antes do envio. Configure `CIELO_CLIENT_ID` e `CIELO_ACCESS_TOKEN` em variáveis de ambiente ou copie o [modelo](../../cielo.local.properties.example) para o arquivo local ignorado `cielo.local.properties` na raiz e preencha suas credenciais; as variáveis de ambiente têm precedência. Os valores são incorporados ao APK de debug e podem ser extraídos dele: use somente credenciais de teste e nunca compartilhe esse APK como se não contivesse segredos. A versão release não recebe as credenciais e mantém o pagamento desabilitado. Se ausentes, um erro é exibido.

Após finalizar a compra, o app revalida o carrinho e inicia a Cielo automaticamente uma única vez. Erros de validação exigem nova tentativa explícita; tentativas existentes são exibidas para recuperação, sem reenvio automático. A saída é uma simulação aprovada, cancelada, negada quando o gateway a informar expressamente, ou resultado desconhecido/erro. O emulador documenta sucesso, cancelamento e erro genérico, não uma negativa bancária específica.

## 4. Estados
Preparação, envio, aguardando retorno, aprovação simulada, cancelamento, negativa, erro e resultado desconhecido. Timeout, retorno incompatível, erro genérico e interrupção durante o envio não comprovam que não houve cobrança. Retornos tardios são processados mesmo após reinício. O callback não é autenticado e não é prova para produção.

## 5. Idempotência
Uma referência UUID é gerada e a tentativa registrada de forma durável antes de abrir o emulador. O registro impede novos envios até que a simulação seja concluída ou descartada manualmente. `reference` é apenas correlação: a Cielo não documenta deduplicação por essa chave. O descarte manual elimina a proteção local contra duplicidade; esta implementação não é adequada a pagamentos reais.

## 6. Critérios de aceite
O app monta o contrato oficial com todos os itens, não envia valor inválido, não expõe credenciais em logs e persiste antes de enviar. Para a aprovação simulada, a referência e o identificador devem corresponder, o valor pendente deve ser zero e a soma das transações de pagamento deve ser igual ao total. O emulador observado também retorna `paidAmount = 0` em um sucesso; esse valor é aceito apenas neste fluxo de debug, além do total correto. Somente no ambiente de debug a aprovação simulada pode acionar a limpeza do carrinho, protegida pelo fingerprint. O fluxo de release permanece desabilitado. Erros, cancelamento, ausência do app, timeout e dados inesperados não apagam o carrinho. As abstrações permitem testes com MockK.

## 7. Erros e cancelamentos
Ausência de credenciais, app Cielo indisponível e falha de persistência são exibidos explicitamente. Códigos documentados: 1 cancelamento, 2 erro genérico, 3 erro de pagamento e 4 autenticação. Código 3 não é tratado como negativa bancária. O código numérico é armazenado para diagnóstico, mas o texto `reason` não é persistido nem exibido, pois pode conter dados sensíveis. Tentativas anteriores podem não ter código registrado. Sem correlação confiável, erros e cancelamentos permanecem bloqueados até uma ação explícita do usuário. Retornos inválidos ou conflitantes nunca são tratados como sucesso.

## 8. Timeout e falhas
Ao expirar a espera, ou se o processo for encerrado após persistir a tentativa, o estado é desconhecido; não há reenvio automático. A documentação recomenda serviço em primeiro plano para reduzir perda do retorno, mas isso não substitui reconciliação. A consulta oficial de pedido requer identificador do pedido ou dados de transação; sem esses dados não há consulta por referência documentada. Não se garante recuperação automática caso o callback seja perdido.

Em debug, qualquer tentativa ou registro inválido pode ser descartado manualmente após confirmação, inclusive quando pendente, desconhecido ou aprovado, ou quando o usuário ou carrinho mudou. A ação está disponível nas telas de eventos e pagamento, verifica se o estado não mudou desde a confirmação e não está disponível em release. Descartar remove a proteção local contra duplicidade, não cancela nem concilia uma transação e pode permitir a chegada de respostas atrasadas. Não use essa recuperação para pagamentos reais. Após uma aprovação simulada, a tela de eventos também permite retomar a limpeza do carrinho caso o app seja interrompido antes da conclusão.

O aplicativo limita a compra a 100 ingressos antes de iniciar o pagamento. Após a aprovação simulada, registra os ingressos localmente antes de limpar o carrinho e reconhecer a tentativa. A interface informa que esses ingressos não têm validade de entrada e que o resumo não comprova pagamento.

Fontes: [pagamento](https://docs.cielo.com.br/cielo-smart/docs/pagamento), [retorno](https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados), [erros](https://docs.cielo.com.br/cielo-smart/docs/codigos-de-erro), [consulta](https://docs.cielo.com.br/cielo-smart/docs/consulta-de-pedido-avulso), [emulador](https://docs.cielo.com.br/cielo-smart/docs/realizando-pagamentos), [FAQ](https://docs.cielo.com.br/cielo-smart/docs/integracao-via-deep-link).
