/*
 * PowerAuth Server and related software components
 * Copyright (C) 2018 Wultra s.r.o.
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
package io.getlime.security.powerauth.app.server.database.model.entity;

import io.getlime.security.powerauth.app.server.converter.CallbackAttributeVOConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

/**
 * Class representing a callback URL associated with given application.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Entity(name = "pa_application_callback")
public class CallbackUrlEntity implements Serializable {

    private static final long serialVersionUID = 3372029113954119581L;

    @Id
    @Column(name = "id", updatable = false, length = 37)
    private String id;

    @Column(name = "application_id", updatable = false, nullable = false)
    private Long applicationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "callback_url", nullable = false)
    private String callbackUrl;

    @Column(name = "attributes")
    @Convert(converter = CallbackAttributeVOConverter.class)
    private List<String> attributes;

    /**
     * Get the ID of an integration.
     * @return ID of an integration.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the ID of an integration.
     * @param id ID of an integration.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the application ID.
     * @return Application ID.
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * Set application ID.
     * @param applicationId Application ID.
     */
    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Get the name of an integration.
     * @return Name of an integration.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of an integration.
     * @param name Name of an integration.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get callback URL string.
     * @return Callback URL string.
     */
    public String getCallbackUrl() {
        return callbackUrl;
    }

    /**
     * Set callback URL string.
     * @param callbackUrl Callback URL string.
     */
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    /**
     * Get callback attribute settings.
     * @return Callback attribute settings.
     */
    public List<String> getAttributes() {
        return attributes;
    }

    /**
     * Set callback attribute settings.
     * @param attributes Callback attribute settings.
     */
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
}
