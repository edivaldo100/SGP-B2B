// test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Configuração para simular carga
  scenarios: {
    stress_test: {
      executor: 'constant-vus', // Simula um número constante de usuários virtuais
      vus: 100, // Número de usuários virtuais (simultâneos)
      duration: '1m', // Duração do teste (1 minuto)
      // Alternativa: 'ramping-vus' para aumentar a carga gradualmente
    },
  },
  // Configuração de limites para falhas (opcional, mas recomendado)
  thresholds: {
    http_req_failed: ['rate<0.01'], // Erros HTTP devem ser menos de 1%
    http_req_duration: ['p(95)<100'], // 95% das requisições devem ser mais rápidas que 100ms
  },
};

export default function () {
  // A URL do seu gateway. Usamos 'host.docker.internal' para acessar o host do Docker Desktop
  // ou o IP do gateway dentro da rede Docker se o k6 estiver na mesma rede (veja abaixo).
  // Se o k6 estiver na mesma rede Docker do docker-compose, use 'http://gateway/restapi/isAlive'
  // Senão, use 'http://localhost/restapi/isAlive' se estiver na porta 80 do host
  const res = http.get('http://host.docker.internal/restapi/isAlive');

  // Verifica se a resposta foi 200 OK
  check(res, { 'status is 200': (r) => r.status === 200 });

  // Pequena pausa entre as requisições (para simular comportamento mais real)
  sleep(2);
}