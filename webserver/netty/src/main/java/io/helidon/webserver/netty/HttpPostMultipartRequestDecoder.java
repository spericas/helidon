/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.webserver.netty;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.InternalThreadLocalMap;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.netty.buffer.Unpooled.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import static io.netty.util.internal.ObjectUtil.*;
import java.io.Serializable;
import java.util.Comparator;

/**
 * This decoder will decode Body and can handle POST BODY.
 *
 * You <strong>MUST</strong> call {@link #destroy()} after completion to release all resources.
 *
 */
public class HttpPostMultipartRequestDecoder {

    /**
     * Default Content-Type in binary form.
     */
    public static final String DEFAULT_BINARY_CONTENT_TYPE = "application/octet-stream";

    /**
     * Request to decode
     */
    private final HttpRequest request;

    /**
     * Default charset to use
     */
    private Charset charset;

    /**
     * Does the last chunk already received
     */
    private boolean isLastChunk;

    /**
     * HttpDatas from Body
     */
    private final List<InterfaceHttpData> bodyListHttpData = new ArrayList<InterfaceHttpData>();

    /**
     * HttpDatas as Map from Body
     */
    private final Map<String, List<InterfaceHttpData>> bodyMapHttpData = new TreeMap<String, List<InterfaceHttpData>>(
            CaseIgnoringComparator.INSTANCE);

    /**
     * The current channelBuffer
     */
    private ByteBuf undecodedChunk;

    /**
     * Body HttpDatas current position
     */
    private int bodyListHttpDataRank;

    /**
     * If multipart, this is the boundary for the global multipart
     */
    private String multipartDataBoundary;

    /**
     * If multipart, there could be internal multiparts (mixed) to the global
     * multipart. Only one level is allowed.
     */
    private String multipartMixedBoundary;

    /**
     * Current getStatus
     */
    private MultiPartStatus currentStatus = MultiPartStatus.NOTSTARTED;

    /**
     * Used in Multipart
     */
    private Map<CharSequence, Attribute> currentFieldAttributes;

    /**
     * The current FileUpload that is currently in decode process
     */
    private FileUpload currentFileUpload;

    /**
     * The current Attribute that is currently in decode process
     */
    private Attribute currentAttribute;

    private boolean destroyed;

/**
     * Multipart attribute.
     */
    public static class Attribute implements HttpData {

        private final String name;
        private long size;
        private final Charset charset;
        private String value;
        private ByteBuf buf = buffer(64);
        private boolean completed = false;

        public Attribute(final String name, final long size, final Charset charset) {
            this.name = name;
            this.size = size;
            this.charset = charset;
        }

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
            this.size = value.getBytes().length;
            this.charset = null;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }

        private void setValue(byte[] array) {
            Charset c = this.charset == null ? Charset.defaultCharset() : this.charset;
            this.value = new String(array, c);
            if (this.size == 0) {
                this.size = array.length;
            }
        }

        @Override
        public boolean release() {
            return true;
        }

        @Override
        public void addContent(ByteBuf buffer, boolean last) throws IOException {
            buf.writeBytes(buffer);
            completed = last;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public long length() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long definedLength() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf getByteBuf() throws IOException {
            return buf;
        }

        @Override
        public String getString() throws IOException {
            return buf.toString(Charset.defaultCharset());
        }

        @Override
        public String getString(Charset encoding) throws IOException {
            return buf.toString(encoding);
        }
    }

    /**
     * states follow NOTSTARTED PREAMBLE ( (HEADERDELIMITER DISPOSITION (FIELD |
     * FILEUPLOAD))* (HEADERDELIMITER DISPOSITION MIXEDPREAMBLE (MIXEDDELIMITER
     * MIXEDDISPOSITION MIXEDFILEUPLOAD)+ MIXEDCLOSEDELIMITER)* CLOSEDELIMITER)+
     * EPILOGUE
     *
     * First getStatus is: NOSTARTED
     *
     * Content-type: multipart/form-data, boundary=AaB03x => PREAMBLE in Header
     *
     * --AaB03x => HEADERDELIMITER content-disposition: form-data; name="field1"
     * => DISPOSITION
     *
     * Joe Blow => FIELD --AaB03x => HEADERDELIMITER content-disposition:
     * form-data; name="pics" => DISPOSITION Content-type: multipart/mixed,
     * boundary=BbC04y
     *
     * --BbC04y => MIXEDDELIMITER Content-disposition: attachment;
     * filename="file1.txt" => MIXEDDISPOSITION Content-Type: text/plain
     *
     * ... contents of file1.txt ... => MIXEDFILEUPLOAD --BbC04y =>
     * MIXEDDELIMITER Content-disposition: file; filename="file2.gif" =>
     * MIXEDDISPOSITION Content-type: image/gif Content-Transfer-Encoding:
     * binary
     *
     * ...contents of file2.gif... => MIXEDFILEUPLOAD --BbC04y-- =>
     * MIXEDCLOSEDELIMITER --AaB03x-- => CLOSEDELIMITER
     *
     * Once CLOSEDELIMITER is found, last getStatus is EPILOGUE
     */
    protected enum MultiPartStatus {
        NOTSTARTED, PREAMBLE, HEADERDELIMITER, DISPOSITION, FIELD, FILEUPLOAD, MIXEDPREAMBLE, MIXEDDELIMITER,
        MIXEDDISPOSITION, MIXEDFILEUPLOAD, MIXEDCLOSEDELIMITER, CLOSEDELIMITER, PREEPILOGUE, EPILOGUE
    }

    /**
     * Check if the given request is a multipart request
     * @param request
     * @return True if the request is a Multipart request
     */
    public static boolean isMultipart(HttpRequest request) {
        if (request.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            return getMultipartDataBoundary(request.headers().get(HttpHeaderNames.CONTENT_TYPE)) != null;
        } else {
            return false;
        }
    }

/**
     * Check from the request ContentType if this request is a Multipart request.
     * @param contentType
     * @return an array of String if multipartDataBoundary exists with the multipartDataBoundary
     * as first element, charset if any as second (missing if not set), else null
     */
    protected static String[] getMultipartDataBoundary(String contentType) {
        // Check if Post using "multipart/form-data; boundary=--89421926422648 [; charset=xxx]"
        String[] headerContentType = splitHeaderContentType(contentType);
        final String multiPartHeader = HttpHeaderValues.MULTIPART_FORM_DATA.toString();
        if (headerContentType[0].regionMatches(true, 0, multiPartHeader, 0 , multiPartHeader.length())) {
            int mrank;
            int crank;
            final String boundaryHeader = HttpHeaderValues.BOUNDARY.toString();
            if (headerContentType[1].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 1;
                crank = 2;
            } else if (headerContentType[2].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 2;
                crank = 1;
            } else {
                return null;
            }
            String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
            if (boundary == null) {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Needs a boundary value");
            }
            if (boundary.charAt(0) == '"') {
                String bound = boundary.trim();
                int index = bound.length() - 1;
                if (bound.charAt(index) == '"') {
                    boundary = bound.substring(1, index);
                }
            }
            final String charsetHeader = HttpHeaderValues.CHARSET.toString();
            if (headerContentType[crank].regionMatches(true, 0, charsetHeader, 0, charsetHeader.length())) {
                String charset = StringUtil.substringAfter(headerContentType[crank], '=');
                if (charset != null) {
                    return new String[] {"--" + boundary, charset};
                }
            }
            return new String[] {"--" + boundary};
        }
        return null;
    }

