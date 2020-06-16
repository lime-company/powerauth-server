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
package io.getlime.security.powerauth.app.server.service.behavior.tasks.v3;

import io.getlime.security.powerauth.app.server.database.RepositoryCatalogue;
import io.getlime.security.powerauth.app.server.database.model.entity.ApplicationEntity;
import io.getlime.security.powerauth.app.server.database.repository.ApplicationRepository;
import io.getlime.security.powerauth.app.server.service.exceptions.GenericServiceException;
import io.getlime.security.powerauth.app.server.service.i18n.LocalizationProvider;
import io.getlime.security.powerauth.app.server.service.model.ServiceError;
import io.getlime.security.powerauth.v3.AddApplicationRolesResponse;
import io.getlime.security.powerauth.v3.ListApplicationRolesResponse;
import io.getlime.security.powerauth.v3.RemoveApplicationRolesResponse;
import io.getlime.security.powerauth.v3.UpdateApplicationRolesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Behavior class implementing management of application roles.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Component("applicationRolesServiceBehavior")
public class ApplicationRolesServiceBehavior {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRolesServiceBehavior.class);

    private final RepositoryCatalogue repositoryCatalogue;
    private final LocalizationProvider localizationProvider;

    @Autowired
    public ApplicationRolesServiceBehavior(RepositoryCatalogue repositoryCatalogue, LocalizationProvider localizationProvider) {
        this.repositoryCatalogue = repositoryCatalogue;
        this.localizationProvider = localizationProvider;
    }

    /**
     * List application roles.
     * @param applicationId Application ID.
     * @return List application roles response.
     * @throws GenericServiceException In case of a business logic error.
     */
    public ListApplicationRolesResponse listApplicationRoles(long applicationId) throws GenericServiceException {
        if (applicationId <= 0) {
            logger.warn("Invalid application ID in listApplicationRoles");
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_REQUEST);
        }
        final ListApplicationRolesResponse response = new ListApplicationRolesResponse();
        Optional<ApplicationEntity> applicationOptional = repositoryCatalogue.getApplicationRepository().findById(applicationId);
        if (!applicationOptional.isPresent()) {
            logger.info("Application not found, application ID: {}", applicationId);
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_APPLICATION);
        }
        ApplicationEntity application = applicationOptional.get();
        response.getApplicationRoles().addAll(application.getRoles());
        return response;
    }

    /**
     * Add application roles.
     * @param applicationId Application ID.
     * @param applicationRoles Application roles.
     * @return Add application roles response.
     * @throws GenericServiceException In case of a business logic error.
     */
    public AddApplicationRolesResponse addApplicationRoles(long applicationId, List<String> applicationRoles) throws GenericServiceException {
        if (applicationId <= 0) {
            logger.warn("Invalid application ID in addApplicationRoles");
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_REQUEST);
        }
        final AddApplicationRolesResponse response = new AddApplicationRolesResponse();
        final ApplicationRepository applicationRepository = repositoryCatalogue.getApplicationRepository();
        response.setApplicationId(applicationId);
        Optional<ApplicationEntity> applicationOptional =  applicationRepository.findById(applicationId);
        if (!applicationOptional.isPresent()) {
            logger.info("Application not found, application ID: {}", applicationId);
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_APPLICATION);
        }
        ApplicationEntity application = applicationOptional.get();
        List<String> currentRoles = application.getRoles();
        List<String> newFlags = applicationRoles.stream().filter(flag -> !currentRoles.contains(flag)).collect(Collectors.toList());
        application.getRoles().addAll(newFlags);
        applicationRepository.save(application);
        response.getApplicationRoles().addAll(application.getRoles());
        return response;
    }

    /**
     * Update application roles.
     * @param applicationId Application ID.
     * @param applicationRoles Application roles.
     * @return Update application roles response.
     * @throws GenericServiceException In case of a business logic error.
     */
    public UpdateApplicationRolesResponse updateApplicationRoles(long applicationId, List<String> applicationRoles) throws GenericServiceException {
        if (applicationId <= 0) {
            logger.warn("Invalid application ID in updateApplicationRoles");
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_REQUEST);
        }
        final UpdateApplicationRolesResponse response = new UpdateApplicationRolesResponse();
        final ApplicationRepository applicationRepository = repositoryCatalogue.getApplicationRepository();
        response.setApplicationId(applicationId);
        Optional<ApplicationEntity> applicationOptional =  applicationRepository.findById(applicationId);
        if (!applicationOptional.isPresent()) {
            logger.info("Application not found, application ID: {}", applicationId);
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_APPLICATION);
        }
        ApplicationEntity application = applicationOptional.get();
        application.getRoles().clear();
        application.getRoles().addAll(applicationRoles);
        applicationRepository.save(application);
        response.getApplicationRoles().addAll(applicationRoles);
        return response;
    }

    /**
     * Delete application roles.
     * @param applicationId Application ID.
     * @param applicationRoles Application roles.
     * @return Delete application roles response.
     * @throws GenericServiceException In case of a business logic error.
     */
    public RemoveApplicationRolesResponse removeApplicationRoles(long applicationId, List<String> applicationRoles) throws GenericServiceException {
        if (applicationId <= 0) {
            logger.warn("Invalid application ID in removeApplicationRoles");
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_REQUEST);
        }
        final RemoveApplicationRolesResponse response = new RemoveApplicationRolesResponse();
        final ApplicationRepository applicationRepository = repositoryCatalogue.getApplicationRepository();
        response.setApplicationId(applicationId);
        Optional<ApplicationEntity> applicationOptional =  applicationRepository.findById(applicationId);
        if (!applicationOptional.isPresent()) {
            logger.info("Application not found, application ID: {}", applicationId);
            // Rollback is not required, error occurs before writing to database
            throw localizationProvider.buildExceptionForCode(ServiceError.INVALID_APPLICATION);
        }
        ApplicationEntity application = applicationOptional.get();
        application.getRoles().removeAll(applicationRoles);
        applicationRepository.save(application);
        response.getApplicationRoles().addAll(application.getRoles());
        return response;
    }

}
