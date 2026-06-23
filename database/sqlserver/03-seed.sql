USE [fox];
GO

DECLARE @now DATETIME2 = SYSDATETIME();

MERGE dbo.roles AS target
USING (VALUES
    (CONVERT(UNIQUEIDENTIFIER, '10000000-0000-0000-0000-000000000001'), 'ADMIN', 'Administrador do sistema'),
    (CONVERT(UNIQUEIDENTIFIER, '10000000-0000-0000-0000-000000000002'), 'USER', 'Usuario padrao'),
    (CONVERT(UNIQUEIDENTIFIER, '10000000-0000-0000-0000-000000000003'), 'ANALYST', 'Analista de processos'),
    (CONVERT(UNIQUEIDENTIFIER, '10000000-0000-0000-0000-000000000004'), 'ANALISTA', 'Analista de processos'),
    (CONVERT(UNIQUEIDENTIFIER, '10000000-0000-0000-0000-000000000005'), 'SUPERVISOR', 'Supervisor de processos')
) AS source (id, name, description)
ON target.name = source.name
WHEN NOT MATCHED THEN
    INSERT (id, name, description, created_at) VALUES (source.id, source.name, source.description, @now)
WHEN MATCHED THEN
    UPDATE SET description = source.description;

MERGE dbo.process_stage AS target
USING (VALUES
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000001'), 'AVISAR_SINISTRO', 'Avisar sinistro', 1),
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000002'), 'VISTORIA', 'Vistoria', 2),
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000003'), 'DOCUMENTACAO', 'Documentacao', 3),
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000004'), 'REGULACAO', 'Regulacao', 4),
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000005'), 'ANALISE', 'Analise', 5),
    (CONVERT(UNIQUEIDENTIFIER, '20000000-0000-0000-0000-000000000006'), 'PAGAMENTO', 'Pagamento', 6)
) AS source (id, code, name, display_order)
ON target.code = source.code
WHEN NOT MATCHED THEN
    INSERT (id, code, name, display_order, created_at) VALUES (source.id, source.code, source.name, source.display_order, @now)
WHEN MATCHED THEN
    UPDATE SET name = source.name, display_order = source.display_order;

MERGE dbo.process_reason AS target
USING (VALUES
    (CONVERT(UNIQUEIDENTIFIER, '30000000-0000-0000-0000-000000000001'), 'AVARIA_TRANSPORTE', 'Avaria de transporte'),
    (CONVERT(UNIQUEIDENTIFIER, '30000000-0000-0000-0000-000000000002'), 'FALTA_DOCUMENTO', 'Falta de documento'),
    (CONVERT(UNIQUEIDENTIFIER, '30000000-0000-0000-0000-000000000003'), 'RESSARCIMENTO', 'Ressarcimento')
) AS source (id, code, name)
ON target.code = source.code
WHEN NOT MATCHED THEN
    INSERT (id, code, name, created_at) VALUES (source.id, source.code, source.name, @now)
WHEN MATCHED THEN
    UPDATE SET name = source.name;

DECLARE @adminId UNIQUEIDENTIFIER = CONVERT(UNIQUEIDENTIFIER, '40000000-0000-0000-0000-000000000001');
DECLARE @adminEmail VARCHAR(255) = 'admin@fox.com';
DECLARE @adminPasswordHash VARCHAR(255) = '$2a$10$xbN9NOl1meA8pCJTp5bltOKDW684InSBfdHJAsjHHHqCsf2sTaliy';

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = @adminEmail)
BEGIN
    INSERT INTO dbo.users (id, name, email, password_hash, active, email_verified, last_login_at, created_at, updated_at, deleted_at)
    VALUES (@adminId, 'Administrador FOX', @adminEmail, @adminPasswordHash, 1, 1, NULL, @now, @now, NULL);
END
ELSE
BEGIN
    UPDATE dbo.users
    SET name = 'Administrador FOX', active = 1, email_verified = 1, updated_at = @now
    WHERE email = @adminEmail;

    SELECT @adminId = id FROM dbo.users WHERE email = @adminEmail;
END

INSERT INTO dbo.user_roles (user_id, role_id)
SELECT @adminId, r.id
FROM dbo.roles r
WHERE r.name IN ('ADMIN', 'USER', 'ANALYST', 'ANALISTA', 'SUPERVISOR')
  AND NOT EXISTS (
      SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = @adminId AND ur.role_id = r.id
  );
GO