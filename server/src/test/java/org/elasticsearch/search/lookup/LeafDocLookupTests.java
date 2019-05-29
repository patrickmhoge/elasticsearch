/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.lookup;

import org.elasticsearch.index.fielddata.AtomicFieldData;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.index.mapper.JsonFieldMapper.KeyedJsonFieldType;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.util.function.Function;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LeafDocLookupTests extends ESTestCase {
    private ScriptDocValues<?> docValues;
    private LeafDocLookup docLookup;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        MappedFieldType fieldType = mock(MappedFieldType.class);
        when(fieldType.name()).thenReturn("field");
        when(fieldType.valueForDisplay(anyObject())).then(returnsFirstArg());

        MapperService mapperService = mock(MapperService.class);
        when(mapperService.fullName("field")).thenReturn(fieldType);
        when(mapperService.fullName("alias")).thenReturn(fieldType);

        docValues = mock(ScriptDocValues.class);
        IndexFieldData<?> fieldData = createFieldData(docValues);

        docLookup = new LeafDocLookup(mapperService,
            ignored -> fieldData,
            null);
    }

    public void testBasicLookup() {
        ScriptDocValues<?> fetchedDocValues = docLookup.get("field");
        assertEquals(docValues, fetchedDocValues);
    }

    public void testFieldAliases() {
        ScriptDocValues<?> fetchedDocValues = docLookup.get("alias");
        assertEquals(docValues, fetchedDocValues);
    }

    public void testJsonFields() {
        MapperService mapperService = mock(MapperService.class);

        ScriptDocValues<?> docValues1 = mock(ScriptDocValues.class);
        IndexFieldData<?> fieldData1 = createFieldData(docValues1);

        ScriptDocValues<?> docValues2 = mock(ScriptDocValues.class);
        IndexFieldData<?> fieldData2 = createFieldData(docValues2);

        KeyedJsonFieldType fieldType1 = new KeyedJsonFieldType("key1");
        fieldType1.setName("json._keyed");
        when(mapperService.fullName("json.key1")).thenReturn(fieldType1);

        KeyedJsonFieldType fieldType2 = new KeyedJsonFieldType("key2");
        fieldType1.setName("json._keyed");
        when(mapperService.fullName("json.key2")).thenReturn(fieldType2);

        Function<MappedFieldType, IndexFieldData<?>> fieldDataSupplier = fieldType -> {
            KeyedJsonFieldType jsonFieldType = (KeyedJsonFieldType) fieldType;
            return jsonFieldType.key().equals("key1") ? fieldData1 : fieldData2;
        };

        LeafDocLookup jsonDocLookup = new LeafDocLookup(mapperService, fieldDataSupplier, null);
        assertEquals(docValues1, jsonDocLookup.get("json.key1"));
        assertEquals(docValues2, jsonDocLookup.get("json.key2"));
    }

    private IndexFieldData<?> createFieldData(ScriptDocValues scriptDocValues) {
        AtomicFieldData atomicFieldData = mock(AtomicFieldData.class);
        doReturn(scriptDocValues).when(atomicFieldData).getScriptValues();

        IndexFieldData<?> fieldData = mock(IndexFieldData.class);
        when(fieldData.getFieldName()).thenReturn("field");
        doReturn(atomicFieldData).when(fieldData).load(anyObject());

        return fieldData;
    }
}
