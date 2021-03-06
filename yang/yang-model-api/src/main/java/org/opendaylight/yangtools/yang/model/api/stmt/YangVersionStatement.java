/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.api.stmt;

import static com.google.common.base.Verify.verifyNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.YangVersion;
import org.opendaylight.yangtools.yang.model.api.YangStmtMapping;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.StatementDefinition;

public interface YangVersionStatement extends DeclaredStatement<YangVersion> {
    @Override
    default StatementDefinition statementDefinition() {
        return YangStmtMapping.YANG_VERSION;
    }

    default @NonNull YangVersion getValue() {
        // FIXME: YANGTOOLS-908: verifyNotNull() should not be needed here
        return verifyNotNull(argument());
    }
}

