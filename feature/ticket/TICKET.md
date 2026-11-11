# Especificação do ingresso digital

## 1. Objetivo
Apresentar localmente o comprovante de uma compra e um QR Code individual para cada unidade de ingresso. Os dados permanecem disponíveis offline no dispositivo e são filtrados pela conta autenticada.

## 2. Compra e ingresso
A raiz de composição (`:app`) recebe a aprovação e o snapshot imutável do pagamento e os converte para o contrato do módulo de ingressos. Cada unidade comprada produz um ingresso, vinculado à referência da tentativa e ao ID do pedido. A compra e todos os ingressos são gravados em uma única transação Room, antes da limpeza do carrinho ou do reconhecimento da tentativa. Repetir a emissão da mesma compra não duplica ingressos; uma repetição com dados diferentes é rejeitada. O limite é de 100 unidades por compra, verificado antes do pagamento.

A única aprovação disponível atualmente é do emulador Cielo em debug. Seus ingressos e comprovantes são explicitamente **simulações, sem validade de entrada**. O callback não é autenticado; não há emissão ou validação de ingresso real em produção. Uma implementação futura deverá receber confirmação confiável e emitir IDs no servidor. A persistência é local e não sincroniza entre dispositivos; limpar dados ou desinstalar o app remove os ingressos locais.

## 3. QR Code
O conteúdo é exclusivamente o ID opaco UUID do ingresso, codificado localmente em QR Code com margem de segurança. Não contém dados pessoais, credenciais ou valores. O ID não é assinatura nem prova de autenticidade; o QR pode ser copiado.

## 4. Estados
Na interface: carregando, vazio, conteúdo, erro e geração de QR em andamento ou com falha. O ingresso emitido possui estado **simulado**, não validado nem utilizado. Os estados de uso, cancelamento e revogação dependem de fonte autoritativa futura e não são inferidos pelo app.

## 5. Critérios de aceite
- Uma aprovação de teste gera exatamente um ingresso para cada unidade, com IDs distintos e vínculo ao pedido e à conta.
- Pagamento pendente, negado, cancelado ou desconhecido não gera ingressos. A emissão repetida da mesma compra devolve os registros existentes; concorrência e reinício não criam duplicatas.
- A compra e os ingressos são gravados antes de limpar o carrinho. Falha de gravação é exibida e permite repetir apenas a emissão, sem nova cobrança nem perda da tentativa.
- Comprovante e detalhes mostram somente dados da conta atual. A tela de detalhes gera um QR legível contendo apenas o ID do ingresso.
- Migração preserva registros antigos, mas não os exibe como ingressos verificados, pois não possuem conta nem vínculo confiável com compra.
- O fluxo oferece estados de carregamento, vazio e erro, testes com MockK e verificação de geração e decodificação do QR.

## 6. Exibição do comprovante
O resumo mostra pedido, itens, quantidades, valor total e identificação de simulação, com acesso aos ingressos e seus detalhes. A lista permanece acessível após reinício e na tela de eventos. Nenhuma tela indica entrada autorizada ou pagamento real. O QR não é exibido se não puder ser gerado.

## 7. Validação futura
Um serviço autoritativo poderá emitir IDs associados à compra confirmada, validar propriedade e situação do ingresso, registrar uso de forma atômica e revogar ingressos. Assinaturas criptográficas e política de expiração podem reduzir falsificação offline, mas não eliminam reutilização sem sincronização. Nenhuma dessas garantias é implementada nesta versão.