    /**
     *
     * @param request
     *            the request to decode
     * @throws NullPointerException
     *             for request
     */
    public HttpPostMultipartRequestDecoder(HttpRequest request) {
        this.request = checkNotNull(request, "request");
        this.charset = HttpConstants.DEFAULT_CHARSET;
        // Fill default values

        setMultipart(this.request.headers().get(HttpHeaderNames.CONTENT_TYPE));
        if (request instanceof HttpContent) {
            // Offer automatically if the given request is als type of HttpContent
            // See #1089
            offer((HttpContent) request);
        } else {
            undecodedChunk = buffer();
            parseBody();
        }
    }

    /**
     * Set from the request ContentType the multipartDataBoundary and the possible charset.
     */
    private void setMultipart(String contentType) {
        String[] dataBoundary = getMultipartDataBoundary(contentType);
        if (dataBoundary != null) {
            multipartDataBoundary = dataBoundary[0];
            if (dataBoundary.length > 1 && dataBoundary[1] != null) {
                charset = Charset.forName(dataBoundary[1]);
            }
        } else {
            multipartDataBoundary = null;
        }
        currentStatus = MultiPartStatus.HEADERDELIMITER;
    }

    private void checkDestroyed() {
        if (destroyed) {
            throw new IllegalStateException(HttpPostMultipartRequestDecoder.class.getSimpleName()
                    + " was destroyed already");
        }
    }

    /**
     * Initialized the internals from a new chunk
     *
     * @param content
     *            the new received chunk
     * @return 
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    public HttpPostMultipartRequestDecoder offer(HttpContent content) {
        checkDestroyed();

        // Maybe we should better not copy here for performance reasons but this will need
        // more care by the caller to release the content in a correct manner later
        // So maybe something to optimize on a later stage
        ByteBuf buf = content.content();
        if (undecodedChunk == null) {
            undecodedChunk = buf.copy();
        } else {
            undecodedChunk.writeBytes(buf);
        }
        if (content instanceof LastHttpContent) {
            isLastChunk = true;
        }
        parseBody();
        return this;
    }

    /**
     * True if at current getStatus, there is an available decoded
     * InterfaceHttpData from the Body.
     *
     * This getMethod works for chunked and not chunked request.
     *
     * @return True if at current getStatus, there is a decoded InterfaceHttpData
     * @throws EndOfDataDecoderException
     *             No more data will be available
     */
    public boolean hasNext() {
        checkDestroyed();

        if (currentStatus == MultiPartStatus.EPILOGUE) {
            // OK except if end of list
            if (bodyListHttpDataRank >= bodyListHttpData.size()) {
                throw new EndOfDataDecoderException();
            }
        }
        return !bodyListHttpData.isEmpty() && bodyListHttpDataRank < bodyListHttpData.size();
    }

    /**
     * Returns the next available InterfaceHttpData or null if, at the time it
     * is called, there is no more available InterfaceHttpData. A subsequent
     * call to offer(httpChunk) could enable more data.
     *
     * Be sure to call {@link InterfaceHttpData#release()} after you are done
     * with processing to make sure to not leak any resources
     *
     * @return the next available InterfaceHttpData or null if none
     * @throws EndOfDataDecoderException
     *             No more data will be available
     */
    public InterfaceHttpData next() {
        checkDestroyed();

        if (hasNext()) {
            return bodyListHttpData.get(bodyListHttpDataRank++);
        }
        return null;
    }

    public InterfaceHttpData currentPartialHttpData() {
        if (currentFileUpload != null) {
            return currentFileUpload;
        } else {
            return currentAttribute;
        }
    }

    /**
     * This getMethod will parse as much as possible data and fill the list and map
     *
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    private void parseBody() {
        if (currentStatus == MultiPartStatus.PREEPILOGUE || currentStatus == MultiPartStatus.EPILOGUE) {
            if (isLastChunk) {
                currentStatus = MultiPartStatus.EPILOGUE;
            }
            return;
        }
        parseBodyMultipart();
    }

    /**
     * Utility function to add a new decoded data
     */
    protected void addHttpData(InterfaceHttpData data) {
        if (data == null) {
            return;
        }
        List<InterfaceHttpData> datas = bodyMapHttpData.get(data.getName());
        if (datas == null) {
            datas = new ArrayList<InterfaceHttpData>(1);
            bodyMapHttpData.put(data.getName(), datas);
        }
        datas.add(data);
        bodyListHttpData.add(data);
    }

    /**
     * Parse the Body for multipart
     *
     * @throws ErrorDataDecoderException
     *             if there is a problem with the charset decoding or other
     *             errors
     */
    private void parseBodyMultipart() {
        if (undecodedChunk == null || undecodedChunk.readableBytes() == 0) {
            // nothing to decode
            return;
        }
        InterfaceHttpData data = decodeMultipart(currentStatus);
        while (data != null) {
            addHttpData(data);
            if (currentStatus == MultiPartStatus.PREEPILOGUE || currentStatus == MultiPartStatus.EPILOGUE) {
                break;
            }
            data = decodeMultipart(currentStatus);
        }
    }

