/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.webserver.netty;

import io.helidon.common.http.BodyPartHeaders;
import java.nio.charset.Charset;

/**
 *
 * @author rgrecour
 */
public class ReadOnlyBodyPartHeaders implements BodyPartHeaders {

    private final String name;
    private final String filename;
    private final String contentType;
    private final String contentTransferEncoding;
    private final Charset charset;
    private final long size;

    public ReadOnlyBodyPartHeaders(final String name, final String filename,
            final String contentType, final String contentTransferEncoding,
            final Charset charset, final long size) {

        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.charset = charset;
        this.size = size;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public String contentTransferEncoding() {
        return contentTransferEncoding;
    }

    @Override
    public Charset charset() {
        return charset;
    }

    @Override
    public long size() {
        return size;
    }
}
