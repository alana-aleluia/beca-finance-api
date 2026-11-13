# MS2 — Transações financeiras

A documentação de execução, variáveis, endpoints, regras, Postman, testes e limitações
está no [README da raiz](../README.md).

Arquitetura: domain/model contém as decisões financeiras; application/usecase coordena
as integrações fora da transação; interfaces Port ficam em application/port/in e out.
adapter/in recebe HTTP e autentica, adapter/out integra MS1/MockAPI/BrasilAPI e PostgreSQL.

O Compose deste módulo permanece disponível para executar somente o banco MS2,
reutilizando o volume beca-transactions_transactions_data. Não execute dois servidores
sobre o mesmo volume. Para a entrega completa, use docker compose up --build na raiz.