    /**
     * Decode a multipart request by pieces<br>
     * <br>
     * NOTSTARTED PREAMBLE (<br>
     * (HEADERDELIMITER DISPOSITION (FIELD | FILEUPLOAD))*<br>
     * (HEADERDELIMITER DISPOSITION MIXEDPREAMBLE<br>
     * (MIXEDDELIMITER MIXEDDISPOSITION MIXEDFILEUPLOAD)+<br>
     * MIXEDCLOSEDELIMITER)*<br>
     * CLOSEDELIMITER)+ EPILOGUE<br>
     *
     * Inspired from HttpMessageDecoder
     *
     * @return the next decoded InterfaceHttpData or null if none until now.
     * @throws ErrorDataDecoderException
     *             if an error occurs
     */
    private InterfaceHttpData decodeMultipart(MultiPartStatus state) {
        switch (state) {
        case NOTSTARTED:
            throw new ErrorDataDecoderException("Should not be called with the current getStatus");
        case PREAMBLE:
            // Content-type: multipart/form-data, boundary=AaB03x
            throw new ErrorDataDecoderException("Should not be called with the current getStatus");
        case HEADERDELIMITER: {
            // --AaB03x or --AaB03x--
            return findMultipartDelimiter(multipartDataBoundary, MultiPartStatus.DISPOSITION,
                    MultiPartStatus.PREEPILOGUE);
        }
        case DISPOSITION: {
            // content-disposition: form-data; name="field1"
            // content-disposition: form-data; name="pics"; filename="file1.txt"
            // and other immediate values like
            // Content-type: image/gif
            // Content-Type: text/plain
            // Content-Type: text/plain; charset=ISO-8859-1
            // Content-Transfer-Encoding: binary
            // The following line implies a change of mode (mixed mode)
            // Content-type: multipart/mixed, boundary=BbC04y
            return findMultipartDisposition();
        }
        case FIELD: {
            // Now get value according to Content-Type and Charset
            Charset localCharset = null;
            Attribute charsetAttribute = currentFieldAttributes.get(HttpHeaderValues.CHARSET);
            if (charsetAttribute != null) {
                try {
                    localCharset = Charset.forName(charsetAttribute.getValue());
                } catch (UnsupportedCharsetException e) {
                    throw new ErrorDataDecoderException(e);
                }
            }
            Attribute nameAttribute = currentFieldAttributes.get(HttpHeaderValues.NAME);
            if (currentAttribute == null) {
                Attribute lengthAttribute = currentFieldAttributes
                        .get(HttpHeaderNames.CONTENT_LENGTH);
                long size;
                try {
                    size = lengthAttribute != null? Long.parseLong(lengthAttribute
                            .getValue()) : 0L;
                } catch (NumberFormatException ignored) {
                    size = 0;
                }
                try {
                    if (size > 0) {
                        currentAttribute = new Attribute(
                            cleanString(nameAttribute.getValue()), size,
                            localCharset);
                        // XXX
//                    } else {
//                        currentAttribute = new Attribute(request,
//                                cleanString(nameAttribute.getValue()));
                    }
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                }
//                if (localCharset != null) {
//                    currentAttribute.setCharset(localCharset);
//                }
            }
            // load data
            if (!loadDataMultipart(undecodedChunk, multipartDataBoundary, currentAttribute)) {
                // Delimiter is not found. Need more chunks.
                // TODO decode partial
                return null;
            }
            Attribute finalAttribute = currentAttribute;
            currentAttribute = null;
            currentFieldAttributes = null;
            // ready to load the next one
            currentStatus = MultiPartStatus.HEADERDELIMITER;
            return finalAttribute;
        }
        case FILEUPLOAD: {
            // eventually restart from existing FileUpload
            return getFileUpload(multipartDataBoundary);
        }
        case MIXEDDELIMITER: {
            // --AaB03x or --AaB03x--
            // Note that currentFieldAttributes exists
            return findMultipartDelimiter(multipartMixedBoundary, MultiPartStatus.MIXEDDISPOSITION,
                    MultiPartStatus.HEADERDELIMITER);
        }
        case MIXEDDISPOSITION: {
            return findMultipartDisposition();
        }
        case MIXEDFILEUPLOAD: {
            // eventually restart from existing FileUpload
            return getFileUpload(multipartMixedBoundary);
        }
        case PREEPILOGUE:
            return null;
        case EPILOGUE:
            return null;
        default:
            throw new ErrorDataDecoderException("Shouldn't reach here.");
        }
    }

