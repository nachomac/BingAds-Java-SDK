package com.microsoft.bingads.v13.internal.bulk.entities;

import com.microsoft.bingads.v13.bulk.entities.BulkError;
import com.microsoft.bingads.v13.internal.bulk.BulkObject;
import com.microsoft.bingads.v13.internal.bulk.BulkObjectWriter;
import com.microsoft.bingads.v13.internal.bulk.BulkRecordReader;
import com.microsoft.bingads.v13.internal.bulk.TryResult;
import java.io.IOException;

public abstract class BulkEntityIdentifier extends BulkObject {

    @Override
    public abstract boolean equals(Object other);

    public abstract boolean isDeleteRow();

    public abstract MultiRecordBulkEntity createEntityWithThisIdentifier();

    @Override
    public void write(BulkObjectWriter rowWriter, boolean excludeReadonlyData) throws IOException {
        rowWriter.writeObjectRow(this);
    }

    @Override
    public void readRelatedData(BulkRecordReader reader) {
        // If this is a delete all row, just skip any error rows after this delete row
        if (isDeleteRow()) {
            boolean hasMoreErrors = true;

            while (hasMoreErrors) {
                TryResult<BulkError> errorResult = reader.tryRead(BulkError.class);
                hasMoreErrors = (errorResult.isSuccessful());
            }
        }
    }

    @Override
    public boolean canEncloseInMultilineEntity() {
        return true;
    }

    @Override
    public MultiRecordBulkEntity encloseInMultilineEntity() {
        return createEntityWithThisIdentifier();
    }
}
