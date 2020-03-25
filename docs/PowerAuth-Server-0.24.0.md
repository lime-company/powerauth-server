# Migration from 0.23.0 to 0.24.0

This guide contains instructions for migration from PowerAuth Server version `0.23.0` to version `0.24.0`.

## Database Changes

Following DB changes occurred between version 0.23.0 and 0.24.0:
- Table `pa_activation` - added columns `device_info`, `platform`, `activation_otp`, `activation_otp_validation`.

Migration script for Oracle:
```sql
ALTER TABLE "PA_ACTIVATION" ADD "PLATFORM" VARCHAR2(255 CHAR);
ALTER TABLE "PA_ACTIVATION" ADD "DEVICE_INFO" VARCHAR2(255 CHAR);
ALTER TABLE "PA_ACTIVATION" ADD "ACTIVATION_OTP" VARCHAR2(255 CHAR);
ALTER TABLE "PA_ACTIVATION" ADD "ACTIVATION_OTP_VALIDATION" NUMBER(2,0) DEFAULT 0 NOT NULL;
```

Migration script for MySQL:
```sql
ALTER TABLE `pa_activation` ADD `platform` varchar(255);
ALTER TABLE `pa_activation` ADD `device_info` varchar(255);
ALTER TABLE `pa_activation` ADD `activation_otp` varchar(255);
ALTER TABLE `pa_activation` ADD `activation_otp_validation` int DEFAULT 0 NOT NULL;
```

Migration script for PostgreSQL:
```sql
ALTER TABLE "pa_activation" ADD "platform" VARCHAR(255);
ALTER TABLE "pa_activation" ADD "device_info" VARCHAR(255);
ALTER TABLE "pa_activation" ADD "activation_otp" VARCHAR(255);
ALTER TABLE `pa_activation` ADD `activation_otp_validation` INTEGER DEFAULT 0 NOT NULL;
```

## SOAP Interface Changes

PowerAuth server in version `0.24.0` slightly changed SOAP interface for protocol version `3` (namespace `http://getlime.io/security/powerauth/v3`):

- Added new enumeration `ActivationOtpValidation` with following values:
  - `NONE` – no additional OTP validation is required during the activation.
  - `ON_KEYS_EXCHANGE` – an additional OTP is validated during the keys-exchange activation phase.
  - `ON_COMMIT` – an additional OTP is validated during the activation commit phase.
- `InitActivationRequest` request object has now optional `activationOtp` and `activationOtpValidation` properties. 
- `PrepareActivationResponse` response object now contains `activationStatus` property that contains the current status of the activation.
- `CreateActivationRequest` request object has now optional `activationOtp` property.
- `CommitActivationRequest` request object has now optional `activationOtp` property.
- `RecoveryCodeActivationRequest` request object has now optional `activationOtp` property.
- `GetActivationStatusResponse` response object now contains new `activationOtpValidation`, `platform` and `deviceInfo` properties.
- `UpdateActivationOtp` is a new SOAP API method with `UpdateActivationOtpRequest` and `UpdateActivationOtpResponse` objects.

Both Spring and Axis2 clients have been updated to support latest changes in SOAP interface.

## RESTful integration Changes

PowerAuth restful integration libraries in version `0.24.0` have the following important changes:

- It's now possible to auto-commit activation when it's created with using the regular activation code. So, your implementation of `CustomActivationProvider` must be prepared that `shouldAutoCommitActivation()` method can receive `CODE` as a new supported type of activation.