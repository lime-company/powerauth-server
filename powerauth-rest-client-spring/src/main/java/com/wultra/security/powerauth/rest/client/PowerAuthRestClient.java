/*
 * PowerAuth Server and related software components
 * Copyright (C) 2020 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.security.powerauth.rest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.entity.Activation;
import com.wultra.security.powerauth.client.model.entity.ActivationHistoryItem;
import com.wultra.security.powerauth.client.model.entity.CallbackUrl;
import com.wultra.security.powerauth.client.model.entity.SignatureAuditItem;
import com.wultra.security.powerauth.client.model.enumeration.*;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.error.PowerAuthError;
import com.wultra.security.powerauth.client.model.error.PowerAuthErrorRecovery;
import com.wultra.security.powerauth.client.model.request.*;
import com.wultra.security.powerauth.client.model.response.*;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Class implementing a PowerAuth REST client.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 *
 */
public class PowerAuthRestClient implements PowerAuthClient {

    private static final Logger logger = LoggerFactory.getLogger(PowerAuthRestClient.class);

    private static final String PA_REST_V3_PREFIX = "/v3";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * PowerAuth REST client constructor.
     *
     * @param baseUrl BASE URL of REST endpoints.
     */
    public PowerAuthRestClient(String baseUrl) throws PowerAuthClientException {
        this(baseUrl, new PowerAuthRestClientConfiguration());
    }

