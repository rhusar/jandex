/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.jandex;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a Jandex index file and returns the saved index. See {@link Indexer}
 * for a thorough description of how the Index data is produced.
 *
 * <p>
 * An IndexReader loads the stream passed to it's constructor and applies the
 * appropriate buffering. The Jandex index format is designed for efficient
 * reading and low final memory storage.
 *
 * <p>
 * <b>Thread-Safety</b>
 * </p>
 * IndexReader is not thread-safe and can not be shared between concurrent
 * threads. The resulting index, however, is.
 *
 * @author Jason T. Greene
 */
public final class IndexReader {

    /**
     * The latest index version supported by this version of Jandex.
     */
    private static final int MAGIC = 0xBABE1F15;
    private PackedDataInputStream input;
    private int version = -1;
    private IndexReaderImpl reader;

    /**
     * Constructs a new IndedReader using the passed stream. The stream is not
     * read from until the read method is called.
     *
     * @param input a stream which points to a jandex index file
     */
    public IndexReader(InputStream input) {
        this.input = new PackedDataInputStream(new BufferedInputStream(input));
    }

    /**
     * Read the index at the associated stream of this reader. This method can be called multiple
     * times if the stream contains multiple index files.
     *
     * @return the Index contained in the stream
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the stream does not point to Jandex index data
     * @throws UnsupportedVersion if the index data is tagged with a version not known to this reader
     */
    public Index read() throws IOException {
        if (version == -1) {
            readVersion();
        }

        return reader.read();
    }

    private void initReader(int version) throws IOException {
        IndexReaderImpl reader;
        if (version >= IndexReaderV1.MIN_VERSION && version <= IndexReaderV1.MAX_VERSION) {
            reader = new IndexReaderV1(input, version);
        } else if (version >= IndexReaderV2.MIN_VERSION && version <= IndexReaderV2.MAX_VERSION) {
            reader = new IndexReaderV2(input, version);
        } else {
            input.close();
            throw new UnsupportedVersion("Can't read index version " + version
                    + "; this IndexReader only supports index versions "
                    + IndexReaderV1.MIN_VERSION + "-" + IndexReaderV1.MAX_VERSION + ","
                    + IndexReaderV2.MIN_VERSION + "-" + IndexReaderV2.MAX_VERSION);
        }

        this.reader = reader;
    }

    /**
     * Returns the index file version. This version number marks the internal storage format and also implies
     * the version of data contract of the index. It is incremented whenever more information are added
     * to the index format, so it may be used to determine whether an index file contains necessary information.
     *
     * @return the index file version
     * @throws IOException If the index could not be read
     */
    public int getIndexVersion() throws IOException {
        if (version == -1) {
            readVersion();
        }

        return version;
    }

    private void readVersion() throws IOException {
        if (input.readInt() != MAGIC) {
            input.close();
            throw new IllegalArgumentException("Not a jandex index");
        }

        version = input.readUnsignedByte();
        initReader(version);
    }
}
