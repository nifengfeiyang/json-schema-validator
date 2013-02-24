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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.load.Dereferencing;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.jsonschema.processors.validation.ValidationProcessor;
import com.github.fge.jsonschema.report.ListProcessingReport;
import com.github.fge.jsonschema.report.LogLevel;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.report.ReportProvider;
import com.github.fge.jsonschema.tree.JsonTree;
import com.github.fge.jsonschema.tree.SchemaTree;
import com.github.fge.jsonschema.tree.SimpleJsonTree;

public final class JsonValidator
{
    private final Dereferencing dereferencing;
    private final ValidationProcessor processor;
    private final ReportProvider reportProvider;

    JsonValidator(final Dereferencing dereferencing,
        final ValidationProcessor processor,
        final ReportProvider reportProvider)
    {
        this.dereferencing = dereferencing;
        this.processor = processor;
        this.reportProvider = reportProvider;
    }

    private void doValidate(final JsonNode schema, final JsonNode instance,
        final ProcessingReport report)
        throws ProcessingException
    {
        final SchemaTree schemaTree = dereferencing.newTree(schema);
        final JsonTree tree = new SimpleJsonTree(instance);
        final FullData data = new FullData(schemaTree, tree);
        processor.process(report, data);
    }

    public ProcessingReport validate(final JsonNode schema,
        final JsonNode instance)
        throws ProcessingException
    {
        final ProcessingReport report = reportProvider.newReport();
        doValidate(schema, instance, report);
        return report;
    }

    public ProcessingReport validateUnchecked(final JsonNode schema,
        final JsonNode instance)
    {
        final ProcessingReport report = reportProvider.newReport();
        try {
            doValidate(schema, instance, report);
            return report;
        } catch (ProcessingException e) {
            return buildUncheckedReport(report, e);
        }
    }

    private ProcessingReport buildUncheckedReport
        (final ProcessingReport report, final ProcessingException e)
    {
        final ProcessingReport ret
            = reportProvider.newReport(report.getLogLevel(), LogLevel.NONE);
        final ProcessingMessage message = e.getProcessingMessage()
            .setLogLevel(LogLevel.FATAL)
            .put("info", "other messages follow (if any)");
        final ListProcessingReport r
            = new ListProcessingReport(report.getLogLevel(), LogLevel.NONE);
        r.log(LogLevel.FATAL, message);
        try {
            r.mergeWith(report);
            ret.mergeWith(r);
        } catch (ProcessingException ignored) {
        }

        return ret;
    }
}