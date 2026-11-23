-- Index the customer filter. Unique document numbers only when none are duplicated.
-- Duplicate non-blank document numbers abort this migration. No rows are deleted.
-- Rollback: DROP INDEX uq_cari_transactions_document_no;
--           DROP INDEX idx_stock_movements_customer_id;

CREATE INDEX IF NOT EXISTS idx_stock_movements_customer_id ON stock_movements (customer_id);

DO $$
DECLARE
    duplicate_count integer;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT document_no
        FROM cari_transactions
        WHERE document_no IS NOT NULL
          AND btrim(document_no) <> ''
        GROUP BY document_no
        HAVING COUNT(*) > 1
    ) duplicates;

    IF duplicate_count > 0 THEN
        RAISE EXCEPTION
            'cari_transactions.document_no has % duplicate value(s); unique index was not created and no rows were deleted',
            duplicate_count;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cari_transactions_document_no
    ON cari_transactions (document_no)
    WHERE document_no IS NOT NULL
      AND btrim(document_no) <> '';
