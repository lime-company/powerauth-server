/*
 * PowerAuth Server and related software components
 * Copyright (C) 2021 Wultra s.r.o.
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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.03.26 at 08:51:41 PM CET 
//


package com.wultra.security.powerauth.client.model.response;

import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidateTokenResponse {

    private boolean tokenValid;
    private String activationId;
    private String userId;
    private Long applicationId;
    private SignatureType signatureType;
    private List<String> applicationRoles = new ArrayList<>();
    private List<String> activationFlags = new ArrayList<>();

}
