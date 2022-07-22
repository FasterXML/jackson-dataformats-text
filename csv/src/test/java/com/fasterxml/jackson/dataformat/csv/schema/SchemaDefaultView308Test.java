package com.fasterxml.jackson.dataformat.csv.schema;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import tools.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class SchemaDefaultView308Test extends ModuleTestBase
{
    interface BaseView {}
    @JsonView(BaseView.class)
    interface BaseViewMixIn {}

    interface ExtendedView extends BaseView {}
    @JsonView(ExtendedView.class)
    interface ExtendedViewMixIn {}

    interface OtherView {}
    @JsonView(OtherView.class)
    interface OtherViewMixIn {}

    static class ViewTestPojo
    {
        @JsonProperty
        boolean flag;
        @JsonProperty
        @JsonView(BaseView.class)
        char character;
        @JsonProperty
        @JsonView(ExtendedView.class)
        int number;
        @JsonProperty
        @JsonView(OtherView.class)
        String text;

        ViewTestPojo(final boolean flag, final char character, final int number, final String text)
        {
            this.flag = flag;
            this.character = character;
            this.number = number;
            this.text = text;
        }
    }

    private final ViewTestPojo POJO = new ViewTestPojo(true, '!', 1234, "dummy");

    private CsvMapper _createMapper(boolean inclusion, Class<?> defaultViewMixin)
    {
        final CsvMapper.Builder builder = CsvMapper.builder().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, inclusion);
        if (defaultViewMixin != null) {
            builder.addMixIn(ViewTestPojo.class, defaultViewMixin);
        }
        return builder.build();
    }

    private void _verifyExpected(final CsvMapper csvMapper, final Class<?> view, final String... expectedNames)
    {
        final Set<String> actualCsvNames = new HashSet<>();
        final CsvSchema schema = csvMapper.schemaForWithView(POJO.getClass(), view);
        schema.rebuild().getColumns().forEachRemaining(c -> assertTrue(actualCsvNames.add(c.getName())));
        assertEquals(
                view == null ? "null" : view.getSimpleName() + " misses fields/columns",
                new HashSet<>(Arrays.asList(expectedNames)),
                actualCsvNames
        );
    }

    public void testSchemaWithImplicitDefaultViewAndDefaultViewInclusionDisabled()
    {
        CsvMapper csvMapper = _createMapper(false, null);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "character");
        _verifyExpected(csvMapper, ExtendedView.class, "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "text");
    }

    public void testSchemaWithDefaultBaseViewAndDefaultViewInclusionDisabled()
    {
        CsvMapper csvMapper = _createMapper(false, BaseViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "flag", "character");
        _verifyExpected(csvMapper, ExtendedView.class, "flag", "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "text");
    }

    public void testSchemaWithDefaultExtendedViewAndDefaultViewInclusionDisabled()
    {
        CsvMapper csvMapper = _createMapper(false, ExtendedViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "character");
        _verifyExpected(csvMapper, ExtendedView.class, "flag", "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "text");
    }

    public void testSchemaWithDefaultOtherViewAndDefaultViewInclusionDisabled()
    {
        CsvMapper csvMapper = _createMapper(false, OtherViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "character");
        _verifyExpected(csvMapper, ExtendedView.class, "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "flag", "text");
    }

    public void testSchemaWithImplicitDefaultViewAndDefaultViewInclusionEnabled()
    {
        CsvMapper csvMapper = _createMapper(true, null);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "flag", "character");
        _verifyExpected(csvMapper, ExtendedView.class, "flag", "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "flag", "text");
    }

    public void testSchemaWithDefaultBaseViewAndDefaultViewInclusionEnabled()
    {
        CsvMapper csvMapper = _createMapper(true, BaseViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "flag", "character");
        _verifyExpected(csvMapper, ExtendedView.class, "flag", "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "text");
    }

    public void testSchemaWithDefaultExtendedViewAndDefaultViewInclusionEnabled()
    {
        CsvMapper csvMapper = _createMapper(true, ExtendedViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "character");
        _verifyExpected(csvMapper, ExtendedView.class, "flag", "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "text");
    }

    public void testSchemaWithDefaultOtherViewAndDefaultViewInclusionEnabled()
    {
        CsvMapper csvMapper = _createMapper(true, OtherViewMixIn.class);
        _verifyExpected(csvMapper, null, "flag", "character", "number", "text");
        _verifyExpected(csvMapper, BaseView.class, "character");
        _verifyExpected(csvMapper, ExtendedView.class, "character", "number");
        _verifyExpected(csvMapper, OtherView.class, "flag", "text");
    }

}
