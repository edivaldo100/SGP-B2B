# Sistema de Gestão de Pedidos B2B
Você foi designado para desenvolver um microserviço crítico para um sistema B2B de gestão de pedidos. Este microserviço será responsável por receber, processar e gerenciar pedidos de parceiros comerciais, com potencial para milhares de requisições simultâneas.
### Prerequisitos

* Java 17
* Mavem
* PostgreSQL
* Docker
* Docker-compose

### Instalação

1. Clone o repo
```sh
git clone https://github.com/edivaldo100/SGP-B2B
```
2. Start com docker-compose
```sh
	docker-compose up
```

### Detalhes

Eurika-server http://localhost:8761/

swagger : http://localhost:8080/restapi/swagger-ui/index.html#/

isAlive : http://localhost:8080/restapi/isAlive
```sh
curl -X 'GET' \
  'http://localhost:8080/restapi/isAlive' \
  -H 'accept: */*'
```
Criar Parceiro:
```sh 
curl -X 'GET' \
  'http://localhost:8080/restapi/api/partners' \
  -H 'accept: */*'
```
Consulta Parceiro
```sh
curl -X 'GET' \
  'http://localhost:8080/restapi/api/orders' \
  -H 'accept: */*'
   ```
   
Criar um pedido
```sh
    curl -X 'POST' \
  'http://localhost:8080/restapi/api/orders' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "partnerId": 1,
  "items": [
    {
      "product": "banana",
      "quantity": 10,
      "unitPrice": 10
    }
  ]
}'
   ```

Consulta pedidos
```sh
curl -X 'GET' \
  'http://localhost:8080/restapi/api/orders' \
  -H 'accept: */*'
   ```
### Arquitetura proposta   
![](desenho.png)