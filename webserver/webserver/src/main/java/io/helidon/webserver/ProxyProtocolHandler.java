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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.codec.haproxy.HAProxyTLV;
import io.netty.util.AttributeKey;

/**
 * An inbound handler that accepts an {@link HAProxyMessage} and populates a {@link PeerIdentity} instance from the
 * customer metadata contained in it. Use {@link ProxyProtocolHandler#getIdentity(Channel)} to retrieve the
 * {@link PeerIdentity} if any was created.  This method may return null if the request arrived over a non-PPv2 port.
 * <p>
 * <b>An {@link io.netty.handler.codec.haproxy.HAProxyMessageDecoder} must come immediately before in the
 * channel pipeline</b>.
 * <p>
 * Unsuccessful parses, either in this class or in its immediately preceding
 * {@link io.netty.handler.codec.haproxy.HAProxyMessageDecoder}, throw exceptions that are handled by this class's
 * {@link ProxyProtocolHandler#exceptionCaught(ChannelHandlerContext, Throwable)}
 * <p>
 * Trailing Tag-length-value (TLV) frames with Netty's {@link HAProxyTLV.Type#OTHER} are checked for OCI internal
 * codes that indicate specific kinds of VCN metadata; see {@link OciType}.  A more complete description of the
 * Proxy Protocol V2 wire format can be found in the OciProxyProtocolGenerator test class.
 */
public class ProxyProtocolHandler extends SimpleChannelInboundHandler<HAProxyMessage> {
    private static final Logger LOGGER = Logger.getLogger(HttpInitializer.class.getName());
    private static final AttributeKey<PeerIdentity> ATTRIBUTE_KEY = AttributeKey.valueOf("PPV2_METADATA");

    private static void setIdentity(final Channel ch, PeerIdentity peerIdentity) {
        ch.attr(ATTRIBUTE_KEY).set(peerIdentity);
    }

    /**
     * Retrieve the {@link PeerIdentity} parsed from the Proxy Protocol v2 message sent on this channel, if any.
     * This may return null if the connection did not begin with a Proxy Protocol v2 message (e.g. because it arrived
     * on the non-PPv2 port)
     *
     * @param ch a channel representing a client connection
     * @return the identity of the peer, if any was received
     */
    public static PeerIdentity getIdentity(final Channel ch) {
        return ch.attr(ATTRIBUTE_KEY).get();
    }

    /**
     * A decoder that throws decoding exceptions on byte arrays that do not contain ASCII.
     */
    private final CharsetDecoder asciiDecoder = StandardCharsets.US_ASCII
            .newDecoder()
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .onMalformedInput(CodingErrorAction.REPORT);

    /**
     * Some invalid inputs may cause {@link io.netty.handler.codec.haproxy.HAProxyMessageDecoder} to throw multiple
     * exceptions during decoding. This state variable ensures that failure metrics are only incremented once.
     */
    private boolean alreadyFailed = false;

