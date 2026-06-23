# FOX SQL Server setup

Execute os scripts nesta ordem:

1. `01-create-database.sql`, se a database `fox` ainda nao existir.
2. `02-schema.sql`, dentro do SQL Server, para criar as tabelas em uma base nova/vazia.
3. `03-seed.sql`, para roles, etapas, motivos e usuario admin inicial.

Login inicial:

- Email: `admin@fox.com`
- Senha: `Admin@123`

A aplicacao aponta por padrao para `databaseName=fox`, mas aceita `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` por variavel de ambiente.