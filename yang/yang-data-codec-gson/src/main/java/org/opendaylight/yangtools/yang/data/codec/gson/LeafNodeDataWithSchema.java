/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.gson;

import java.io.IOException;

import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

class LeafNodeDataWithSchema extends SimpleNodeDataWithSchema {

    public LeafNodeDataWithSchema(final DataSchemaNode schema) {
        super(schema);
    }

    @Override
    protected void writeToStream(final NormalizedNodeStreamWriter nnStreamWriter) throws IOException {
        nnStreamWriter.leafNode(provideNodeIdentifier(), getValue());
    }

}
