# Produção Service – FastFood (Fase 4)

Microsserviço responsável pelo **acompanhamento da produção** (visão da cozinha).

## Informações Gerais
- **Porta:** 8083
- **Banco:** MongoDB (NoSQL)
- **Framework:** Spring Boot 3
- **Linguagem:** Java 21

## Responsabilidades
- Controlar fila de produção
- Atualizar status: RECEBIDO, EM_PREPARACAO, PRONTO
- Persistir histórico no MongoDB
- Atualizar pedido-service via REST

## Swagger
- http://localhost:8083/swagger
- http://localhost:8083/swagger-ui/index.html

## Execução
```bash
mvn clean spring-boot:run
```

## Testes e BDD
Possui testes unitários e cenários BDD conforme exigência da Fase 4.

```bash
mvn test
```

## Cobertura de Testes (Evidência)
> Inserir aqui print ou link do relatório de cobertura (mínimo 80%)

---

Grupo: Felipe  
Integrante: Felipe Santos de Jesus  
Discord: felipesjh7546
