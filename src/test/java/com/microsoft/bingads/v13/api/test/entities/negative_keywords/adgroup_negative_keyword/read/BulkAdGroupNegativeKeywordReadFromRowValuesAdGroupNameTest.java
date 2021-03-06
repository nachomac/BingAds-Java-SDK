package com.microsoft.bingads.v13.api.test.entities.negative_keywords.adgroup_negative_keyword.read;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.microsoft.bingads.internal.functionalinterfaces.Function;
import com.microsoft.bingads.v13.api.test.entities.negative_keywords.adgroup_negative_keyword.BulkAdGroupNegativeKeywordTest;
import com.microsoft.bingads.v13.bulk.entities.BulkAdGroupNegativeKeyword;

public class BulkAdGroupNegativeKeywordReadFromRowValuesAdGroupNameTest extends BulkAdGroupNegativeKeywordTest {

    @Parameter(value = 1)
    public String expectedResult;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"Test AdGroup 1", "Test AdGroup 1"},
            {"", ""},
            {null, null}
        });
    }

    @Test
    public void testRead() {
        this.<String>testReadProperty("Ad Group", this.datum, this.expectedResult, new Function<BulkAdGroupNegativeKeyword, String>() {
            @Override
            public String apply(BulkAdGroupNegativeKeyword c) {
                return c.getAdGroupName();
            }
        });
    }
}
