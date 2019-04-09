/*
 * PowerAuth Server and related software components
 * Copyright (C) 2019 Wultra s.r.o.
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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing recovery configuration in the database.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Entity(name = "pa_recovery_config")
public class RecoveryConfigEntity implements Serializable {

    private static final long serialVersionUID = -6333795855186594869L;

    @Id
    @SequenceGenerator(name = "pa_recovery_config", sequenceName = "pa_recovery_config_seq")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "pa_recovery_config")
    @Column(name = "id")
    private Long id;

    @Column(name = "activation_recovery_enabled")
    private Boolean activationRecoveryEnabled;

    @Column(name = "recovery_postcard_enabled")
    private Boolean recoveryPostcardEnabled;

    @Column(name = "recovery_private_key_base64")
    private String recoveryPostcardPrivateKeyBase64;

    @Column(name = "recovery_public_key_base64")
    private String recoveryPostcardPublicKeyBase64;

    @Column(name = "remote_public_key_base64")
    private String remotePostcardPublicKeyBase64;

    @ManyToOne
    @JoinColumn(name = "application_id", referencedColumnName = "id", nullable = false, updatable = false)
    private ApplicationEntity application;

    /**
     * Default constructor
     */
    public RecoveryConfigEntity() {
    }

    /**
     * Constructor with all details.
     * @param id Recovery config ID.
     * @param activationRecoveryEnabled Whether activation recovery is enabled.
     * @param recoveryPostcardEnabled Whether recovery postcard is enabled.
     * @param recoveryPostcardPrivateKeyBase64 Base64 encoded local recovery postcard private key.
     * @param recoveryPostcardPublicKeyBase64 Base64 encoded local recovery postcard public key.
     * @param remotePostcardPublicKeyBase64 Base64 enncoded remote recovery postcard public key.
     */
    public RecoveryConfigEntity(Long id, Boolean activationRecoveryEnabled, Boolean recoveryPostcardEnabled, String recoveryPostcardPrivateKeyBase64, String recoveryPostcardPublicKeyBase64, String remotePostcardPublicKeyBase64) {
        this.id = id;
        this.activationRecoveryEnabled = activationRecoveryEnabled;
        this.recoveryPostcardEnabled = recoveryPostcardEnabled;
        this.recoveryPostcardPrivateKeyBase64 = recoveryPostcardPrivateKeyBase64;
        this.recoveryPostcardPublicKeyBase64 = recoveryPostcardPublicKeyBase64;
        this.remotePostcardPublicKeyBase64 = remotePostcardPublicKeyBase64;
    }

    /**
     * Get master key pair ID
     *
     * @return Master key pair ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set master key pair ID
     *
     * @param id Master key pair ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get whether activation recovery is enabled.
     * @return Whether activation recovery is enabled.
     */
    public Boolean getActivationRecoveryEnabled() {
        return activationRecoveryEnabled;
    }

    /**
     * Set whether activation recovery is enabled.
     * @param activationRecoveryEnabled Whether activation recoveyr is enabled.
     */
    public void setActivationRecoveryEnabled(Boolean activationRecoveryEnabled) {
        this.activationRecoveryEnabled = activationRecoveryEnabled;
    }

    /**
     * Get whether recovery postcard is enabled.
     * @return Whether recovery postcard is enabled.
     */
    public Boolean getRecoveryPostcardEnabled() {
        return recoveryPostcardEnabled;
    }

    /**
     * Set whether recovery postcard is enabled.
     * @param recoveryPostcardEnabled Whether recovery postcard is enabled.
     */
    public void setRecoveryPostcardEnabled(Boolean recoveryPostcardEnabled) {
        this.recoveryPostcardEnabled = recoveryPostcardEnabled;
    }

    /**
     * Get Base64 encoded local recovery postcard private key.
     * @return Base64 encoded local recovery postcard private key.
     */
    public String getRecoveryPostcardPrivateKeyBase64() {
        return recoveryPostcardPrivateKeyBase64;
    }

    /**
     * Set Base64 encoded local recovery postcard private key.
     * @param recoveryPostcardPrivateKeyBase64 Base64 encoded local recovery postcard private key.
     */
    public void setRecoveryPostcardPrivateKeyBase64(String recoveryPostcardPrivateKeyBase64) {
        this.recoveryPostcardPrivateKeyBase64 = recoveryPostcardPrivateKeyBase64;
    }

    /**
     * Get Base64 encoded local recovery postcard public key.
     * @return Base64 encoded local recovery postcard public key.
     */
    public String getRecoveryPostcardPublicKeyBase64() {
        return recoveryPostcardPublicKeyBase64;
    }

    /**
     * Set Base64 encoded local recovery postcard public key.
     * @param recoveryPostcardPublicKeyBase64 Base64 encoded local recovery postcard public key.
     */
    public void setRecoveryPostcardPublicKeyBase64(String recoveryPostcardPublicKeyBase64) {
        this.recoveryPostcardPublicKeyBase64 = recoveryPostcardPublicKeyBase64;
    }

    /**
     *  Get Base64 encoded remote recovery postcard public key.
     * @return Base64 encoded remote recovery postcard public key.
     */
    public String getRemotePostcardPublicKeyBase64() {
        return remotePostcardPublicKeyBase64;
    }

    /**
     * Set Base64 encoded remote recovery postcard public key.
     * @param remotePostcardPublicKeyBase64 Base64 encoded remote recovery postcard public key.
     */
    public void setRemotePostcardPublicKeyBase64(String remotePostcardPublicKeyBase64) {
        this.remotePostcardPublicKeyBase64 = remotePostcardPublicKeyBase64;
    }

    /**
     * Get associated application.
     *
     * @return Associated application
     */
    public ApplicationEntity getApplication() {
        return application;
    }

    /**
     * Set associated application.
     *
     * @param application Associated application
     */
    public void setApplication(ApplicationEntity application) {
        this.application = application;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.activationRecoveryEnabled);
        hash = 37 * hash + Objects.hashCode(this.recoveryPostcardEnabled);
        hash = 37 * hash + Objects.hashCode(this.recoveryPostcardPrivateKeyBase64);
        hash = 37 * hash + Objects.hashCode(this.recoveryPostcardPublicKeyBase64);
        hash = 37 * hash + Objects.hashCode(this.remotePostcardPublicKeyBase64);
        hash = 37 * hash + Objects.hashCode(this.application);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RecoveryConfigEntity other = (RecoveryConfigEntity) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.activationRecoveryEnabled, other.activationRecoveryEnabled)) {
            return false;
        }
        if (!Objects.equals(this.recoveryPostcardEnabled, other.recoveryPostcardEnabled)) {
            return false;
        }
        if (!Objects.equals(this.recoveryPostcardPrivateKeyBase64, other.recoveryPostcardPrivateKeyBase64)) {
            return false;
        }
        if (!Objects.equals(this.recoveryPostcardPublicKeyBase64, other.recoveryPostcardPublicKeyBase64)) {
            return false;
        }
        if (!Objects.equals(this.remotePostcardPublicKeyBase64, other.remotePostcardPublicKeyBase64)) {
            return false;
        }
        return Objects.equals(this.application, other.application);
    }

    @Override
    public String toString() {
        return "RecoveryConfigEntity{"
                + "id=" + id
                + ", activationRecoveryEnabled=" + activationRecoveryEnabled
                + ", recoveryPostcardEnabled=" + recoveryPostcardEnabled
                + ", recoveryPostcardPublicKeyBase64=" + recoveryPostcardPublicKeyBase64
                + ", remotePostcardPublicKeyBase64=" + remotePostcardPublicKeyBase64
                + ", application=" + application.getId()
                + '}';
    }

}