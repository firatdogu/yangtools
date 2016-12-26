/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.stmt.reactor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.YangVersion;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.meta.IdentifierNamespace;
import org.opendaylight.yangtools.yang.parser.spi.meta.CopyType;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.NamespaceStorageNode;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.Registry;
import org.opendaylight.yangtools.yang.parser.spi.meta.NamespaceBehaviour.StorageNodeType;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContextUtils;
import org.opendaylight.yangtools.yang.parser.spi.source.IncludedModuleContext;

/**
 * Root statement class for a YANG source. All statements defined in that YANG source are mapped underneath an instance
 * of this class, hence recursive lookups from them cross this class.
 */
public class RootStatementContext<A, D extends DeclaredStatement<A>, E extends EffectiveStatement<A, D>> extends
        StatementContextBase<A, D, E> {

    public static final YangVersion DEFAULT_VERSION = YangVersion.VERSION_1;

    private final SourceSpecificContext sourceContext;
    private final A argument;

    private YangVersion version;

    /**
     * References to RootStatementContext of submodules which are included in this source.
     */
    private Collection<RootStatementContext<?, ?, ?>> includedContexts = ImmutableList.of();

    RootStatementContext(final ContextBuilder<A, D, E> builder, final SourceSpecificContext sourceContext) {
        super(builder);
        this.sourceContext = Preconditions.checkNotNull(sourceContext);
        this.argument = builder.getDefinition().parseArgumentValue(this, builder.getRawArgument());
    }

    RootStatementContext(final ContextBuilder<A, D, E> builder, final SourceSpecificContext sourceContext,
            final YangVersion version) {
        this(builder, sourceContext);
        this.setRootVersion(version);
    }

    RootStatementContext(final RootStatementContext<A, D, E> original, final QNameModule newQNameModule,
        final CopyType typeOfCopy) {
        super(original);

        sourceContext = Preconditions.checkNotNull(original.sourceContext);
        this.argument = original.argument;

        final Collection<StatementContextBase<?, ?, ?>> declared = original.declaredSubstatements();
        final Collection<StatementContextBase<?, ?, ?>> effective = original.effectiveSubstatements();
        final Collection<StatementContextBase<?, ?, ?>> buffer = new ArrayList<>(declared.size() + effective.size());

        for (final StatementContextBase<?, ?, ?> stmtContext : declared) {
            if (StmtContextUtils.areFeaturesSupported(stmtContext)) {
                buffer.add(stmtContext.createCopy(newQNameModule, this, typeOfCopy));
            }
        }
        for (final StmtContext<?, ?, ?> stmtContext : effective) {
            buffer.add(stmtContext.createCopy(newQNameModule, this, typeOfCopy));
        }

        addEffectiveSubstatements(buffer);
    }

    /**
     * @return null as root cannot have parent
     */
    @Override
    public StatementContextBase<?, ?, ?> getParentContext() {
        return null;
    }

    /**
     * @return namespace storage of source context
     */
    @Override
    public NamespaceStorageNode getParentNamespaceStorage() {
        return sourceContext;
    }

    @Override
    public Registry getBehaviourRegistry() {
        return sourceContext;
    }

    @Override
    public StorageNodeType getStorageNodeType() {
        return StorageNodeType.ROOT_STATEMENT_LOCAL;
    }
    /**
     * @return this as its own root
     */
    @Nonnull
    @Override
    public RootStatementContext<?, ?, ?> getRoot() {
        return this;
    }

    SourceSpecificContext getSourceContext() {
        return sourceContext;
    }

    @Override
    public A getStatementArgument() {
        return argument;
    }

    /**
     * @return copy of this considering {@link CopyType} (augment, uses)
     *
     * @throws org.opendaylight.yangtools.yang.parser.spi.source.SourceException instance of SourceException
     */
    @Override
    public StatementContextBase<?, ?, ?> createCopy(final StatementContextBase<?, ?, ?> newParent,
            final CopyType typeOfCopy) {
        return createCopy(null, newParent, typeOfCopy);
    }

    /**
     * @return copy of this considering {@link CopyType} (augment, uses)
     *
     * @throws org.opendaylight.yangtools.yang.parser.spi.source.SourceException instance of SourceException
     */
    @Override
    public StatementContextBase<A, D, E> createCopy(final QNameModule newQNameModule,
            final StatementContextBase<?, ?, ?> newParent, final CopyType typeOfCopy) {
        final RootStatementContext<A, D, E> copy = new RootStatementContext<>(this, newQNameModule, typeOfCopy);

        copy.appendCopyHistory(typeOfCopy, this.getCopyHistory());

        if (this.getOriginalCtx() != null) {
            copy.setOriginalCtx(this.getOriginalCtx());
        } else {
            copy.setOriginalCtx(this);
        }
        definition().onStatementAdded(copy);
        return copy;
    }

    @Nonnull
    @Override
    public Optional<SchemaPath> getSchemaPath() {
        return Optional.of(SchemaPath.ROOT);
    }

    /**
     * @return true
     */
    @Override
    public boolean isRootContext() {
        return true;
    }

    @Override
    public boolean isConfiguration() {
        return true;
    }

    @Override
    public boolean isEnabledSemanticVersioning() {
        return sourceContext.isEnabledSemanticVersioning();
    }

    @Override
    public <K, V, N extends IdentifierNamespace<K, V>> void addToLocalStorage(final Class<N> type, final K key,
            final V value) {
        if (IncludedModuleContext.class.isAssignableFrom(type)) {
            if (includedContexts.isEmpty()) {
                includedContexts = new ArrayList<>(1);
            }
            Verify.verify(value instanceof RootStatementContext);
            includedContexts.add((RootStatementContext<?, ?, ?>) value);
        }
        super.addToLocalStorage(type, key, value);
    }

    @Override
    public <K, V, N extends IdentifierNamespace<K, V>> V getFromLocalStorage(final Class<N> type, final K key) {
        final V potentialLocal = super.getFromLocalStorage(type, key);
        if (potentialLocal != null) {
            return potentialLocal;
        }
        for (final NamespaceStorageNode includedSource : includedContexts) {
            final V potential = includedSource.getFromLocalStorage(type, key);
            if (potential != null) {
                return potential;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public <K, V, N extends IdentifierNamespace<K, V>> Map<K, V> getAllFromLocalStorage(final Class<N> type) {
        final Map<K, V> potentialLocal = super.getAllFromLocalStorage(type);
        if (potentialLocal != null) {
            return potentialLocal;
        }
        for (final NamespaceStorageNode includedSource : includedContexts) {
            final Map<K, V> potential = includedSource.getAllFromLocalStorage(type);
            if (potential != null) {
                return potential;
            }
        }
        return null;
    }

    @Override
    public YangVersion getRootVersion() {
        return version == null ? DEFAULT_VERSION : version;
    }

    @Override
    public void setRootVersion(final YangVersion version) {
        Preconditions.checkArgument(sourceContext.getSupportedVersions().contains(version),
                "Unsupported yang version %s in %s", version, getStatementSourceReference());
        Preconditions.checkState(this.version == null, "Version of root %s has been already set to %s", argument,
                this.version);
        this.version = Preconditions.checkNotNull(version);
    }
}
