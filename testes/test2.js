// test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics'; // Para contar sucesso/falha de operações específicas

// Contador para operações de criação
const postSuccess = new Counter('post_partner_success');
const postFailed = new Counter('post_partner_failed');
const getSuccess = new Counter('get_partner_success');
const getFailed = new Counter('get_partner_failed');

export const options = {
  // Cenários para simular carga
  scenarios: {
    stress_test: {
      executor: 'constant-vus', // Simula um número constante de usuários virtuais
      vus: 50, // Número de usuários virtuais (simultâneos)
      duration: '1m', // Duração do teste (1 minuto)
      // ramp-up gradual para evitar sobrecarga inicial
      // executor: 'ramping-vus',
      // startVUs: 0,
      // stages: [
      //   { duration: '30s', target: 50 }, // Aumenta para 50 VUs em 30s
      //   { duration: '30s', target: 50 }, // Mantém 50 VUs por mais 30s
      // ],
      // gracefulStop: '30s', // Tempo para VUs terminarem requisições pendentes
    },
  },
  // Configuração de limites para falhas e tempos de resposta
  thresholds: {
    // Para todas as requisições HTTP:
    http_req_failed: ['rate<0.01'], // Taxa de falhas HTTP deve ser menor que 1%

    // Para requisições POST específicas:
    'http_req_duration{method:POST}': ['p(95)<500'], // 95% das requisições POST devem ser mais rápidas que 500ms
    'post_partner_success': ['count>=1'], // Pelo menos 1 POST bem-sucedido (para garantir que a operação aconteça)
    'post_partner_failed': ['count==0'],  // Nenhuma falha lógica em POST (se aplicável, refine com base no seu serviço)

    // Para requisições GET específicas:
    'http_req_duration{method:GET}': ['p(95)<1000'], // 95% das requisições GET devem ser mais rápidas que 1000ms
    'get_partner_success': ['count>=1'],  // Pelo menos 1 GET bem-sucedido
    'get_partner_failed': ['count==0'],   // Nenhuma falha lógica em GET
  },
};

export default function () {
  // Geração de dados únicos para cada requisição POST
  const uniqueId = `${__VU}${__ITER}${Date.now()}`; // ID único baseado em VU, Iteração e Timestamp
  const partnerName = `Partner_${uniqueId}`;

  const postBody = {
    id: parseInt(uniqueId.substring(0, 8)), // Usar parte do uniqueId como ID numérico
    name: partnerName,
    creditLimit: 1000000,
    currentCredit: 100000,
  };

  const headers = {
    'accept': '*/*',
    'Content-Type': 'application/json',
  };

  // --- Operação de Criação (POST) ---
  const postRes = http.post('http://host.docker.internal/restapi/api/partners', JSON.stringify(postBody), { headers: headers, tags: { method: 'POST' } });

  // Verifica se o POST foi bem-sucedido (status 200 OK ou 201 Created)
  const postCheck = check(postRes, {
    'POST status is 200/201': (r) => r.status === 200 || r.status === 201,
  });

  if (postCheck) {
    postSuccess.add(1);
  } else {
    postFailed.add(1);
    console.error(`POST Failed: Status ${postRes.status}, Body: ${postRes.body}`);
  }

  // --- Pequena pausa para simular tempo de "pensamento" do usuário ---
  sleep(1);

  // --- Operação de Consulta (GET) ---
  const getRes = http.get('http://host.docker.internal/restapi/api/partners', { headers: headers, tags: { method: 'GET' } });

  // Verifica se o GET foi bem-sucedido (status 200 OK)
  const getCheck = check(getRes, {
    'GET status is 200': (r) => r.status === 200,
  });

  if (getCheck) {
    getSuccess.add(1);
  } else {
    getFailed.add(1);
    console.error(`GET Failed: Status ${getRes.status}, Body: ${getRes.body}`);
  }

  // --- Pequena pausa antes da próxima iteração ---
  sleep(1);
}