    public ProxyProtocolHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HAProxyMessage haProxyMessage) {
        HAProxyCommand command = haProxyMessage.command();
        if (command != HAProxyCommand.PROXY) {
            throw new ProxyProtocolException("Invalid command: " + command);
        }

        HAProxyProxiedProtocol proto = haProxyMessage.proxiedProtocol();
        if (proto != HAProxyProxiedProtocol.TCP4 && proto != HAProxyProxiedProtocol.TCP6) {
            throw new ProxyProtocolException("Invalid proxied protocol: " + proto);
        }

        PeerIdentity.Builder peerIdentityBuilder = PeerIdentity.builder()
                .sourceAddress(haProxyMessage.sourceAddress())
                .destAddress(haProxyMessage.destinationAddress())
                .sourcePort(haProxyMessage.sourcePort())
                .destPort(haProxyMessage.destinationPort());

        for (HAProxyTLV tlv : haProxyMessage.tlvs()) {
            if (tlv.type() != HAProxyTLV.Type.OTHER) {
                continue;
            }

            byte literalType = tlv.typeByteValue();
            OciType ociType = OciType.typeForByteValue(literalType);
            // WARNING - Assumes HAProxyMessageDecoder was given a suitably low TLV length limit.
            ByteBuf content = tlv.content();

            switch (ociType) {
                case VCN_CUSTOM -> {
                    peerIdentityBuilder.vcnMetadata(ByteBufUtil.getBytes(content));
                }
                case SGWIP_CUSTOM -> {
                    peerIdentityBuilder.sgwSourceInetAddress(inetAddress(ociType, content));
                }
                case SGWPEIP_CUSTOM -> {
                    peerIdentityBuilder.sgwPeSourceInetAddress(inetAddress(ociType, content));
                }
                case VCN_OCID_CUSTOM -> {
                    peerIdentityBuilder.vcnOcid(asciiString(ociType, content));
                }
                case SGWPE_OCID_CUSTOM -> {
                    peerIdentityBuilder.vcnPeOcid(asciiString(ociType, content));
                }
                case AUTHORITY -> peerIdentityBuilder.authority(asciiString(ociType, content));
                default -> LOGGER.info("Unknown TLV type " + ociType);
            }
        }
        setIdentity(ctx.channel(), peerIdentityBuilder.build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, "Could not parse proxy protocol v2 header", cause);
        if (!alreadyFailed) {
            ctx.close();
            alreadyFailed = true;
        }
    }

    /**
     * Validate that a {@link ByteBuf} contains a valid IP address (IPv4 or IPv6), and return that address as a
     * string suitable for use in logging.
     *
     * @param type the type of metadata this IP was meant to convey; used in error reporting
     * @param content the IP as bytes
     * @return the parsed IP as a string
     */
    private String inetAddress(OciType type, ByteBuf content) {
        byte[] address = ByteBufUtil.getBytes(content);
        try {
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (UnknownHostException e) {
            throw new ProxyProtocolException("could not parse IP address for " + type, e);
        }
    }

    /**
     * Extract an ASCII-encoded string from a {@link ByteBuf}.
     *
     * @param type the type of metadata this IP was meant to convey; used in error reporting
     * @param content the ASCII data
     * @return the decoded string
     */
    private String asciiString(OciType type, ByteBuf content) {
        return asciiString(type, content.nioBuffer());
    }

    /**
     * Extract an ASCII-encoded string from a {@link ByteBuffer}.
     *
     * @param type the type of metadata this IP was meant to convey; used in error reporting
     * @param buffer the ASCII data as an NIO byte buffer
     * @return the decoded string
     */
    private String asciiString(OciType type, ByteBuffer buffer) {
        try {
            return asciiDecoder.decode(buffer).toString();
        } catch (CharacterCodingException e) {
            throw new ProxyProtocolException("failed to decode " + type, e);
        }
    }

    public enum OciType {
        /**
         * The raw VCN metadata.  Useful for debugging
         */
        VCN_CUSTOM,
        /**
         * The Class E address inserted into source address by Service Gateway
         */
        SGWIP_CUSTOM,
        /**
         * The Class E address inserted into source address by Prive Endpoints
         */
        SGWPEIP_CUSTOM,
        /**
         * The OCID that identifies the originating VCN
         */
        VCN_OCID_CUSTOM,
        /**
         * The OCID that identifies the originating private endpoint
         */
        SGWPE_OCID_CUSTOM,
        /**
         * The client's Server Name Indication field, if the load balancer performed TLS termination.
         * Not used by KMS
         */
        AUTHORITY,
        /**
         * A Proxy Protocol V2 TLV type unknown to both Netty and this handler.
         */
        UNKNOWN;

        public static OciType typeForByteValue(byte byteValue) {
            return switch (byteValue) {
                case (byte) 0xE0 -> VCN_CUSTOM;
                case (byte) 0xE1 -> SGWIP_CUSTOM;
                case (byte) 0xE2 -> SGWPEIP_CUSTOM;
                case (byte) 0xE3 -> VCN_OCID_CUSTOM;
                case (byte) 0xE4 -> SGWPE_OCID_CUSTOM;
                case (byte) 0x02 -> AUTHORITY;
                default -> UNKNOWN;
            };
        }

        static byte byteValueForType(OciType type) {
            return switch (type) {
                case VCN_CUSTOM -> (byte) 0xE0;
                case SGWIP_CUSTOM -> (byte) 0xE1;
                case SGWPEIP_CUSTOM -> (byte) 0xE2;
                case VCN_OCID_CUSTOM -> (byte) 0xE3;
                case SGWPE_OCID_CUSTOM -> (byte) 0xE4;
                case AUTHORITY -> (byte) 0x02;
                default -> throw new IllegalArgumentException("Unknown type: " + type);
            };
        }
    }
}
