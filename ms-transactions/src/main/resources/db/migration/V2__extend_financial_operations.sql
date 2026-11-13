ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;
ALTER TABLE transactions DROP CONSTRAINT transactions_currency_check;
-- O PostgreSQL nomeia automaticamente CHECKs que usam várias colunas.
-- Localizamos apenas as duas regras da V1 que serão substituídas, preservando as demais.
DO $$
DECLARE rule_name TEXT;
BEGIN
    FOR rule_name IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'transactions'::regclass AND contype = 'c'
          AND (pg_get_constraintdef(oid) LIKE '%amount_brl = amount%'
               OR (pg_get_constraintdef(oid) LIKE '%status%' AND pg_get_constraintdef(oid) LIKE '%balance_after%'))
    LOOP
        EXECUTE format('ALTER TABLE transactions DROP CONSTRAINT %I', rule_name);
    END LOOP;
END $$;
ALTER TABLE transactions
    ADD COLUMN exchange_rate NUMERIC(24,12) NOT NULL DEFAULT 1,
    ADD COLUMN exchange_date DATE,
    ADD COLUMN quoted_at TIMESTAMPTZ,
    ADD COLUMN exchange_bulletin VARCHAR(100),
    ADD COLUMN destination_account_id BIGINT,
    ADD COLUMN destination_user_id BIGINT,
    ADD COLUMN destination_balance_before NUMERIC(19,2),
    ADD COLUMN destination_balance_after NUMERIC(19,2),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD CONSTRAINT transactions_type_check CHECK (type IN ('PURCHASE','DEPOSIT','WITHDRAWAL','TRANSFER')),
    ADD CONSTRAINT transactions_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT transactions_conversion_check CHECK (amount_brl > 0 AND exchange_rate > 0 AND
        ((currency = 'BRL' AND exchange_rate = 1 AND amount_brl = amount) OR
         (currency <> 'BRL' AND exchange_date IS NOT NULL AND quoted_at IS NOT NULL AND exchange_bulletin IS NOT NULL))),
    ADD CONSTRAINT transactions_destination_fk FOREIGN KEY (destination_account_id,destination_user_id) REFERENCES accounts(id,user_id),
    ADD CONSTRAINT transactions_destination_check CHECK (
        (type <> 'TRANSFER' AND destination_account_id IS NULL AND destination_user_id IS NULL
            AND destination_balance_before IS NULL AND destination_balance_after IS NULL)
        OR (type = 'TRANSFER' AND destination_account_id IS NOT NULL AND destination_user_id IS NOT NULL
            AND destination_user_id <> user_id AND destination_balance_before IS NOT NULL AND destination_balance_before >= 0
            AND destination_balance_after IS NOT NULL AND destination_balance_after >= 0)),
    ADD CONSTRAINT transactions_effect_check CHECK (
        (status = 'APPROVED' AND rejection_reason IS NULL AND
            balance_after = balance_before + CASE WHEN type = 'DEPOSIT' THEN amount_brl ELSE -amount_brl END
            AND (type <> 'TRANSFER' OR destination_balance_after = destination_balance_before + amount_brl))
        OR (status = 'REJECTED' AND rejection_reason IS NOT NULL AND
            rejection_reason IN ('INSUFFICIENT_BALANCE','PURCHASE_LIMIT_EXCEEDED','BALANCE_LIMIT_EXCEEDED','DESTINATION_BALANCE_LIMIT_EXCEEDED')
            AND balance_after = balance_before AND (type <> 'TRANSFER' OR destination_balance_after = destination_balance_before)));

CREATE TABLE transaction_cancellations (
    transaction_id BIGINT PRIMARY KEY REFERENCES transactions(id),
    cancelled_at TIMESTAMPTZ NOT NULL,
    source_balance_before NUMERIC(19,2) NOT NULL CHECK (source_balance_before >= 0),
    source_balance_after NUMERIC(19,2) NOT NULL CHECK (source_balance_after >= 0),
    destination_balance_before NUMERIC(19,2),
    destination_balance_after NUMERIC(19,2)
);
CREATE INDEX transactions_owner_created ON transactions(user_id,created_at);
