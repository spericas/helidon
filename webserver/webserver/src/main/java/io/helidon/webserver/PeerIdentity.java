/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.webserver;

/**
 * The identity of a peer, usable for audit logging and IAM policies.
 */
class PeerIdentity {

    /**
     * The client's source address.
     */
    private final String sourceAddress;

    /**
     * The address by which the client accessed KMS.
     */
    private final String destAddress;

    /**
     * The client's (likely ephemeral) source port.
     */
    private final int sourcePort;

    /**
     * The port by which the client accessed KMS (likely 443).
     */
    private final int destPort;

    /**
     * The raw metadata that describes the client's VCN, if any.
     */
    private final byte[] vcnMetadata;

    /**
     * The Class E source address assigned by SGW, if any.
     */
    private final String sgwSourceInetAddress;

    /**
     * The Class E source address assigned by SGW PE (private endpoints), if any.
     */
    private final String sgwPeSourceInetAddress;

    /**
     * The OCID of the VCN from which the client accessed KMS, if any.
     */
    private final String vcnOcid;

    /**
     * The OCID of the private endpoint through which the customer accessed KMS, if any.
     */
    private final String vcnPeOcid;

    /**
     * The name in the client's SNI extension, if the load balancer terminated TLS.
     */
    private final String authority;

    private PeerIdentity(Builder builder) {
        this.sourceAddress = builder.sourceAddress;
        this.destAddress = builder.destAddress;
        this.sourcePort = builder.sourcePort;
        this.destPort = builder.destPort;
        this.vcnMetadata = builder.vcnMetadata;
        this.sgwSourceInetAddress = builder.sgwSourceInetAddress;
        this.sgwPeSourceInetAddress = builder.sgwPeSourceInetAddress;
        this.vcnOcid = builder.vcnOcid;
        this.vcnPeOcid = builder.vcnPeOcid;
        this.authority = builder.authority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String sourceAddress() {
        return sourceAddress;
    }

    public String destAddress() {
        return destAddress;
    }

    public int sourcePort() {
        return sourcePort;
    }

    public int destPort() {
        return destPort;
    }

    public byte[] vcnMetadata() {
        return vcnMetadata;
    }

    public String sgwSourceInetAddress() {
        return sgwSourceInetAddress;
    }

    public String sgwPeSourceInetAddress() {
        return sgwPeSourceInetAddress;
    }

    public String vcnOcid() {
        return vcnOcid;
    }

    public String vcnPeOcid() {
        return vcnPeOcid;
    }

    public String authority() {
        return authority;
    }

    public String forwardedFor() {
        StringBuilder sb = new StringBuilder();
        if (sourceAddress != null) {
            sb.append(sourceAddress);
        }
        if (sourcePort != 0) {
            sb.append(":").append(sourcePort);
        }
        return sb.toString();
    }

    static class Builder implements io.helidon.common.Builder<PeerIdentity.Builder, PeerIdentity> {

        private String sourceAddress;
        private String destAddress;
        private int sourcePort;
        private int destPort;
        private byte[] vcnMetadata;
        private String sgwSourceInetAddress;
        private String sgwPeSourceInetAddress;
        private String vcnOcid;
        private String vcnPeOcid;
        private String authority;

        public Builder sourceAddress(String sourceAddress) {
            this.sourceAddress = sourceAddress;
            return this;
        }

        public Builder destAddress(String destAddress) {
            this.destAddress = destAddress;
            return this;
        }

        public Builder sourcePort(int sourcePort) {
            this.sourcePort = sourcePort;
            return this;
        }

        public Builder destPort(int destPort) {
            this.destPort = destPort;
            return this;
        }

        public Builder vcnMetadata(byte[] vcnMetadata) {
            this.vcnMetadata = vcnMetadata;
            return this;
        }

        public Builder sgwSourceInetAddress(String sgwSourceInetAddress) {
            this.sgwSourceInetAddress = sgwSourceInetAddress;
            return this;
        }

        public Builder sgwPeSourceInetAddress(String sgwPeSourceInetAddress) {
            this.sgwPeSourceInetAddress = sgwPeSourceInetAddress;
            return this;
        }

        public Builder vcnOcid(String vcnOcid) {
            this.vcnOcid = vcnOcid;
            return this;
        }

        public Builder vcnPeOcid(String vcnPeOcid) {
            this.vcnPeOcid = vcnPeOcid;
            return this;
        }

        public Builder authority(String authority) {
            this.authority = authority;
            return this;
        }

        @Override
        public PeerIdentity build() {
            return new PeerIdentity(this);
        }
    }
}