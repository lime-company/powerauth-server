--
--  Drop all tables.
--
DROP TABLE IF EXISTS "pa_activation" CASCADE;
DROP TABLE IF EXISTS "pa_activation_history" CASCADE;
DROP TABLE IF EXISTS "pa_application" CASCADE;
DROP TABLE IF EXISTS "pa_application_version" CASCADE;
DROP TABLE IF EXISTS "pa_token" CASCADE;
DROP TABLE IF EXISTS "pa_master_keypair" CASCADE;
DROP TABLE IF EXISTS "pa_signature_audit" CASCADE;
DROP TABLE IF EXISTS "pa_integration" CASCADE;
DROP TABLE IF EXISTS "pa_application_callback" CASCADE;

--
--  Drop all sequences.
--
DROP SEQUENCE IF EXISTS "pa_application_seq";
DROP SEQUENCE IF EXISTS "pa_application_version_seq";
DROP SEQUENCE IF EXISTS "pa_master_keypair_seq";
DROP SEQUENCE IF EXISTS "pa_signature_audit_seq";
DROP SEQUENCE IF EXISTS "pa_activation_history_seq";