    /**
     * PowerAuth REST client constructor.
     *
     * @param baseUrl Base URL of REST endpoints.
     */
    public PowerAuthRestClient(String baseUrl, PowerAuthRestClientConfiguration config) throws PowerAuthClientException {
        final DefaultRestClient.Builder builder = DefaultRestClient.builder().baseUrl(baseUrl)
                .acceptInvalidCertificate(config.getAcceptInvalidSslCertificate())
                .connectionTimeout(config.getConnectTimeout())
                .maxInMemorySize(config.getMaxMemorySize());
        if (config.isProxyEnabled()) {
            final DefaultRestClient.ProxyBuilder proxyBuilder = builder.proxy().host(config.getProxyHost()).port(config.getProxyPort());
            if (config.getProxyUsername() != null) {
                proxyBuilder.username(config.getProxyUsername()).password(config.getProxyPassword());
            }
            proxyBuilder.build();
        }
        if (config.getPowerAuthClientToken() != null) {
            builder.httpBasicAuth().username(config.getPowerAuthClientToken()).password(config.getPowerAuthClientSecret()).build();
        }
        try {
            restClient = builder.build();
        } catch (RestClientException ex) {
            throw new PowerAuthClientException("REST client initialization failed, error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Call the PowerAuth v3 API.
     *
     * @param path Path of the endpoint.
     * @param request Request object.
     * @param responseType Response type.
     * @return Response.
     */
    private <T> T callV3RestApi(String path, Object request, Class<T> responseType) throws PowerAuthClientException {
        final ObjectRequest<?> objectRequest = new ObjectRequest<>(request);
        try {
            final ObjectResponse<T> objectResponse = restClient.postObject(PA_REST_V3_PREFIX + path, objectRequest, responseType);
            return objectResponse.getResponseObject();
        } catch (RestClientException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // Error handling for PowerAuth errors
                handleBadRequestError(ex);
            }
            // Error handling for generic HTTP errors
            throw new PowerAuthClientException(ex.getMessage(), ex);
        }
    }

    /**
     * Handle the HTTP response with BAD_REQUEST status code.
     * @param ex Exception which captured the error.
     * @throws PowerAuthClientException PowerAuth client exception.
     */
    private void handleBadRequestError(RestClientException ex) throws PowerAuthClientException {
        // Try to parse exception into PowerAuthError model class
        try {
            final TypeReference<ObjectResponse<PowerAuthError>> typeReference = new TypeReference<ObjectResponse<PowerAuthError>>(){};
            final ObjectResponse<PowerAuthError> error = objectMapper.readValue(ex.getResponse(), typeReference);
            if (error == null || error.getResponseObject() == null) {
                throw new PowerAuthClientException("Invalid response object");
            }
            if ("ERR_RECOVERY".equals(error.getResponseObject().getCode())) {
                // In case of special recovery errors, return PowerAuthErrorRecovery which includes additional information about recovery
                final TypeReference<ObjectResponse<PowerAuthErrorRecovery>> PowerAuthErrorRecovery = new TypeReference<ObjectResponse<PowerAuthErrorRecovery>>(){};
                final ObjectResponse<PowerAuthErrorRecovery> errorRecovery = objectMapper.readValue(ex.getResponse(), PowerAuthErrorRecovery);
                if (errorRecovery == null || errorRecovery.getResponseObject() == null) {
                    throw new PowerAuthClientException("Invalid response object for recovery");
                }
                throw new PowerAuthClientException(errorRecovery.getResponseObject().getMessage(), ex, errorRecovery.getResponseObject());
            }
            throw new PowerAuthClientException(error.getResponseObject().getMessage(), ex, error.getResponseObject());
        } catch (IOException ex2) {
            // Parsing failed, return a regular error
            throw new PowerAuthClientException(ex.getMessage(), ex);
        }
    }

    /**
     * Convert date to XMLGregorianCalendar
     *
     * @param date Date to be converted.
     * @return A new instance of {@link XMLGregorianCalendar}.
     */
    private Instant instantWithDate(Date date) {
        return Instant.ofEpochMilli(date.getTime());
    }

    @Override
    public GetSystemStatusResponse getSystemStatus() throws PowerAuthClientException {
        return callV3RestApi("/status", new Object(), GetSystemStatusResponse.class);
    }

    @Override
    public GetErrorCodeListResponse getErrorList(GetErrorCodeListRequest request) throws PowerAuthClientException {
        return callV3RestApi("/error/list", request, GetErrorCodeListResponse.class);
    }

    @Override
    public GetErrorCodeListResponse getErrorList() throws PowerAuthClientException {
        final GetErrorCodeListRequest request = new GetErrorCodeListRequest();
        request.setLanguage("en");
        return getErrorList(request);
    }

    @Override
    public InitActivationResponse initActivation(InitActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/init", request, InitActivationResponse.class);
    }

    @Override
    public InitActivationResponse initActivation(String userId, Long applicationId) throws PowerAuthClientException {
        return this.initActivation(userId, applicationId, null, null, ActivationOtpValidation.NONE, null);
    }

    @Override
    public InitActivationResponse initActivation(String userId, Long applicationId, ActivationOtpValidation otpValidation, String otp) throws PowerAuthClientException {
        return this.initActivation(userId, applicationId, null, null, otpValidation, otp);
    }

    @Override
    public InitActivationResponse initActivation(String userId, Long applicationId, Long maxFailureCount, Date timestampActivationExpire) throws PowerAuthClientException {
        return this.initActivation(userId, applicationId, maxFailureCount, timestampActivationExpire, ActivationOtpValidation.NONE, null);
    }

    @Override
    public InitActivationResponse initActivation(String userId, Long applicationId, Long maxFailureCount, Date timestampActivationExpire, ActivationOtpValidation otpValidation, String otp) throws PowerAuthClientException {
        final InitActivationRequest request = new InitActivationRequest();
        request.setUserId(userId);
        request.setApplicationId(applicationId);
        request.setActivationOtpValidation(otpValidation);
        request.setActivationOtp(otp);
        if (maxFailureCount != null) {
            request.setMaxFailureCount(maxFailureCount);
        }
        if (timestampActivationExpire != null) {
            request.setTimestampActivationExpire(instantWithDate(timestampActivationExpire));
        }
        return this.initActivation(request);
    }

    @Override
    public PrepareActivationResponse prepareActivation(PrepareActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/prepare", request, PrepareActivationResponse.class);
    }

    @Override
    public PrepareActivationResponse prepareActivation(String activationCode, String applicationKey, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final PrepareActivationRequest request = new PrepareActivationRequest();
        request.setActivationCode(activationCode);
        request.setApplicationKey(applicationKey);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return prepareActivation(request);
    }

    @Override
    public CreateActivationResponse createActivation(CreateActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/create", request, CreateActivationResponse.class);
    }

    @Override
    public CreateActivationResponse createActivation(String userId, Date timestampActivationExpire, Long maxFailureCount, String applicationKey, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final CreateActivationRequest request = new CreateActivationRequest();
        request.setUserId(userId);
        if (timestampActivationExpire != null) {
            request.setTimestampActivationExpire(instantWithDate(timestampActivationExpire));
        }
        if (maxFailureCount != null) {
            request.setMaxFailureCount(maxFailureCount);
        }
        request.setApplicationKey(applicationKey);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return createActivation(request);
    }

    @Override
    public UpdateActivationOtpResponse updateActivationOtp(String activationId, String externalUserId, String activationOtp) throws PowerAuthClientException {
        final UpdateActivationOtpRequest request = new UpdateActivationOtpRequest();
        request.setActivationId(activationId);
        request.setExternalUserId(externalUserId);
        request.setActivationOtp(activationOtp);
        return updateActivationOtp(request);
    }

    @Override
    public UpdateActivationOtpResponse updateActivationOtp(UpdateActivationOtpRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/otp/update", request, UpdateActivationOtpResponse.class);
    }

    @Override
    public CommitActivationResponse commitActivation(CommitActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/commit", request, CommitActivationResponse.class);
    }

    @Override
    public CommitActivationResponse commitActivation(String activationId, String externalUserId) throws PowerAuthClientException {
        final CommitActivationRequest request = new CommitActivationRequest();
        request.setActivationId(activationId);
        request.setExternalUserId(externalUserId);
        return this.commitActivation(request);
    }

    @Override
    public CommitActivationResponse commitActivation(String activationId, String externalUserId, String activationOtp) throws PowerAuthClientException {
        final CommitActivationRequest request = new CommitActivationRequest();
        request.setActivationId(activationId);
        request.setExternalUserId(externalUserId);
        request.setActivationOtp(activationOtp);
        return this.commitActivation(request);
    }

    @Override
    public GetActivationStatusResponse getActivationStatus(GetActivationStatusRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/status", request, GetActivationStatusResponse.class);
    }

    @Override
    public GetActivationStatusResponse getActivationStatus(String activationId) throws PowerAuthClientException {
        final GetActivationStatusResponse response = this.getActivationStatusWithEncryptedStatusBlob(activationId, null);
        response.setEncryptedStatusBlob(null);
        return response;
    }

    @Override
    public GetActivationStatusResponse getActivationStatusWithEncryptedStatusBlob(String activationId, String challenge) throws PowerAuthClientException {
        final GetActivationStatusRequest request = new GetActivationStatusRequest();
        request.setActivationId(activationId);
        request.setChallenge(challenge);
        return this.getActivationStatus(request);
    }

    @Override
    public RemoveActivationResponse removeActivation(RemoveActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/remove", request, RemoveActivationResponse.class);
    }

    @Override
    public RemoveActivationResponse removeActivation(String activationId, String externalUserId) throws PowerAuthClientException {
        return this.removeActivation(activationId, externalUserId, false);
    }

    @Override
    public RemoveActivationResponse removeActivation(String activationId, String externalUserId, Boolean revokeRecoveryCodes) throws PowerAuthClientException {
        final RemoveActivationRequest request = new RemoveActivationRequest();
        request.setActivationId(activationId);
        request.setExternalUserId(externalUserId);
        request.setRevokeRecoveryCodes(revokeRecoveryCodes);
        return this.removeActivation(request);
    }

    @Override
    public GetActivationListForUserResponse getActivationListForUser(GetActivationListForUserRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/list", request, GetActivationListForUserResponse.class);
    }

    @Override
    public List<Activation> getActivationListForUser(String userId) throws PowerAuthClientException {
        final GetActivationListForUserRequest request = new GetActivationListForUserRequest();
        request.setUserId(userId);
        return this.getActivationListForUser(request).getActivations();
    }

    @Override
    public LookupActivationsResponse lookupActivations(LookupActivationsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/lookup", request, LookupActivationsResponse.class);
    }

    @Override
    public List<Activation> lookupActivations(List<String> userIds, List<Long> applicationIds, Date timestampLastUsedBefore, Date timestampLastUsedAfter, ActivationStatus activationStatus, List<String> activationFlags) throws PowerAuthClientException {
        final LookupActivationsRequest request = new LookupActivationsRequest();
        request.getUserIds().addAll(userIds);
        if (applicationIds != null) {
            request.getApplicationIds().addAll(applicationIds);
        }
        if (timestampLastUsedBefore != null) {
            request.setTimestampLastUsedBefore(instantWithDate(timestampLastUsedBefore));
        }
        if (timestampLastUsedAfter != null) {
            request.setTimestampLastUsedAfter(instantWithDate(timestampLastUsedAfter));
        }
        if (activationStatus != null) {
            request.setActivationStatus(activationStatus);
        }
        if (activationFlags != null) {
            request.getActivationFlags().addAll(activationFlags);
        }
        return this.lookupActivations(request).getActivations();
    }

    @Override
    public UpdateStatusForActivationsResponse updateStatusForActivations(UpdateStatusForActivationsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/status/update", request, UpdateStatusForActivationsResponse.class);
    }

    @Override
    public UpdateStatusForActivationsResponse updateStatusForActivations(List<String> activationIds, ActivationStatus activationStatus) throws PowerAuthClientException {
        final UpdateStatusForActivationsRequest request = new UpdateStatusForActivationsRequest();
        request.getActivationIds().addAll(activationIds);
        if (activationStatus != null) {
            request.setActivationStatus(activationStatus);
        }
        return this.updateStatusForActivations(request);
    }

    @Override
    public VerifySignatureResponse verifySignature(VerifySignatureRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/verify", request, VerifySignatureResponse.class);
    }

    @Override
    public VerifySignatureResponse verifySignature(String activationId, String applicationKey, String data, String signature, SignatureType signatureType, String signatureVersion, Long forcedSignatureVersion) throws PowerAuthClientException {
        final VerifySignatureRequest request = new VerifySignatureRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setData(data);
        request.setSignature(signature);
        request.setSignatureType(signatureType);
        request.setSignatureVersion(signatureVersion);
        request.setForcedSignatureVersion(forcedSignatureVersion);
        return this.verifySignature(request);
    }

    @Override
    public CreatePersonalizedOfflineSignaturePayloadResponse createPersonalizedOfflineSignaturePayload(CreatePersonalizedOfflineSignaturePayloadRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/offline/personalized/create", request, CreatePersonalizedOfflineSignaturePayloadResponse.class);
    }

    @Override
    public CreatePersonalizedOfflineSignaturePayloadResponse createPersonalizedOfflineSignaturePayload(String activationId, String data) throws PowerAuthClientException {
        final CreatePersonalizedOfflineSignaturePayloadRequest request = new CreatePersonalizedOfflineSignaturePayloadRequest();
        request.setActivationId(activationId);
        request.setData(data);
        return createPersonalizedOfflineSignaturePayload(request);
    }

    @Override
    public CreateNonPersonalizedOfflineSignaturePayloadResponse createNonPersonalizedOfflineSignaturePayload(CreateNonPersonalizedOfflineSignaturePayloadRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/offline/non-personalized/create", request, CreateNonPersonalizedOfflineSignaturePayloadResponse.class);
    }

    @Override
    public CreateNonPersonalizedOfflineSignaturePayloadResponse createNonPersonalizedOfflineSignaturePayload(long applicationId, String data) throws PowerAuthClientException {
        final CreateNonPersonalizedOfflineSignaturePayloadRequest request = new CreateNonPersonalizedOfflineSignaturePayloadRequest();
        request.setApplicationId(applicationId);
        request.setData(data);
        return createNonPersonalizedOfflineSignaturePayload(request);
    }

    @Override
    public VerifyOfflineSignatureResponse verifyOfflineSignature(VerifyOfflineSignatureRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/offline/verify", request, VerifyOfflineSignatureResponse.class);
    }

    @Override
    public VerifyOfflineSignatureResponse verifyOfflineSignature(String activationId, String data, String signature, boolean allowBiometry) throws PowerAuthClientException {
        final VerifyOfflineSignatureRequest request = new VerifyOfflineSignatureRequest();
        request.setActivationId(activationId);
        request.setData(data);
        request.setSignature(signature);
        request.setAllowBiometry(allowBiometry);
        return verifyOfflineSignature(request);
    }

    @Override
    public VaultUnlockResponse unlockVault(VaultUnlockRequest request) throws PowerAuthClientException {
        return callV3RestApi("/vault/unlock", request, VaultUnlockResponse.class);
    }

    @Override
    public VaultUnlockResponse unlockVault(String activationId, String applicationKey, String signature, SignatureType signatureType, String signatureVersion, String signedData, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final VaultUnlockRequest request = new VaultUnlockRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setSignedData(signedData);
        request.setSignature(signature);
        request.setSignatureType(signatureType);
        request.setSignatureVersion(signatureVersion);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return unlockVault(request);
    }

    @Override
    public VerifyECDSASignatureResponse verifyECDSASignature(VerifyECDSASignatureRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/ecdsa/verify", request, VerifyECDSASignatureResponse.class);
    }

    @Override
    public VerifyECDSASignatureResponse verifyECDSASignature(String activationId, String data, String signature) throws PowerAuthClientException {
        final VerifyECDSASignatureRequest request = new VerifyECDSASignatureRequest();
        request.setActivationId(activationId);
        request.setData(data);
        request.setSignature(signature);
        return this.verifyECDSASignature(request);
    }

    @Override
    public SignatureAuditResponse getSignatureAuditLog(SignatureAuditRequest request) throws PowerAuthClientException {
        return callV3RestApi("/signature/list", request, SignatureAuditResponse.class);
    }

    @Override
    public List<SignatureAuditItem> getSignatureAuditLog(String userId, Date startingDate, Date endingDate) throws PowerAuthClientException {
        final SignatureAuditRequest request = new SignatureAuditRequest();
        request.setUserId(userId);
        request.setTimestampFrom(instantWithDate(startingDate));
        request.setTimestampTo(instantWithDate(endingDate));
        return this.getSignatureAuditLog(request).getItems();
    }

    @Override
    public List<SignatureAuditItem> getSignatureAuditLog(String userId, Long applicationId, Date startingDate, Date endingDate) throws PowerAuthClientException {
        final SignatureAuditRequest request = new SignatureAuditRequest();
        request.setUserId(userId);
        request.setApplicationId(applicationId);
        request.setTimestampFrom(instantWithDate(startingDate));
        request.setTimestampTo(instantWithDate(endingDate));
        return this.getSignatureAuditLog(request).getItems();
    }

    @Override
    public ActivationHistoryResponse getActivationHistory(ActivationHistoryRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/history", request, ActivationHistoryResponse.class);
    }

    @Override
    public List<ActivationHistoryItem> getActivationHistory(String activationId, Date startingDate, Date endingDate) throws PowerAuthClientException {
        final ActivationHistoryRequest request = new ActivationHistoryRequest();
        request.setActivationId(activationId);
        request.setTimestampFrom(instantWithDate(startingDate));
        request.setTimestampTo(instantWithDate(endingDate));
        return this.getActivationHistory(request).getItems();
    }

    @Override
    public BlockActivationResponse blockActivation(BlockActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/block", request, BlockActivationResponse.class);
    }

    @Override
    public BlockActivationResponse blockActivation(String activationId, String reason, String externalUserId) throws PowerAuthClientException {
        final BlockActivationRequest request = new BlockActivationRequest();
        request.setActivationId(activationId);
        request.setReason(reason);
        request.setExternalUserId(externalUserId);
        return this.blockActivation(request);
    }

    @Override
    public UnblockActivationResponse unblockActivation(UnblockActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/unblock", request, UnblockActivationResponse.class);
    }

    @Override
    public UnblockActivationResponse unblockActivation(String activationId, String externalUserId) throws PowerAuthClientException {
        final UnblockActivationRequest request = new UnblockActivationRequest();
        request.setActivationId(activationId);
        request.setExternalUserId(externalUserId);
        return this.unblockActivation(request);
    }

    @Override
    public GetApplicationListResponse getApplicationList() throws PowerAuthClientException {
        return callV3RestApi("/application/list", new Object(), GetApplicationListResponse.class);
    }

    @Override
    public GetApplicationDetailResponse getApplicationDetail(GetApplicationDetailRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/detail", request, GetApplicationDetailResponse.class);
    }

    @Override
    public GetApplicationDetailResponse getApplicationDetail(Long applicationId) throws PowerAuthClientException {
        final GetApplicationDetailRequest request = new GetApplicationDetailRequest();
        request.setApplicationId(applicationId);
        return this.getApplicationDetail(request);
    }

    @Override
    public GetApplicationDetailResponse getApplicationDetail(String applicationName) throws PowerAuthClientException {
        final GetApplicationDetailRequest request = new GetApplicationDetailRequest();
        request.setApplicationName(applicationName);
        return this.getApplicationDetail(request);
    }

    @Override
    public LookupApplicationByAppKeyResponse lookupApplicationByAppKey(LookupApplicationByAppKeyRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/detail/version", request, LookupApplicationByAppKeyResponse.class);
    }

    @Override
    public LookupApplicationByAppKeyResponse lookupApplicationByAppKey(String applicationKey) throws PowerAuthClientException {
        final LookupApplicationByAppKeyRequest request = new LookupApplicationByAppKeyRequest();
        request.setApplicationKey(applicationKey);
        return this.lookupApplicationByAppKey(request);
    }

    @Override
    public CreateApplicationResponse createApplication(CreateApplicationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/create", request, CreateApplicationResponse.class);
    }

    @Override
    public CreateApplicationResponse createApplication(String name) throws PowerAuthClientException {
        final CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationName(name);
        return this.createApplication(request);
    }

    @Override
    public CreateApplicationVersionResponse createApplicationVersion(CreateApplicationVersionRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/version/create", request, CreateApplicationVersionResponse.class);
    }

    @Override
    public CreateApplicationVersionResponse createApplicationVersion(Long applicationId, String versionName) throws PowerAuthClientException {
        final CreateApplicationVersionRequest request = new CreateApplicationVersionRequest();
        request.setApplicationId(applicationId);
        request.setApplicationVersionName(versionName);
        return this.createApplicationVersion(request);
    }

    @Override
    public UnsupportApplicationVersionResponse unsupportApplicationVersion(UnsupportApplicationVersionRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/version/unsupport", request, UnsupportApplicationVersionResponse.class);
    }

    @Override
    public UnsupportApplicationVersionResponse unsupportApplicationVersion(Long versionId) throws PowerAuthClientException {
        final UnsupportApplicationVersionRequest request = new UnsupportApplicationVersionRequest();
        request.setApplicationVersionId(versionId);
        return this.unsupportApplicationVersion(request);
    }

    @Override
    public SupportApplicationVersionResponse supportApplicationVersion(SupportApplicationVersionRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/version/support", request, SupportApplicationVersionResponse.class);
    }

    @Override
    public SupportApplicationVersionResponse supportApplicationVersion(Long versionId) throws PowerAuthClientException {
        final SupportApplicationVersionRequest request = new SupportApplicationVersionRequest();
        request.setApplicationVersionId(versionId);
        return this.supportApplicationVersion(request);
    }

    @Override
    public CreateIntegrationResponse createIntegration(CreateIntegrationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/integration/create", request, CreateIntegrationResponse.class);
    }

    @Override
    public CreateIntegrationResponse createIntegration(String name) throws PowerAuthClientException {
        final CreateIntegrationRequest request = new CreateIntegrationRequest();
        request.setName(name);
        return this.createIntegration(request);
    }

    @Override
    public GetIntegrationListResponse getIntegrationList() throws PowerAuthClientException {
        return callV3RestApi("/integration/list", new Object(), GetIntegrationListResponse.class);
    }


    @Override
    public RemoveIntegrationResponse removeIntegration(RemoveIntegrationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/integration/remove", request, RemoveIntegrationResponse.class);
    }

    @Override
    public RemoveIntegrationResponse removeIntegration(String id) throws PowerAuthClientException {
        final RemoveIntegrationRequest request = new RemoveIntegrationRequest();
        request.setId(id);
        return this.removeIntegration(request);
    }

    @Override
    public CreateCallbackUrlResponse createCallbackUrl(CreateCallbackUrlRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/callback/create", request, CreateCallbackUrlResponse.class);
    }

    @Override
    public CreateCallbackUrlResponse createCallbackUrl(Long applicationId, String name, String callbackUrl, List<String> attributes) throws PowerAuthClientException {
        final CreateCallbackUrlRequest request = new CreateCallbackUrlRequest();
        request.setApplicationId(applicationId);
        request.setName(name);
        request.setCallbackUrl(callbackUrl);
        if (attributes != null) {
            request.getAttributes().addAll(attributes);
        }
        return this.createCallbackUrl(request);
    }

    @Override
    public UpdateCallbackUrlResponse updateCallbackUrl(UpdateCallbackUrlRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/callback/update", request, UpdateCallbackUrlResponse.class);
    }

    @Override
    public UpdateCallbackUrlResponse updateCallbackUrl(String id, long applicationId, String name, String callbackUrl, List<String> attributes) throws PowerAuthClientException {
        final UpdateCallbackUrlRequest request = new UpdateCallbackUrlRequest();
        request.setId(id);
        request.setApplicationId(applicationId);
        request.setName(name);
        request.setCallbackUrl(callbackUrl);
        if (attributes != null) {
            request.getAttributes().addAll(attributes);
        }
        return this.updateCallbackUrl(request);
    }

    @Override
    public GetCallbackUrlListResponse getCallbackUrlList(GetCallbackUrlListRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/callback/list", request, GetCallbackUrlListResponse.class);
    }

    @Override
    public List<CallbackUrl> getCallbackUrlList(Long applicationId) throws PowerAuthClientException {
        final GetCallbackUrlListRequest request = new GetCallbackUrlListRequest();
        request.setApplicationId(applicationId);
        return getCallbackUrlList(request).getCallbackUrlList();
    }

    @Override
    public RemoveCallbackUrlResponse removeCallbackUrl(RemoveCallbackUrlRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/callback/remove", request, RemoveCallbackUrlResponse.class);
    }

    @Override
    public RemoveCallbackUrlResponse removeCallbackUrl(String callbackUrlId) throws PowerAuthClientException {
        final RemoveCallbackUrlRequest request = new RemoveCallbackUrlRequest();
        request.setId(callbackUrlId);
        return removeCallbackUrl(request);
    }

    @Override
    public CreateTokenResponse createToken(CreateTokenRequest request) throws PowerAuthClientException {
        return callV3RestApi("/token/create", request, CreateTokenResponse.class);
    }

    @Override
    public CreateTokenResponse createToken(String activationId, String applicationKey, String ephemeralPublicKey, String encryptedData, String mac, String nonce, SignatureType signatureType) throws PowerAuthClientException {
        final CreateTokenRequest request = new CreateTokenRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setNonce(nonce);
        request.setSignatureType(signatureType);
        return createToken(request);
    }

    @Override
    public ValidateTokenResponse validateToken(ValidateTokenRequest request) throws PowerAuthClientException {
        return callV3RestApi("/token/validate", request, ValidateTokenResponse.class);
    }

    @Override
    public ValidateTokenResponse validateToken(String tokenId, String nonce, long timestamp, String tokenDigest) throws PowerAuthClientException {
        final ValidateTokenRequest request = new ValidateTokenRequest();
        request.setTokenId(tokenId);
        request.setNonce(nonce);
        request.setTimestamp(timestamp);
        request.setTokenDigest(tokenDigest);
        return validateToken(request);
    }

    @Override
    public RemoveTokenResponse removeToken(RemoveTokenRequest request) throws PowerAuthClientException {
        return callV3RestApi("/token/remove", request, RemoveTokenResponse.class);
    }

    @Override
    public RemoveTokenResponse removeToken(String tokenId, String activationId) throws PowerAuthClientException {
        final RemoveTokenRequest request = new RemoveTokenRequest();
        request.setTokenId(tokenId);
        request.setActivationId(activationId);
        return removeToken(request);
    }

    @Override
    public GetEciesDecryptorResponse getEciesDecryptor(GetEciesDecryptorRequest request) throws PowerAuthClientException {
        return callV3RestApi("/ecies/decryptor", request, GetEciesDecryptorResponse.class);
    }

    @Override
    public GetEciesDecryptorResponse getEciesDecryptor(String activationId, String applicationKey, String ephemeralPublicKey) throws PowerAuthClientException {
        final GetEciesDecryptorRequest request = new GetEciesDecryptorRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        return getEciesDecryptor(request);
    }

    @Override
    public StartUpgradeResponse startUpgrade(StartUpgradeRequest request) throws PowerAuthClientException {
        return callV3RestApi("/upgrade/start", request, StartUpgradeResponse.class);
    }

    @Override
    public StartUpgradeResponse startUpgrade(String activationId, String applicationKey, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final StartUpgradeRequest request = new StartUpgradeRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return startUpgrade(request);
    }

    @Override
    public CommitUpgradeResponse commitUpgrade(CommitUpgradeRequest request) throws PowerAuthClientException {
        return callV3RestApi("/upgrade/commit", request, CommitUpgradeResponse.class);
    }

    @Override
    public CommitUpgradeResponse commitUpgrade(String activationId, String applicationKey) throws PowerAuthClientException {
        final CommitUpgradeRequest request = new CommitUpgradeRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        return commitUpgrade(request);
    }

    @Override
    public CreateRecoveryCodeResponse createRecoveryCode(CreateRecoveryCodeRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/create", request, CreateRecoveryCodeResponse.class);
    }

    @Override
    public CreateRecoveryCodeResponse createRecoveryCode(Long applicationId, String userId, Long pukCount) throws PowerAuthClientException {
        final CreateRecoveryCodeRequest request = new CreateRecoveryCodeRequest();
        request.setApplicationId(applicationId);
        request.setUserId(userId);
        request.setPukCount(pukCount);
        return createRecoveryCode(request);
    }

    @Override
    public ConfirmRecoveryCodeResponse confirmRecoveryCode(ConfirmRecoveryCodeRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/confirm", request, ConfirmRecoveryCodeResponse.class);
    }

    @Override
    public ConfirmRecoveryCodeResponse confirmRecoveryCode(String activationId, String applicationKey, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final ConfirmRecoveryCodeRequest request = new ConfirmRecoveryCodeRequest();
        request.setActivationId(activationId);
        request.setApplicationKey(applicationKey);
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return confirmRecoveryCode(request);
    }

    @Override
    public LookupRecoveryCodesResponse lookupRecoveryCodes(LookupRecoveryCodesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/lookup", request, LookupRecoveryCodesResponse.class);
    }

    @Override
    public LookupRecoveryCodesResponse lookupRecoveryCodes(String userId, String activationId, Long applicationId, RecoveryCodeStatus recoveryCodeStatus, RecoveryPukStatus recoveryPukStatus) throws PowerAuthClientException {
        final LookupRecoveryCodesRequest request = new LookupRecoveryCodesRequest();
        request.setUserId(userId);
        request.setActivationId(activationId);
        request.setApplicationId(applicationId);
        request.setRecoveryCodeStatus(recoveryCodeStatus);
        request.setRecoveryPukStatus(recoveryPukStatus);
        return lookupRecoveryCodes(request);
    }

    @Override
    public RevokeRecoveryCodesResponse revokeRecoveryCodes(RevokeRecoveryCodesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/revoke", request, RevokeRecoveryCodesResponse.class);
    }

    @Override
    public RevokeRecoveryCodesResponse revokeRecoveryCodes(List<Long> recoveryCodeIds) throws PowerAuthClientException {
        final RevokeRecoveryCodesRequest request = new RevokeRecoveryCodesRequest();
        request.getRecoveryCodeIds().addAll(recoveryCodeIds);
        return revokeRecoveryCodes(request);
    }

    @Override
    public RecoveryCodeActivationResponse createActivationUsingRecoveryCode(RecoveryCodeActivationRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/recovery/create", request, RecoveryCodeActivationResponse.class);
    }

    @Override
    public RecoveryCodeActivationResponse createActivationUsingRecoveryCode(String recoveryCode, String puk, String applicationKey, Long maxFailureCount, String ephemeralPublicKey, String encryptedData, String mac, String nonce) throws PowerAuthClientException {
        final RecoveryCodeActivationRequest request = new RecoveryCodeActivationRequest();
        request.setRecoveryCode(recoveryCode);
        request.setPuk(puk);
        request.setApplicationKey(applicationKey);
        if (maxFailureCount != null) {
            request.setMaxFailureCount(maxFailureCount);
        }
        request.setEphemeralPublicKey(ephemeralPublicKey);
        request.setEncryptedData(encryptedData);
        request.setMac(mac);
        request.setNonce(nonce);
        return createActivationUsingRecoveryCode(request);
    }

    @Override
    public GetRecoveryConfigResponse getRecoveryConfig(GetRecoveryConfigRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/config/detail", request, GetRecoveryConfigResponse.class);
    }

    @Override
    public GetRecoveryConfigResponse getRecoveryConfig(Long applicationId) throws PowerAuthClientException {
        final GetRecoveryConfigRequest request = new GetRecoveryConfigRequest();
        request.setApplicationId(applicationId);
        return getRecoveryConfig(request);
    }

    @Override
    public UpdateRecoveryConfigResponse updateRecoveryConfig(UpdateRecoveryConfigRequest request) throws PowerAuthClientException {
        return callV3RestApi("/recovery/config/update", request, UpdateRecoveryConfigResponse.class);
    }

    @Override
    public UpdateRecoveryConfigResponse updateRecoveryConfig(Long applicationId, Boolean activationRecoveryEnabled, Boolean recoveryPostcardEnabled, Boolean allowMultipleRecoveryCodes, String remoteRecoveryPublicKeyBase64) throws PowerAuthClientException {
        final UpdateRecoveryConfigRequest request = new UpdateRecoveryConfigRequest();
        request.setApplicationId(applicationId);
        request.setActivationRecoveryEnabled(activationRecoveryEnabled);
        request.setRecoveryPostcardEnabled(recoveryPostcardEnabled);
        request.setAllowMultipleRecoveryCodes(allowMultipleRecoveryCodes);
        request.setRemotePostcardPublicKey(remoteRecoveryPublicKeyBase64);
        return updateRecoveryConfig(request);
    }

    @Override
    public ListActivationFlagsResponse listActivationFlags(ListActivationFlagsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/flags/list", request, ListActivationFlagsResponse.class);
    }

    @Override
    public ListActivationFlagsResponse listActivationFlags(String activationId) throws PowerAuthClientException {
        final ListActivationFlagsRequest request = new ListActivationFlagsRequest();
        request.setActivationId(activationId);
        return listActivationFlags(request);
    }

    @Override
    public AddActivationFlagsResponse addActivationFlags(AddActivationFlagsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/flags/create", request, AddActivationFlagsResponse.class);
    }

    @Override
    public AddActivationFlagsResponse addActivationFlags(String activationId, List<String> activationFlags) throws PowerAuthClientException {
        final AddActivationFlagsRequest request = new AddActivationFlagsRequest();
        request.setActivationId(activationId);
        request.getActivationFlags().addAll(activationFlags);
        return addActivationFlags(request);
    }

    @Override
    public UpdateActivationFlagsResponse updateActivationFlags(UpdateActivationFlagsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/flags/update", request, UpdateActivationFlagsResponse.class);
    }

    @Override
    public UpdateActivationFlagsResponse updateActivationFlags(String activationId, List<String> activationFlags) throws PowerAuthClientException {
        final UpdateActivationFlagsRequest request = new UpdateActivationFlagsRequest();
        request.setActivationId(activationId);
        request.getActivationFlags().addAll(activationFlags);
        return updateActivationFlags(request);
    }

    @Override
    public RemoveActivationFlagsResponse removeActivationFlags(RemoveActivationFlagsRequest request) throws PowerAuthClientException {
        return callV3RestApi("/activation/flags/remove", request, RemoveActivationFlagsResponse.class);
    }

    @Override
    public RemoveActivationFlagsResponse removeActivationFlags(String activationId, List<String> activationFlags) throws PowerAuthClientException {
        final RemoveActivationFlagsRequest request = new RemoveActivationFlagsRequest();
        request.setActivationId(activationId);
        request.getActivationFlags().addAll(activationFlags);
        return removeActivationFlags(request);
    }

    @Override
    public ListApplicationRolesResponse listApplicationRoles(ListApplicationRolesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/roles/list", request, ListApplicationRolesResponse.class);
    }

    @Override
    public ListApplicationRolesResponse listApplicationRoles(Long applicationId) throws PowerAuthClientException {
        final ListApplicationRolesRequest request = new ListApplicationRolesRequest();
        request.setApplicationId(applicationId);
        return listApplicationRoles(request);
    }

    @Override
    public AddApplicationRolesResponse addApplicationRoles(AddApplicationRolesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/roles/create", request, AddApplicationRolesResponse.class);
    }

    @Override
    public AddApplicationRolesResponse addApplicationRoles(Long applicationId, List<String> applicationRoles) throws PowerAuthClientException {
        final AddApplicationRolesRequest request = new AddApplicationRolesRequest();
        request.setApplicationId(applicationId);
        request.getApplicationRoles().addAll(applicationRoles);
        return addApplicationRoles(request);
    }

    @Override
    public UpdateApplicationRolesResponse updateApplicationRoles(UpdateApplicationRolesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/roles/update", request, UpdateApplicationRolesResponse.class);
    }

    @Override
    public UpdateApplicationRolesResponse updateApplicationRoles(Long applicationId, List<String> applicationRoles) throws PowerAuthClientException {
        final UpdateApplicationRolesRequest request = new UpdateApplicationRolesRequest();
        request.setApplicationId(applicationId);
        request.getApplicationRoles().addAll(applicationRoles);
        return updateApplicationRoles(request);
    }

    @Override
    public RemoveApplicationRolesResponse removeApplicationRoles(RemoveApplicationRolesRequest request) throws PowerAuthClientException {
        return callV3RestApi("/application/roles/remove", request, RemoveApplicationRolesResponse.class);
    }

    @Override
    public RemoveApplicationRolesResponse removeApplicationRoles(Long applicationId, List<String> applicationRoles) throws PowerAuthClientException {
        final RemoveApplicationRolesRequest request = new RemoveApplicationRolesRequest();
        request.setApplicationId(applicationId);
        request.getApplicationRoles().addAll(applicationRoles);
        return removeApplicationRoles(request);
    }

    @Override
    public OperationDetailResponse createOperation(OperationCreateRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/create", request, OperationDetailResponse.class);
    }

    @Override
    public OperationDetailResponse operationDetail(OperationDetailRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/detail", request, OperationDetailResponse.class);
    }

    @Override
    public OperationListResponse operationList(OperationListForUserRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/list", request, OperationListResponse.class);
    }

    @Override
    public OperationListResponse operationPendingList(OperationListForUserRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/list/pending", request, OperationListResponse.class);
    }

    @Override
    public OperationDetailResponse operationCancel(OperationCancelRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/cancel", request, OperationDetailResponse.class);
    }

    @Override
    public OperationUserActionResponse operationApprove(OperationApproveRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/approve", request, OperationUserActionResponse.class);
    }

    @Override
    public OperationUserActionResponse failApprovalOperation(OperationFailApprovalRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/approve/fail", request, OperationUserActionResponse.class);
    }

    @Override
    public OperationUserActionResponse operationReject(OperationRejectRequest request) throws PowerAuthClientException {
        return callV3RestApi("/operation/reject", request, OperationUserActionResponse.class);
    }

}
