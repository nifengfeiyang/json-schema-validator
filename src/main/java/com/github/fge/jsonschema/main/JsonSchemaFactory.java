/*
 * Copyright (c) 2013, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.fge.jsonschema.main;

import com.github.fge.jsonschema.cfg.LoadingConfiguration;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.load.SchemaLoader;
import com.github.fge.jsonschema.processing.Processor;
import com.github.fge.jsonschema.processing.ProcessorMap;
import com.github.fge.jsonschema.processors.data.SchemaContext;
import com.github.fge.jsonschema.processors.data.ValidatorList;
import com.github.fge.jsonschema.processors.ref.RefResolver;
import com.github.fge.jsonschema.processors.validation.ValidationChain;
import com.github.fge.jsonschema.processors.validation.ValidationProcessor;
import com.github.fge.jsonschema.ref.JsonRef;
import com.github.fge.jsonschema.report.ReportProvider;
import com.github.fge.jsonschema.util.Frozen;
import com.google.common.base.Function;

import java.util.Map;

public final class JsonSchemaFactory
    implements Frozen<JsonSchemaFactoryBuilder>
{
    final ReportProvider reportProvider;
    final LoadingConfiguration loadingCfg;
    final ValidationConfiguration validationCfg;
    final JsonValidator validator;

    public static JsonSchemaFactory byDefault()
    {
        return newBuilder().freeze();
    }

    public static JsonSchemaFactoryBuilder newBuilder()
    {
        return new JsonSchemaFactoryBuilder();
    }

    JsonSchemaFactory(final JsonSchemaFactoryBuilder builder)
    {
        reportProvider = builder.reportProvider;
        loadingCfg = builder.loadingCfg;
        validationCfg = builder.validationCfg;
        final Processor<SchemaContext, ValidatorList> processor
            = buildProcessor(loadingCfg, validationCfg);
        validator = new JsonValidator(loadingCfg.getDereferencing(),
            new ValidationProcessor(processor), reportProvider);
    }

    public JsonValidator getValidator()
    {
        return validator;
    }

    @Override
    public JsonSchemaFactoryBuilder thaw()
    {
        return new JsonSchemaFactoryBuilder(this);
    }

    private static Processor<SchemaContext, ValidatorList>
        buildProcessor(final LoadingConfiguration loadingCfg,
            final ValidationConfiguration validationCfg)
    {
        final SchemaLoader loader = new SchemaLoader(loadingCfg);
        final RefResolver resolver = new RefResolver(loader);
        final boolean useFormat = validationCfg.getUseFormat();

        final Map<JsonRef, Library> libraries = validationCfg.getLibraries();
        final Library defaultLibrary = validationCfg.getDefaultLibrary();
        final ValidationChain defaultChain
            = new ValidationChain(resolver, defaultLibrary, useFormat);
        ProcessorMap<JsonRef, SchemaContext, ValidatorList> map
            = new FullChain().setDefaultProcessor(defaultChain);

        JsonRef ref;
        ValidationChain chain;

        for (final Map.Entry<JsonRef, Library> entry: libraries.entrySet()) {
            ref = entry.getKey();
            chain = new ValidationChain(resolver, entry.getValue(), useFormat);
            map = map.addEntry(ref, chain);
        }

        return map.getProcessor();
    }

    private static final class FullChain
        extends ProcessorMap<JsonRef, SchemaContext, ValidatorList>
    {
        @Override
        protected Function<SchemaContext, JsonRef> f()
        {
            return new Function<SchemaContext, JsonRef>()
            {
                @Override
                public JsonRef apply(final SchemaContext input)
                {
                    return input.getSchema().getDollarSchema();
                }
            };
        }
    }
}