    /**
     * Skip control Characters
     *
     * @throws NotEnoughDataDecoderException
     */
    private static void skipControlCharacters(ByteBuf undecodedChunk) {
        if (!undecodedChunk.hasArray()) {
            try {
                skipControlCharactersStandard(undecodedChunk);
            } catch (IndexOutOfBoundsException e1) {
                throw new NotEnoughDataDecoderException(e1);
            }
            return;
        }
        SeekAheadOptimize sao = new SeekAheadOptimize(undecodedChunk);
        while (sao.pos < sao.limit) {
            char c = (char) (sao.bytes[sao.pos++] & 0xFF);
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                sao.setReadPosition(1);
                return;
            }
        }
        throw new NotEnoughDataDecoderException("Access out of bounds");
    }

    private static void skipControlCharactersStandard(ByteBuf undecodedChunk) {
        for (;;) {
            char c = (char) undecodedChunk.readUnsignedByte();
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                break;
            }
        }
    }

    /**
     * Find the next Multipart Delimiter
     *
     * @param delimiter
     *            delimiter to find
     * @param dispositionStatus
     *            the next getStatus if the delimiter is a start
     * @param closeDelimiterStatus
     *            the next getStatus if the delimiter is a close delimiter
     * @return the next InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    private InterfaceHttpData findMultipartDelimiter(String delimiter, MultiPartStatus dispositionStatus,
            MultiPartStatus closeDelimiterStatus) {
        // --AaB03x or --AaB03x--
        int readerIndex = undecodedChunk.readerIndex();
        try {
            skipControlCharacters(undecodedChunk);
        } catch (NotEnoughDataDecoderException ignored) {
            undecodedChunk.readerIndex(readerIndex);
            // TODO return partial data
            return null;
        }
        skipOneLine();
        String newline;
        try {
            newline = readDelimiter(undecodedChunk, delimiter);
        } catch (NotEnoughDataDecoderException ignored) {
            undecodedChunk.readerIndex(readerIndex);
            // TODO return partial data
            return null;
        }
        if (newline.equals(delimiter)) {
            currentStatus = dispositionStatus;
            return decodeMultipart(dispositionStatus);
        }
        if (newline.equals(delimiter + "--")) {
            // CLOSEDELIMITER or MIXED CLOSEDELIMITER found
            currentStatus = closeDelimiterStatus;
            if (currentStatus == MultiPartStatus.HEADERDELIMITER) {
                // MIXEDCLOSEDELIMITER
                // end of the Mixed part
                currentFieldAttributes = null;
                return decodeMultipart(MultiPartStatus.HEADERDELIMITER);
            }
            return null;
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new ErrorDataDecoderException("No Multipart delimiter found");
    }

    /**
     * Find the next Disposition
     *
     * @return the next InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    private InterfaceHttpData findMultipartDisposition() {
        int readerIndex = undecodedChunk.readerIndex();
        if (currentStatus == MultiPartStatus.DISPOSITION) {
            currentFieldAttributes = new TreeMap<CharSequence, Attribute>(CaseIgnoringComparator.INSTANCE);
        }
        // read many lines until empty line with newline found! Store all data
        while (!skipOneLine()) {
            String newline;
            try {
                skipControlCharacters(undecodedChunk);
                newline = readLine(undecodedChunk, charset);
            } catch (NotEnoughDataDecoderException ignored) {
                undecodedChunk.readerIndex(readerIndex);
                // TODO return partial
                return null;
            }
            String[] contents = splitMultipartHeader(newline);
            if (HttpHeaderNames.CONTENT_DISPOSITION.contentEqualsIgnoreCase(contents[0])) {
                boolean checkSecondArg;
                if (currentStatus == MultiPartStatus.DISPOSITION) {
                    checkSecondArg = HttpHeaderValues.FORM_DATA.contentEqualsIgnoreCase(contents[1]);
                } else {
                    checkSecondArg = HttpHeaderValues.ATTACHMENT.contentEqualsIgnoreCase(contents[1])
                            || HttpHeaderValues.FILE.contentEqualsIgnoreCase(contents[1]);
                }
                if (checkSecondArg) {
                    // read next values and store them in the map as Attribute
                    for (int i = 2; i < contents.length; i++) {
                        String[] values = contents[i].split("=", 2);
                        Attribute attribute;
                        try {
                            attribute = getContentDispositionAttribute(values);
                        } catch (NullPointerException e) {
                            throw new ErrorDataDecoderException(e);
                        } catch (IllegalArgumentException e) {
                            throw new ErrorDataDecoderException(e);
                        }
                        currentFieldAttributes.put(attribute.getName(), attribute);
                    }
                }
            } else if (HttpHeaderNames.CONTENT_TRANSFER_ENCODING.contentEqualsIgnoreCase(contents[0])) {
                Attribute attribute;
                try {
                    attribute = new Attribute(HttpHeaderNames.CONTENT_TRANSFER_ENCODING.toString(),
                            cleanString(contents[1]));
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                }

                currentFieldAttributes.put(HttpHeaderNames.CONTENT_TRANSFER_ENCODING, attribute);
            } else if (HttpHeaderNames.CONTENT_LENGTH.contentEqualsIgnoreCase(contents[0])) {
                Attribute attribute;
                try {
                    attribute = new Attribute(HttpHeaderNames.CONTENT_LENGTH.toString(),
                            cleanString(contents[1]));
                } catch (NullPointerException e) {
                    throw new ErrorDataDecoderException(e);
                } catch (IllegalArgumentException e) {
                    throw new ErrorDataDecoderException(e);
                }

                currentFieldAttributes.put(HttpHeaderNames.CONTENT_LENGTH, attribute);
            } else if (HttpHeaderNames.CONTENT_TYPE.contentEqualsIgnoreCase(contents[0])) {
                // Take care of possible "multipart/mixed"
                if (HttpHeaderValues.MULTIPART_MIXED.contentEqualsIgnoreCase(contents[1])) {
                    if (currentStatus == MultiPartStatus.DISPOSITION) {
                        String values = StringUtil.substringAfter(contents[2], '=');
                        multipartMixedBoundary = "--" + values;
                        currentStatus = MultiPartStatus.MIXEDDELIMITER;
                        return decodeMultipart(MultiPartStatus.MIXEDDELIMITER);
                    } else {
                        throw new ErrorDataDecoderException("Mixed Multipart found in a previous Mixed Multipart");
                    }
                } else {
                    for (int i = 1; i < contents.length; i++) {
                        final String charsetHeader = HttpHeaderValues.CHARSET.toString();
                        if (contents[i].regionMatches(true, 0, charsetHeader, 0, charsetHeader.length())) {
                            String values = StringUtil.substringAfter(contents[i], '=');
                            Attribute attribute;
                            try {
                                attribute = new Attribute(charsetHeader, cleanString(values));
                            } catch (NullPointerException e) {
                                throw new ErrorDataDecoderException(e);
                            } catch (IllegalArgumentException e) {
                                throw new ErrorDataDecoderException(e);
                            }
                            currentFieldAttributes.put(HttpHeaderValues.CHARSET, attribute);
                        } else {
                            Attribute attribute;
                            try {
                                attribute = new Attribute(
                                        cleanString(contents[0]), contents[i]);
                            } catch (NullPointerException e) {
                                throw new ErrorDataDecoderException(e);
                            } catch (IllegalArgumentException e) {
                                throw new ErrorDataDecoderException(e);
                            }
                            currentFieldAttributes.put(attribute.getName(), attribute);
                        }
                    }
                }
            } else {
                throw new ErrorDataDecoderException("Unknown Params: " + newline);
            }
        }
        // Is it a FileUpload
        Attribute filenameAttribute = currentFieldAttributes.get(HttpHeaderValues.FILENAME);
        if (currentStatus == MultiPartStatus.DISPOSITION) {
            if (filenameAttribute != null) {
                // FileUpload
                currentStatus = MultiPartStatus.FILEUPLOAD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.FILEUPLOAD);
            } else {
                // Field
                currentStatus = MultiPartStatus.FIELD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.FIELD);
            }
        } else {
            if (filenameAttribute != null) {
                // FileUpload
                currentStatus = MultiPartStatus.MIXEDFILEUPLOAD;
                // do not change the buffer position
                return decodeMultipart(MultiPartStatus.MIXEDFILEUPLOAD);
            } else {
                // Field is not supported in MIXED mode
                throw new ErrorDataDecoderException("Filename not found");
            }
        }
    }

    private static final String FILENAME_ENCODED = HttpHeaderValues.FILENAME.toString() + '*';

    private Attribute getContentDispositionAttribute(String... values) {
        String name = cleanString(values[0]);
        String value = values[1];

        // Filename can be token, quoted or encoded. See https://tools.ietf.org/html/rfc5987
        if (HttpHeaderValues.FILENAME.contentEquals(name)) {
            // Value is quoted or token. Strip if quoted:
            int last = value.length() - 1;
            if (last > 0 &&
              value.charAt(0) == HttpConstants.DOUBLE_QUOTE &&
              value.charAt(last) == HttpConstants.DOUBLE_QUOTE) {
                value = value.substring(1, last);
            }
        } else if (FILENAME_ENCODED.equals(name)) {
            try {
                name = HttpHeaderValues.FILENAME.toString();
                String[] split = value.split("'", 3);
                value = QueryStringDecoder.decodeComponent(split[2], Charset.forName(split[0]));
            } catch (ArrayIndexOutOfBoundsException e) {
                 throw new ErrorDataDecoderException(e);
            } catch (UnsupportedCharsetException e) {
                throw new ErrorDataDecoderException(e);
            }
        } else {
            // otherwise we need to clean the value
            value = cleanString(value);
        }
        return new Attribute(name, value);
    }

    /**
     * Get the FileUpload (new one or current one)
     *
     * @param delimiter
     *            the delimiter to use
     * @return the InterfaceHttpData if any
     * @throws ErrorDataDecoderException
     */
    protected InterfaceHttpData getFileUpload(String delimiter) {
        // eventually restart from existing FileUpload
        // Now get value according to Content-Type and Charset
        Attribute encoding = currentFieldAttributes.get(HttpHeaderNames.CONTENT_TRANSFER_ENCODING);
        Charset localCharset = charset;
        // Default
        TransferEncodingMechanism mechanism = TransferEncodingMechanism.BIT7;
        if (encoding != null) {
            String code;
//            try {
                code = encoding.getValue().toLowerCase();
//            } catch (IOException e) {
//                throw new ErrorDataDecoderException(e);
//            }
            if (code.equals(TransferEncodingMechanism.BIT7.value())) {
                localCharset = CharsetUtil.US_ASCII;
            } else if (code.equals(TransferEncodingMechanism.BIT8.value())) {
                localCharset = CharsetUtil.ISO_8859_1;
                mechanism = TransferEncodingMechanism.BIT8;
            } else if (code.equals(TransferEncodingMechanism.BINARY.value())) {
                // no real charset, so let the default
                mechanism = TransferEncodingMechanism.BINARY;
            } else {
                throw new ErrorDataDecoderException("TransferEncoding Unknown: " + code);
            }
        }
        Attribute charsetAttribute = currentFieldAttributes.get(HttpHeaderValues.CHARSET);
        if (charsetAttribute != null) {
            try {
                localCharset = Charset.forName(charsetAttribute.getValue());
//            } catch (IOException e) {
//                throw new ErrorDataDecoderException(e);
            } catch (UnsupportedCharsetException e) {
                throw new ErrorDataDecoderException(e);
            }
        }
        if (currentFileUpload == null) {
            Attribute filenameAttribute = currentFieldAttributes.get(HttpHeaderValues.FILENAME);
            Attribute nameAttribute = currentFieldAttributes.get(HttpHeaderValues.NAME);
            Attribute contentTypeAttribute = currentFieldAttributes.get(HttpHeaderNames.CONTENT_TYPE);
            Attribute lengthAttribute = currentFieldAttributes.get(HttpHeaderNames.CONTENT_LENGTH);
            long size;
            try {
                size = lengthAttribute != null ? Long.parseLong(lengthAttribute.getValue()) : 0L;
//            } catch (IOException e) {
//                throw new ErrorDataDecoderException(e);
            } catch (NumberFormatException ignored) {
                size = 0;
            }
            try {
                String contentType;
                if (contentTypeAttribute != null) {
                    contentType = contentTypeAttribute.getValue();
                } else {
                    contentType = DEFAULT_BINARY_CONTENT_TYPE;
                }
                currentFileUpload = new FileUploadImpl(
                        cleanString(nameAttribute.getValue()), cleanString(filenameAttribute.getValue()),
                        contentType, mechanism.value(), localCharset,
                        size);
            } catch (NullPointerException e) {
                throw new ErrorDataDecoderException(e);
            } catch (IllegalArgumentException e) {
                throw new ErrorDataDecoderException(e);
//            } catch (IOException e) {
//                throw new ErrorDataDecoderException(e);
            }
        }
        // load data as much as possible
        if (!loadDataMultipart(undecodedChunk, delimiter, currentFileUpload)) {
            // Delimiter is not found. Need more chunks.
            // TODO return partial
            return null;
        }
        if (currentFileUpload.isCompleted()) {
            // ready to load the next one
            if (currentStatus == MultiPartStatus.FILEUPLOAD) {
                currentStatus = MultiPartStatus.HEADERDELIMITER;
                currentFieldAttributes = null;
            } else {
                currentStatus = MultiPartStatus.MIXEDDELIMITER;
                cleanMixedAttributes();
            }
            FileUpload fileUpload = currentFileUpload;
            currentFileUpload = null;
            return fileUpload;
        }
        // do not change the buffer position
        // since some can be already saved into FileUpload
        // So do not change the currentStatus
        return null;
    }

    /**
     * Destroy the {@link HttpPostMultipartRequestDecoder} and release all it resources. After this method
     * was called it is not possible to operate on it anymore.
     */
    public void destroy() {
        checkDestroyed();
        destroyed = true;

        if (undecodedChunk != null && undecodedChunk.refCnt() > 0) {
            undecodedChunk.release();
            undecodedChunk = null;
        }

        // release all data which was not yet pulled
        for (int i = bodyListHttpDataRank; i < bodyListHttpData.size(); i++) {
            bodyListHttpData.get(i).release();
        }
    }

    /**
     * Remove all Attributes that should be cleaned between two FileUpload in
     * Mixed mode
     */
    private void cleanMixedAttributes() {
        currentFieldAttributes.remove(HttpHeaderValues.CHARSET);
        currentFieldAttributes.remove(HttpHeaderNames.CONTENT_LENGTH);
        currentFieldAttributes.remove(HttpHeaderNames.CONTENT_TRANSFER_ENCODING);
        currentFieldAttributes.remove(HttpHeaderNames.CONTENT_TYPE);
        currentFieldAttributes.remove(HttpHeaderValues.FILENAME);
    }

    /**
     * Read one line up to the CRLF or LF
     *
     * @return the String from one line
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the {@code readerIndex} to the previous
     *             value
     */
    private static String readLineStandard(ByteBuf undecodedChunk, Charset charset) {
        int readerIndex = undecodedChunk.readerIndex();
        try {
            ByteBuf line = buffer(64);

            while (undecodedChunk.isReadable()) {
                byte nextByte = undecodedChunk.readByte();
                if (nextByte == HttpConstants.CR) {
                    // check but do not changed readerIndex
                    nextByte = undecodedChunk.getByte(undecodedChunk.readerIndex());
                    if (nextByte == HttpConstants.LF) {
                        // force read
                        undecodedChunk.readByte();
                        return line.toString(charset);
                    } else {
                        // Write CR (not followed by LF)
                        line.writeByte(HttpConstants.CR);
                    }
                } else if (nextByte == HttpConstants.LF) {
                    return line.toString(charset);
                } else {
                    line.writeByte(nextByte);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to the CRLF or LF
     *
     * @return the String from one line
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the {@code readerIndex} to the previous
     *             value
     */
    private static String readLine(ByteBuf undecodedChunk, Charset charset) {
        if (!undecodedChunk.hasArray()) {
            return readLineStandard(undecodedChunk, charset);
        }
        SeekAheadOptimize sao = new SeekAheadOptimize(undecodedChunk);
        int readerIndex = undecodedChunk.readerIndex();
        try {
            ByteBuf line = buffer(64);

            while (sao.pos < sao.limit) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == HttpConstants.CR) {
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == HttpConstants.LF) {
                            sao.setReadPosition(0);
                            return line.toString(charset);
                        } else {
                            // Write CR (not followed by LF)
                            sao.pos--;
                            line.writeByte(HttpConstants.CR);
                        }
                    } else {
                        line.writeByte(nextByte);
                    }
                } else if (nextByte == HttpConstants.LF) {
                    sao.setReadPosition(0);
                    return line.toString(charset);
                } else {
                    line.writeByte(nextByte);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to --delimiter or --delimiter-- and if existing the CRLF
     * or LF Read one line up to --delimiter or --delimiter-- and if existing
     * the CRLF or LF. Note that CRLF or LF are mandatory for opening delimiter
     * (--delimiter) but not for closing delimiter (--delimiter--) since some
     * clients does not include CRLF in this case.
     *
     * @param delimiter
     *            of the form --string, such that '--' is already included
     * @return the String from one line as the delimiter searched (opening or
     *         closing)
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the {@code readerIndex} to the previous
     *             value
     */
    private static String readDelimiterStandard(ByteBuf undecodedChunk, String delimiter) {
        int readerIndex = undecodedChunk.readerIndex();
        try {
            StringBuilder sb = new StringBuilder(64);
            int delimiterPos = 0;
            int len = delimiter.length();
            while (undecodedChunk.isReadable() && delimiterPos < len) {
                byte nextByte = undecodedChunk.readByte();
                if (nextByte == delimiter.charAt(delimiterPos)) {
                    delimiterPos++;
                    sb.append((char) nextByte);
                } else {
                    // delimiter not found so break here !
                    undecodedChunk.readerIndex(readerIndex);
                    throw new NotEnoughDataDecoderException();
                }
            }
            // Now check if either opening delimiter or closing delimiter
            if (undecodedChunk.isReadable()) {
                byte nextByte = undecodedChunk.readByte();
                // first check for opening delimiter
                if (nextByte == HttpConstants.CR) {
                    nextByte = undecodedChunk.readByte();
                    if (nextByte == HttpConstants.LF) {
                        return sb.toString();
                    } else {
                        // error since CR must be followed by LF
                        // delimiter not found so break here !
                        undecodedChunk.readerIndex(readerIndex);
                        throw new NotEnoughDataDecoderException();
                    }
                } else if (nextByte == HttpConstants.LF) {
                    return sb.toString();
                } else if (nextByte == '-') {
                    sb.append('-');
                    // second check for closing delimiter
                    nextByte = undecodedChunk.readByte();
                    if (nextByte == '-') {
                        sb.append('-');
                        // now try to find if CRLF or LF there
                        if (undecodedChunk.isReadable()) {
                            nextByte = undecodedChunk.readByte();
                            if (nextByte == HttpConstants.CR) {
                                nextByte = undecodedChunk.readByte();
                                if (nextByte == HttpConstants.LF) {
                                    return sb.toString();
                                } else {
                                    // error CR without LF
                                    // delimiter not found so break here !
                                    undecodedChunk.readerIndex(readerIndex);
                                    throw new NotEnoughDataDecoderException();
                                }
                            } else if (nextByte == HttpConstants.LF) {
                                return sb.toString();
                            } else {
                                // No CRLF but ok however (Adobe Flash uploader)
                                // minus 1 since we read one char ahead but
                                // should not
                                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                                return sb.toString();
                            }
                        }
                        // FIXME what do we do here?
                        // either considering it is fine, either waiting for
                        // more data to come?
                        // lets try considering it is fine...
                        return sb.toString();
                    }
                    // only one '-' => not enough
                    // whatever now => error since incomplete
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Read one line up to --delimiter or --delimiter-- and if existing the CRLF
     * or LF. Note that CRLF or LF are mandatory for opening delimiter
     * (--delimiter) but not for closing delimiter (--delimiter--) since some
     * clients does not include CRLF in this case.
     *
     * @param delimiter
     *            of the form --string, such that '--' is already included
     * @return the String from one line as the delimiter searched (opening or
     *         closing)
     * @throws NotEnoughDataDecoderException
     *             Need more chunks and reset the readerInder to the previous
     *             value
     */
    private static String readDelimiter(ByteBuf undecodedChunk, String delimiter) {
        if (!undecodedChunk.hasArray()) {
            return readDelimiterStandard(undecodedChunk, delimiter);
        }
        SeekAheadOptimize sao = new SeekAheadOptimize(undecodedChunk);
        int readerIndex = undecodedChunk.readerIndex();
        int delimiterPos = 0;
        int len = delimiter.length();
        try {
            StringBuilder sb = new StringBuilder(64);
            // check conformity with delimiter
            while (sao.pos < sao.limit && delimiterPos < len) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == delimiter.charAt(delimiterPos)) {
                    delimiterPos++;
                    sb.append((char) nextByte);
                } else {
                    // delimiter not found so break here !
                    undecodedChunk.readerIndex(readerIndex);
                    throw new NotEnoughDataDecoderException();
                }
            }
            // Now check if either opening delimiter or closing delimiter
            if (sao.pos < sao.limit) {
                byte nextByte = sao.bytes[sao.pos++];
                if (nextByte == HttpConstants.CR) {
                    // first check for opening delimiter
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == HttpConstants.LF) {
                            sao.setReadPosition(0);
                            return sb.toString();
                        } else {
                            // error CR without LF
                            // delimiter not found so break here !
                            undecodedChunk.readerIndex(readerIndex);
                            throw new NotEnoughDataDecoderException();
                        }
                    } else {
                        // error since CR must be followed by LF
                        // delimiter not found so break here !
                        undecodedChunk.readerIndex(readerIndex);
                        throw new NotEnoughDataDecoderException();
                    }
                } else if (nextByte == HttpConstants.LF) {
                    // same first check for opening delimiter where LF used with
                    // no CR
                    sao.setReadPosition(0);
                    return sb.toString();
                } else if (nextByte == '-') {
                    sb.append('-');
                    // second check for closing delimiter
                    if (sao.pos < sao.limit) {
                        nextByte = sao.bytes[sao.pos++];
                        if (nextByte == '-') {
                            sb.append('-');
                            // now try to find if CRLF or LF there
                            if (sao.pos < sao.limit) {
                                nextByte = sao.bytes[sao.pos++];
                                if (nextByte == HttpConstants.CR) {
                                    if (sao.pos < sao.limit) {
                                        nextByte = sao.bytes[sao.pos++];
                                        if (nextByte == HttpConstants.LF) {
                                            sao.setReadPosition(0);
                                            return sb.toString();
                                        } else {
                                            // error CR without LF
                                            // delimiter not found so break here !
                                            undecodedChunk.readerIndex(readerIndex);
                                            throw new NotEnoughDataDecoderException();
                                        }
                                    } else {
                                        // error CR without LF
                                        // delimiter not found so break here !
                                        undecodedChunk.readerIndex(readerIndex);
                                        throw new NotEnoughDataDecoderException();
                                    }
                                } else if (nextByte == HttpConstants.LF) {
                                    sao.setReadPosition(0);
                                    return sb.toString();
                                } else {
                                    // No CRLF but ok however (Adobe Flash
                                    // uploader)
                                    // minus 1 since we read one char ahead but
                                    // should not
                                    sao.setReadPosition(1);
                                    return sb.toString();
                                }
                            }
                            // FIXME what do we do here?
                            // either considering it is fine, either waiting for
                            // more data to come?
                            // lets try considering it is fine...
                            sao.setReadPosition(0);
                            return sb.toString();
                        }
                        // whatever now => error since incomplete
                        // only one '-' => not enough or whatever not enough
                        // element
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            undecodedChunk.readerIndex(readerIndex);
            throw new NotEnoughDataDecoderException(e);
        }
        undecodedChunk.readerIndex(readerIndex);
        throw new NotEnoughDataDecoderException();
    }

    /**
     * Load the field value or file data from a Multipart request
     *
     * @return {@code true} if the last chunk is loaded (boundary delimiter found), {@code false} if need more chunks
     * @throws ErrorDataDecoderException
     */
    private static boolean loadDataMultipartStandard(ByteBuf undecodedChunk, String delimiter, HttpData httpData) {
        final int startReaderIndex = undecodedChunk.readerIndex();
        final int delimeterLength = delimiter.length();
        int index = 0;
        int lastPosition = startReaderIndex;
        byte prevByte = HttpConstants.LF;
        boolean delimiterFound = false;
        while (undecodedChunk.isReadable()) {
            final byte nextByte = undecodedChunk.readByte();
            // Check the delimiter
            if (prevByte == HttpConstants.LF && nextByte == delimiter.codePointAt(index)) {
                index++;
                if (delimeterLength == index) {
                    delimiterFound = true;
                    break;
                }
                continue;
            }
            lastPosition = undecodedChunk.readerIndex();
            if (nextByte == HttpConstants.LF) {
                index = 0;
                lastPosition -= (prevByte == HttpConstants.CR)? 2 : 1;
            }
            prevByte = nextByte;
        }
        if (prevByte == HttpConstants.CR) {
            lastPosition--;
        }
        ByteBuf content = undecodedChunk.copy(startReaderIndex, lastPosition - startReaderIndex);
        try {
            httpData.addContent(content, delimiterFound);
        } catch (IOException e) {
            throw new ErrorDataDecoderException(e);
        }
        undecodedChunk.readerIndex(lastPosition);
        return delimiterFound;
    }

    /**
     * Load the field value from a Multipart request
     *
     * @return {@code true} if the last chunk is loaded (boundary delimiter found), {@code false} if need more chunks
     * @throws ErrorDataDecoderException
     */
    private static boolean loadDataMultipart(ByteBuf undecodedChunk, String delimiter, HttpData httpData) {
        if (!undecodedChunk.hasArray()) {
            return loadDataMultipartStandard(undecodedChunk, delimiter, httpData);
        }
        final SeekAheadOptimize sao = new SeekAheadOptimize(undecodedChunk);
        final int startReaderIndex = undecodedChunk.readerIndex();
        final int delimeterLength = delimiter.length();
        int index = 0;
        int lastRealPos = sao.pos;
        byte prevByte = HttpConstants.LF;
        boolean delimiterFound = false;
        while (sao.pos < sao.limit) {
            final byte nextByte = sao.bytes[sao.pos++];
            // Check the delimiter
            if (prevByte == HttpConstants.LF && nextByte == delimiter.codePointAt(index)) {
                index++;
                if (delimeterLength == index) {
                    delimiterFound = true;
                    break;
                }
                continue;
            }
            lastRealPos = sao.pos;
            if (nextByte == HttpConstants.LF) {
                index = 0;
                lastRealPos -= (prevByte == HttpConstants.CR)? 2 : 1;
            }
            prevByte = nextByte;
        }
        if (prevByte == HttpConstants.CR) {
            lastRealPos--;
        }
        final int lastPosition = sao.getReadPosition(lastRealPos);
        final ByteBuf content = undecodedChunk.copy(startReaderIndex, lastPosition - startReaderIndex);
        try {
            httpData.addContent(content, delimiterFound);
        } catch (IOException e) {
            throw new ErrorDataDecoderException(e);
        }
        undecodedChunk.readerIndex(lastPosition);
        return delimiterFound;
    }

    /**
     * Clean the String from any unallowed character
     *
     * @return the cleaned String
     */
    private static String cleanString(String field) {
        int size = field.length();
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char nextChar = field.charAt(i);
            switch (nextChar) {
            case HttpConstants.COLON:
            case HttpConstants.COMMA:
            case HttpConstants.EQUALS:
            case HttpConstants.SEMICOLON:
            case HttpConstants.HT:
                sb.append(HttpConstants.SP_CHAR);
                break;
            case HttpConstants.DOUBLE_QUOTE:
                // nothing added, just removes it
                break;
            default:
                sb.append(nextChar);
                break;
            }
        }
        return sb.toString().trim();
    }

    /**
     * Skip one empty line
     *
     * @return True if one empty line was skipped
     */
    private boolean skipOneLine() {
        if (!undecodedChunk.isReadable()) {
            return false;
        }
        byte nextByte = undecodedChunk.readByte();
        if (nextByte == HttpConstants.CR) {
            if (!undecodedChunk.isReadable()) {
                undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
                return false;
            }
            nextByte = undecodedChunk.readByte();
            if (nextByte == HttpConstants.LF) {
                return true;
            }
            undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 2);
            return false;
        }
        if (nextByte == HttpConstants.LF) {
            return true;
        }
        undecodedChunk.readerIndex(undecodedChunk.readerIndex() - 1);
        return false;
    }

    /**
     * Split one header in Multipart
     *
     * @return an array of String where rank 0 is the name of the header,
     *         follows by several values that were separated by ';' or ','
     */
    private static String[] splitMultipartHeader(String sb) {
        ArrayList<String> headers = new ArrayList<String>(1);
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;
        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < sb.length(); nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }
        for (colonEnd = nameEnd; colonEnd < sb.length(); colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }
        valueStart = findNonWhitespace(sb, colonEnd);
        valueEnd = findEndOfString(sb);
        headers.add(sb.substring(nameStart, nameEnd));
        String svalue = (valueStart >= valueEnd) ? StringUtil.EMPTY_STRING : sb.substring(valueStart, valueEnd);
        String[] values;
        if (svalue.indexOf(';') >= 0) {
            values = splitMultipartHeaderValues(svalue);
        } else {
            values = svalue.split(",");
        }
        for (String value : values) {
            headers.add(value.trim());
        }
        String[] array = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            array[i] = headers.get(i);
        }
        return array;
    }

    /**
     * Split one header value in Multipart
     * @return an array of String where values that were separated by ';' or ','
     */
    private static String[] splitMultipartHeaderValues(String svalue) {
        List<String> values = InternalThreadLocalMap.get().arrayList(1);
        boolean inQuote = false;
        boolean escapeNext = false;
        int start = 0;
        for (int i = 0; i < svalue.length(); i++) {
            char c = svalue.charAt(i);
            if (inQuote) {
                if (escapeNext) {
                    escapeNext = false;
                } else {
                    if (c == '\\') {
                        escapeNext = true;
                    } else if (c == '"') {
                        inQuote = false;
                    }
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == ';') {
                    values.add(svalue.substring(start, i));
                    start = i + 1;
                }
            }
        }
        values.add(svalue.substring(start));
        return values.toArray(new String[values.size()]);
    }

    /**
     * Exception when try reading data from request in chunked format, and not
     * enough data are available (need more chunks)
     */
    public static class NotEnoughDataDecoderException extends DecoderException {
        private static final long serialVersionUID = -7846841864603865638L;

        public NotEnoughDataDecoderException() {
        }

        public NotEnoughDataDecoderException(String msg) {
            super(msg);
        }

        public NotEnoughDataDecoderException(Throwable cause) {
            super(cause);
        }

        public NotEnoughDataDecoderException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Exception when the body is fully decoded, even if there is still data
     */
    public static class EndOfDataDecoderException extends DecoderException {
        private static final long serialVersionUID = 1336267941020800769L;
    }

    /**
     * Exception when an error occurs while decoding
     */
    public static class ErrorDataDecoderException extends DecoderException {
        private static final long serialVersionUID = 5020247425493164465L;

        public ErrorDataDecoderException() {
        }

        public ErrorDataDecoderException(String msg) {
            super(msg);
        }

        public ErrorDataDecoderException(Throwable cause) {
            super(cause);
        }

        public ErrorDataDecoderException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * A {@link CharSequence} case insensitive comparator.
     */
    private static class CaseIgnoringComparator
            implements Comparator<CharSequence>, Serializable {

        private static final long serialVersionUID = 4582133183775373862L;

        static final CaseIgnoringComparator INSTANCE =
                new CaseIgnoringComparator();

        private CaseIgnoringComparator() {
        }

        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            int o1Length = o1.length();
            int o2Length = o2.length();
            int min = Math.min(o1Length, o2Length);
            for (int i = 0; i < min; i++) {
                char c1 = o1.charAt(i);
                char c2 = o2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2) {
                            return c1 - c2;
                        }
                    }
                }
            }
            return o1Length - o2Length;
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
    * This class intends to decrease the CPU in seeking ahead some bytes in
    * {@link MultipartDecoder}.
    */
    private static class SeekAheadOptimize {
        byte[] bytes;
        int readerIndex;
        int pos;
        int origPos;
        int limit;
        ByteBuf buffer;

        /**
         * @param buffer buffer with a backing byte array
         */
        SeekAheadOptimize(ByteBuf buffer) {
            if (!buffer.hasArray()) {
                throw new IllegalArgumentException(
                        "buffer hasn't backing byte array");
            }
            this.buffer = buffer;
            bytes = buffer.array();
            readerIndex = buffer.readerIndex();
            origPos = pos = buffer.arrayOffset() + readerIndex;
            limit = buffer.arrayOffset() + buffer.writerIndex();
        }

        /**
         * @param minus this value will be used as (currentPos - minus) to set
         * the current readerIndex in the buffer.
         */
        void setReadPosition(int minus) {
            pos -= minus;
            readerIndex = getReadPosition(pos);
            buffer.readerIndex(readerIndex);
        }

        /**
        * @param index raw index of the array (pos in general)
        * @return the value equivalent of raw index to be used in
        *  readerIndex(value)
        */
        int getReadPosition(int index) {
            return index - origPos + readerIndex;
        }
    }

    /**
     * Supported transfer encoding mechanisms.
     *
     * Allowed mechanism for multipart
     * mechanism := "7bit"
                  / "8bit"
                  / "binary"
       Not allowed: "quoted-printable"
                  / "base64"
     */
    private static enum TransferEncodingMechanism {
        /**
         * Default encoding
         */
        BIT7("7bit"),
        /**
         * Short lines but not in ASCII - no encoding
         */
        BIT8("8bit"),
        /**
         * Could be long text not in ASCII - no encoding
         */
        BINARY("binary");

        private final String value;

        TransferEncodingMechanism(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Find the first non white space.
     * @return the rank of the first non white space
     */
    private static int findNonWhitespace(final String sb, final int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    /**
     * Find the end of String.
     * @return the rank of the end of string
     */
    private static int findEndOfString(final String sb) {
        int result;
        for (result = sb.length(); result > 0; result --) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    private static interface InterfaceHttpData {

        /**
         * Returns the name of this InterfaceHttpData.
         */
        String getName();

        /**
         * Decreases the reference count by {@code 1} and deallocates this object if the reference count reaches at
         * {@code 0}.
         *
         * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
         */
        boolean release();
    }

    private static interface HttpData extends InterfaceHttpData {

        /**
         * Add the content from the ChannelBuffer
         *
         * @param buffer
         *            must be not null except if last is set to False
         * @param last
         *            True of the buffer is the last one
         * @throws IOException
         */
        void addContent(ByteBuf buffer, boolean last) throws IOException;

        /**
         *
         * @return True if the InterfaceHttpData is completed (all data are stored)
         */
        boolean isCompleted();

        /**
         * Returns the size in byte of the InterfaceHttpData
         *
         * @return the size of the InterfaceHttpData
         */
        long length();

        /**
         * Returns the defined length of the HttpData.
         *
         * If no Content-Length is provided in the request, the defined length is
         * always 0 (whatever during decoding or in final state).
         *
         * If Content-Length is provided in the request, this is this given defined length.
         * This value does not change, whatever during decoding or in the final state.
         *
         * This method could be used for instance to know the amount of bytes transmitted for
         * one particular HttpData, for example one {@link FileUpload} or any known big {@link Attribute}.
         *
         * @return the defined length of the HttpData
         */
        long definedLength();

        /**
         * Returns the content of the file item as a ByteBuf
         *
         * @return the content of the file item as a ByteBuf
         * @throws IOException
         */
        ByteBuf getByteBuf() throws IOException;

        /**
         * Returns the contents of the file item as a String, using the default
         * character encoding.
         *
         * @return the contents of the file item as a String, using the default
         *         character encoding.
         * @throws IOException
         */
        String getString() throws IOException;

        /**
         * Returns the contents of the file item as a String, using the specified
         * charset.
         *
         * @param encoding
         *            the charset to use
         * @return the contents of the file item as a String, using the specified
         *         charset.
         * @throws IOException
         */
        String getString(Charset encoding) throws IOException;
    }

    private static interface FileUpload extends HttpData {
        /**
         * Returns the original filename in the client's filesystem,
         * as provided by the browser (or other client software).
         * @return the original filename
         */
        String getFilename();

        /**
         * Returns the content type passed by the browser or null if not defined.
         * @return the content type passed by the browser or null if not defined.
         */
        String getContentType();

        /**
         * Returns the Content-Transfer-Encoding
         * @return the Content-Transfer-Encoding
         */
        String getContentTransferEncoding();
    }

    private static class FileUploadImpl implements FileUpload {

        private final String name;
        private final String fileName;
        private final String contentType;
        private final String contentTransfertEncoding;
        private ByteBuf buf;
        private boolean completed = false;
        private long size;
        private final Charset charset;

        public FileUploadImpl(String name, String fileName, String contentType, String contentTransfertEncoding, Charset charset, long size) {
            this.name = name;
            this.fileName = fileName;
            this.contentType = contentType;
            this.contentTransfertEncoding = contentTransfertEncoding;
            this.charset = charset;
            this.size = size;
            this.buf = null; //XXX FIXME
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getContentTransferEncoding() {
            return contentTransfertEncoding;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean release() {
            return buf.release();
        }

        @Override
        public void addContent(ByteBuf buffer, boolean last) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public long length() {
            return size;
        }

        @Override
        public long definedLength() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuf getByteBuf() throws IOException {
            return buf;
        }

        @Override
        public String getString() throws IOException {
            return buf.toString(Charset.defaultCharset());
        }

        @Override
        public String getString(Charset encoding) throws IOException {
            return buf.toString(encoding);
        }
    }

/**
     * Split the very first line (Content-Type value) in 3 Strings
     *
     * @return the array of 3 Strings
     */
    private static String[] splitHeaderContentType(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;
        aStart = findNonWhitespace(sb, 0);
        aEnd =  sb.indexOf(';');
        if (aEnd == -1) {
            return new String[] { sb, "", "" };
        }
        bStart = findNonWhitespace(sb, aEnd + 1);
        if (sb.charAt(aEnd - 1) == ' ') {
            aEnd--;
        }
        bEnd =  sb.indexOf(';', bStart);
        if (bEnd == -1) {
            bEnd = findEndOfString(sb);
            return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), "" };
        }
        cStart = findNonWhitespace(sb, bEnd + 1);
        if (sb.charAt(bEnd - 1) == ' ') {
            bEnd--;
        }
        cEnd = findEndOfString(sb);
        return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd) };
    }
}
