---
layout: page
title: Migration from 0.19.0 to 0.21.0
---

This guide contains instructions for migration from PowerAuth Server version 0.19.0 to version 0.21.0.

## Database Changes

Following DB changes occurred between version 0.19.0 and 0.21.0:
- Table `PA_ACTIVATION` - added columns `CTR_DATA` and `VERSION` for support of PowerAuth protocol version 3.0
- Table `PA_ACTIVATION` - column `ACTIVATION_CODE` replaces `ACTIVATION_ID_SHORT` and `ACTIVATION_OTP` in PowerAuth protocol version 3.0
- Table `PA_SIGNATURE_AUDIT` - added columns `ACTIVATION_CTR_DATA` and `VERSION` for support of PowerAuth protocol version 3.0

Migration script for Oracle:
```sql
--
--  Added Column ACTIVATION_CODE in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD ACTIVATION_CODE VARCHAR2(255 CHAR);

--
--  Added Column CTR_DATA in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD CTR_DATA VARCHAR2(255 CHAR);

--
--  Added Column VERSION in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD VERSION NUMBER(2,0) DEFAULT 2;

--
--  Added Column ACTIVATION_CTR_DATA in Table PA_SIGNATURE_AUDIT
--
ALTER TABLE PA_SIGNATURE_AUDIT ADD ACTIVATION_CTR_DATA VARCHAR2(255 CHAR);

--
--  Added Column VERSION in Table PA_SIGNATURE_AUDIT
--
ALTER TABLE PA_SIGNATURE_AUDIT ADD VERSION NUMBER(2,0) DEFAULT 2;

--
--  Column ACTIVATION_CODE Filled with Data from ACTIVATION_ID_SHORT || '-' || ACTIATION_OTP
--
UPDATE PA_ACTIVATION SET ACTIVATION_CODE = ACTIVATION_ID_SHORT || '-' || ACTIVATION_OTP;

--
--  Dropped Column ACTIVATION_ID_SHORT in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION DROP COLUMN ACTIVATION_ID_SHORT;

--
--  Dropped Column ACTIVATION_OTP in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION DROP COLUMN ACTIVATION_OTP;
```

Migration script for MySQL:
```sql
--
--  Added Column ACTIVATION_CODE in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD ACTIVATION_CODE VARCHAR(255);

--
--  Added Column CTR_DATA in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD CTR_DATA VARCHAR(255);

--
--  Added Column VERSION in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION ADD VERSION INT(2) DEFAULT 2;

--
--  Added Column ACTIVATION_CTR_DATA in Table PA_SIGNATURE_AUDIT
--
ALTER TABLE PA_SIGNATURE_AUDIT ADD ACTIVATION_CTR_DATA VARCHAR(255);

--
--  Added Column VERSION in Table PA_SIGNATURE_AUDIT
--
ALTER TABLE PA_SIGNATURE_AUDIT ADD VERSION INT(2) DEFAULT 2;

--
--  Column ACTIVATION_CODE Filled with Data from ACTIVATION_ID_SHORT || '-' || ACTIATION_OTP
--
UPDATE PA_ACTIVATION SET ACTIVATION_CODE = ACTIVATION_ID_SHORT || '-' || ACTIVATION_OTP;

--
--  Dropped Column ACTIVATION_ID_SHORT in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION DROP COLUMN ACTIVATION_ID_SHORT;

--
--  Dropped Column ACTIVATION_OTP in Table PA_ACTIVATION
--
ALTER TABLE PA_ACTIVATION DROP COLUMN ACTIVATION_OTP;
```

## Database Indexes

We have added database indexes into the official DDL scripts. In case you already added your own indexes to PowerAuth, please check that all required indexes are present. Otherwise you can add all of the indexes using provided SQL script.

Script for Oracle and PostgreSQL:
```sql
CREATE INDEX PA_ACTIVATION_APPLICATION ON PA_ACTIVATION(APPLICATION_ID);

CREATE INDEX PA_ACTIVATION_KEYPAIR ON PA_ACTIVATION(MASTER_KEYPAIR_ID);

CREATE INDEX PA_ACTIVATION_CODE ON PA_ACTIVATION(ACTIVATION_CODE);

CREATE INDEX PA_ACTIVATION_USER_ID ON PA_ACTIVATION(USER_ID);

CREATE INDEX PA_ACTIVATION_HISTORY_ACTIVATION ON PA_ACTIVATION_HISTORY(ACTIVATION_ID);

CREATE INDEX PA_ACTIVATION_HISTORY_CREATED ON PA_ACTIVATION_HISTORY(TIMESTAMP_CREATED);

CREATE INDEX PA_APPLICATION_VERSION_APPLICATION ON PA_APPLICATION_VERSION(APPLICATION_ID);

CREATE INDEX PA_MASTER_KEYPAIR_APPLICATION ON PA_MASTER_KEYPAIR(APPLICATION_ID);

CREATE UNIQUE INDEX PA_APP_VERSION_APP_KEY ON PA_APPLICATION_VERSION(APPLICATION_KEY);

CREATE INDEX PA_APP_CALLBACK_APP ON PA_APPLICATION_CALLBACK(APPLICATION_ID);

CREATE UNIQUE INDEX PA_INTEGRATION_TOKEN ON PA_INTEGRATION(CLIENT_TOKEN);

CREATE INDEX PA_SIGNATURE_AUDIT_ACTIVATION ON PA_SIGNATURE_AUDIT(ACTIVATION_ID);

CREATE INDEX PA_SIGNATURE_AUDIT_CREATED ON PA_SIGNATURE_AUDIT(TIMESTAMP_CREATED);

CREATE INDEX PA_TOKEN_ACTIVATION ON PA_TOKEN(ACTIVATION_ID);
```

