-- Apply once for ddl-auto=validate/none. Development ddl-auto=update adds this column.
ALTER TABLE verification_records ADD COLUMN supporting_document BOOLEAN NOT NULL DEFAULT FALSE;
