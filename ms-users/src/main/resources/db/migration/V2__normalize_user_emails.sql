-- Stop before updating anything if normalization would merge existing identities.
DO $$
BEGIN
    IF EXISTS (
        SELECT lower(btrim(email))
        FROM users
        GROUP BY lower(btrim(email))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Email normalization collision: resolve conflicting users before retrying migration';
    END IF;
END $$;

UPDATE users SET email = lower(btrim(email));

ALTER TABLE users ADD CONSTRAINT users_email_normalized
    CHECK (email = lower(btrim(email)));