Script for MySQL (foreign key indexes are created automatically in InnoDB):
```sql
CREATE INDEX PA_ACTIVATION_CODE ON PA_ACTIVATION(ACTIVATION_CODE);

CREATE INDEX PA_ACTIVATION_USER_ID ON PA_ACTIVATION(USER_ID);

CREATE INDEX PA_ACTIVATION_HISTORY_CREATED ON PA_ACTIVATION_HISTORY(TIMESTAMP_CREATED);

CREATE UNIQUE INDEX PA_APP_VERSION_APP_KEY ON PA_APPLICATION_VERSION(APPLICATION_KEY);

CREATE INDEX PA_APP_CALLBACK_APP ON PA_APPLICATION_CALLBACK(APPLICATION_ID);

CREATE UNIQUE INDEX PA_INTEGRATION_TOKEN ON PA_INTEGRATION(CLIENT_TOKEN);

CREATE INDEX PA_SIGNATURE_AUDIT_CREATED ON PA_SIGNATURE_AUDIT(TIMESTAMP_CREATED);
```

## PowerAuth Protocol Versioning

PowerAuth protocol version `3.0` support has been introduced in PowerAuth server version `0.21.0`.

The main changes in PowerAuth protocol are following:
- **ECIES scheme** is used for key agreement, encryption and message authentication.
- Activations use **hash based counter** instead of numeric counter for replay protection.

The changes of cryptography are documented in details in the [powerauth-crypto](https://github.com/wultra/powerauth-crypto/docs) project. 

The PowerAuth protocol upgrade caused following changes in PowerAuth server implementation:
- Activations, tokens and vault unlock use ECIES scheme in PowerAuth protocol version `3.0`. The original `2.1` implementations are still available. 
- Signatures use the hash based counter introduced in version `3.0`. The signatures use numeric or hash based counter depending on version of activation. 
- Instead of using separate *Activation ID Short* and *Activation OTP*, a single *Activation Code* is used.
- Binary format of encrypted status blob in *Activation Status* has been changed.
- Original PowerAuth End-to-end encryption has been deprecated in favor of ECIES-based encryption.
- PowerAuth SDK release `0.21.0` will trigger upgrade of activations to version `3` using new upgrade endpoints. Previous versions of the SDK use the `2` version of activations.

The original version `2.1` interfaces are still available both in the SOAP API, REST API and both client implementations, however migration to new interfaces is recommended. 
The version `2.1` interfaces will be deprecated in a future release. 

### JAXB Marshaller Context Path Update

The Spring configuration for JAXB marshaller has been updated to reflect two versions of generated client classes. The classes
are generated in two separate packages: `v2` and `v3`. You need to update the marshaller context path in case you use the 
Spring PowerAuth SOAP Service Client. 

Original marshaller context path setting in previous versions:
```java
marshaller.setContextPaths("io.getlime.powerauth.soap");
```

Marshaller context path setting in version `0.21.0`:
```java
marshaller.setContextPaths("io.getlime.powerauth.soap.v2", "io.getlime.powerauth.soap.v3");
```

### SOAP Interface Changes

PowerAuth server in version `0.21.0` supports both version `3.0` and version `2.1` of PowerAuth protocol. 

You can access the WSDL files in following URLs:
- version `3.0`: http://localhost:8080/powerauth-java-server/soap/service-v3.wsdl
- version `2.1`: http://localhost:8080/powerauth-java-server/soap/service-v2.wsdl

Note that the namespaces reflect the WSDL version:
- version `3.0`: http://getlime.io/security/powerauth/v3
- version `2.1`: http://getlime.io/security/powerauth/v2

The interface changes are described in details in chapter [SOAP Method Compatibility](./SOAP-Method-Compatibility.md).

### Client API Changes

Both Spring and Axis2 clients have been updated to support multiple versions of PowerAuth protocol.

The version `3.0` methods are available as default implementation directly on the client class. 

You can access the version `2.1` specific methods using the `v2()` method in the client. This method will be deprecated in a future release.

The interface changes are described in details in chapter [SOAP Method Compatibility](./SOAP-Method-Compatibility.md).

### Activation Short ID Change to Activation Code

The setting `generateActivationShortIdIterations` in PowerAuth server `application.properties` configuration has been replaced by `generateActivationCodeIterations`. In case you do not customize this setting, you can safely ignore this change.

The `UNABLE_TO_GENERATE_SHORT_ACTIVATION_ID` error code has been changed to `UNABLE_TO_GENERATE_ACTIVATION_CODE`. In case you do not process the PowerAuth server error codes, you can safely ignore this change.

### Updated error codes

Due to new functionality the list of [PowerAuth server error codes](./Server-Error-Codes.md) has been updated. If you are not using the error code list, you can ignore this change.