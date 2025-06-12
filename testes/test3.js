// test3.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution'; // <--- ADICIONE ESTA LINHA
import { SharedArray } from 'k6/data';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ... resto do script

// --- Métricas Customizadas ---
const approveOrderSuccess = new Counter('approve_order_success');
const approveOrderFailed = new Counter('approve_order_failed');
const creditInsufficientErrors = new Counter('credit_insufficient_errors');
const approveTxDuration = new Trend('approve_order_tx_duration');

// --- Configuração do Teste ---
export const options = {
  scenarios: {
    stress_approval_concurrency: {
      executor: 'constant-vus',
      vus: 500,
      duration: '1m',
      exec: 'approveOrderScenario',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'approve_order_tx_duration': ['p(95)<2000'],
    'credit_insufficient_errors': ['count==0'],
  },
};

// =================================================================================
// SETUP: Preparação do ambiente antes da execução dos VUs
// =================================================================================
export function setup() {
// Adiciona uma pausa de 5 segundos como salvaguarda
  sleep(5);

  console.log('--- SETUP: Iniciando preparação do teste de concorrência ---');
  // Usamos o nome do serviço do docker-compose. O Gateway expõe na porta 80.
  const BASE_URL = 'http://host.docker.internal/restapi/api';
  const headers = { 'Content-Type': 'application/json' };

  // --- 1. Criar um ÚNICO parceiro para concentrar a contenção ---
  const initialCredit = 1000000.00;
  const partnerPayload = {
    name: `Partner_Contention_${Date.now()}`,
    creditLimit: initialCredit,
    currentCredit: initialCredit,
  };

  const partnerRes = http.post(`${BASE_URL}/partners`, JSON.stringify(partnerPayload), { headers });
  if (partnerRes.status !== 201) {
    throw new Error(`SETUP FAILED: Não foi possível criar o parceiro de teste. Status: ${partnerRes.status}, Body: ${partnerRes.body}`);
  }
  const partner = partnerRes.json();
  const partnerId = partner.id;
  console.log(`✅ SETUP: Parceiro de teste criado com sucesso! ID: ${partnerId}, Crédito Inicial: ${initialCredit}`);

  // --- 2. Criar múltiplos pedidos PENDENTES para este parceiro ---
  const numOrdersToCreate = 500;
  const orderValue = 10.00;
  let createdOrders = [];

  console.log(`... SETUP: Criando ${numOrdersToCreate} pedidos de R$${orderValue} cada...`);
  for (let i = 0; i < numOrdersToCreate; i++) {
    const orderPayload = {
      partnerId: partnerId,
      items: [
        {
          product: 'Produto Concorrência',
          quantity: 1,
          unitPrice: orderValue,
        },
      ],
    };

    const orderRes = http.post(`${BASE_URL}/orders`, JSON.stringify(orderPayload), { headers });

    if (orderRes.status === 201) {
      createdOrders.push(orderRes.json());
    } else {
      console.warn(`SETUP WARN: Falha ao criar pedido ${i + 1}. Status: ${orderRes.status}, Body: ${orderRes.body}`);
    }
  }

  if (createdOrders.length === 0) {
    throw new Error('SETUP FAILED: Nenhum pedido foi criado com sucesso. Abortando teste.');
  }

  console.log(`✅ SETUP: ${createdOrders.length} pedidos criados com sucesso.`);
  console.log('--- SETUP: Concluído ---');

  // Retorna os dados que serão usados pelos VUs
  return JSON.stringify({
    partner: partner,
    orders: createdOrders,
    orderValue: orderValue
  });
}

// =================================================================================
// LÓGICA DO CENÁRIO: O que cada VU vai fazer
// =================================================================================
//export function approveOrderScenario(dataStr) {
//    const data = JSON.parse(dataStr);
//    const BASE_URL = 'http://host.docker.internal/restapi/api';
//
//    // Pega um ID de pedido de forma sequencial para garantir que cada VU pegue um diferente
//    const iteration = __VU * options.scenarios.stress_approval_concurrency.duration.replace('m', '') * 60 + __ITER;
//
//    if (iteration >= data.orders.length) {
//        return;
//    }
//
//    const order = data.orders[iteration];
//    if (!order) {
//        return;
//    }
//    const orderId = order.id;
//
//  // --- Ação Principal: Tentar aprovar o pedido ---
//  const approveUrl = `${BASE_URL}/orders/${orderId}/status?newStatus=APROVADO`;
//  const res = http.patch(approveUrl, null, { tags: { name: 'ApproveOrder' } });
//
//  // =======================================================================
//  //  INÍCIO DA ALTERAÇÃO - PASSO 3
//  // =======================================================================
//  // Adiciona a duração da requisição à nossa métrica de tendência
//  // Apenas se a requisição não falhou a nível de rede (tendo o objeto timings)
//  if (res && res.timings) {
//    approveTxDuration.add(res.timings.duration);
//  }
//  // =======================================================================
//  //  FIM DA ALTERAÇÃO - PASSO 3
//  // =======================================================================
//
//  // --- Verificações (Checks) ---
//  const success = check(res, {
//    'Status da aprovação é 200 OK': (r) => r.status === 200,
//  });
//
//  if (success) {
//    approveOrderSuccess.add(1);
//  } else {
//    approveOrderFailed.add(1);
//    // Loga o erro apenas se a resposta tiver um corpo, para evitar poluir o log com erros de rede
//    if (res && res.body) {
//        console.error(`ERRO ao aprovar pedido ${orderId}: Status ${res.status}, Body: ${res.body}`);
//        if (res.body.includes('crédito suficiente')) {
//            creditInsufficientErrors.add(1);
//        }
//    }
//  }
//
//  sleep(1);
//}
// LÓGICA DO CENÁRIO: O que cada VU vai fazer
export function approveOrderScenario(dataStr) {
    const data = JSON.parse(dataStr);
    const BASE_URL = 'http://host.docker.internal/restapi/api';

    // Pega um pedido único do array de dados usando o contador de iteração do k6.
    // Esta é a forma mais robusta de garantir que cada iteração processe um pedido diferente.
    const order = data.orders[exec.scenario.iterationInTest];

    // Se a iteração for maior que o número de pedidos, ou se o pedido não existir, encerra.
    if (!order) {
        return;
    }
    const orderId = order.id;

    // --- Ação Principal: Tentar aprovar o pedido ---
    const approveUrl = `${BASE_URL}/orders/${orderId}/status?newStatus=APROVADO`;
    const res = http.patch(approveUrl, null, { tags: { name: 'ApproveOrder' } });

    // Adiciona a duração da requisição à nossa métrica de tendência
    if (res && res.timings) {
        approveTxDuration.add(res.timings.duration);
    }

    // --- Verificações (Checks) ---
    const success = check(res, {
        'Status da aprovação é 200 OK': (r) => r.status === 200,
    });

    if (success) {
        approveOrderSuccess.add(1);
    } else {
        approveOrderFailed.add(1);
        if (res && res.body) {
            console.error(`ERRO ao aprovar pedido ${orderId}: Status ${res.status}, Body: ${res.body}`);
            if (res.body.includes('crédito suficiente')) {
                creditInsufficientErrors.add(1);
            }
        }
    }

    sleep(1);
}
// =================================================================================
// TEARDOWN: Verificação final após o fim do teste
// =================================================================================
export function teardown(dataStr) {
    const data = JSON.parse(dataStr);
    const BASE_URL = 'http://gateway/restapi/api';
    console.log('--- TEARDOWN: Verificando estado final do parceiro ---');

    // Passo 1: Buscar quantos pedidos foram REALMENTE aprovados consultando a API
    const approvedOrdersRes = http.get(`${BASE_URL}/orders?partnerId=${data.partner.id}&status=APROVADO`);
    if (approvedOrdersRes.status !== 200) {
        console.error(`TEARDOWN FAILED: Não foi possível buscar os pedidos aprovados. Status: ${approvedOrdersRes.status}`);
        return;
    }

    // O número de aprovações é o tamanho do array retornado
    const successfulApprovals = approvedOrdersRes.json().length;

    // Passo 2: Buscar o estado final do parceiro
    const partnerRes = http.get(`${BASE_URL}/partners/${data.partner.id}`);
    if (partnerRes.status !== 200) {
        console.error(`TEARDOWN FAILED: Não foi possível buscar o parceiro final. Status: ${partnerRes.status}`);
        return;
    }

    // Passo 3: Realizar a validação matemática com os dados corretos
    const finalPartner = partnerRes.json();
    const initialCredit = parseFloat(data.partner.currentCredit);
    const finalCredit = parseFloat(finalPartner.currentCredit);
    const expectedCreditSpent = successfulApprovals * data.orderValue;
    const expectedFinalCredit = initialCredit - expectedCreditSpent;

    console.log(`Crédito Inicial: ${initialCredit.toFixed(2)}`);
    console.log(`Pedidos aprovados com sucesso (via API): ${successfulApprovals}`);
    console.log(`Valor de cada pedido: ${data.orderValue.toFixed(2)}`);
    console.log(`Gasto esperado (crédito debitado): ${expectedCreditSpent.toFixed(2)}`);
    console.log(`Crédito final ESPERADO: ${expectedFinalCredit.toFixed(2)}`);
    console.log(`Crédito final REAL: ${finalCredit.toFixed(2)}`);

    // A validação agora usará os números corretos
    if (Math.abs(expectedFinalCredit - finalCredit) < 0.01) {
        console.log('✅ SUCESSO! O crédito final do parceiro está correto. O bloqueio pessimista funcionou!');
    } else {
        console.error('❌ FALHA! O crédito final do parceiro está INCORRETO. Houve uma race condition!');
    }
}

// Função para gerar um resumo mais legível no final
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}