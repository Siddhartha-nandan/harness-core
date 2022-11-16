-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS ON_DEMAND_ROLLBACK BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS ORIGINAL_EXECUTION_ID TEXT;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS MANUALLY_ROLLED_BACK BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ON_DEMAND_ROLLBACK_INDEX ON DEPLOYMENT(on_demand_rollback,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS MANUALLY_ROLLED_BACK_INDEX ON DEPLOYMENT(manually_rolled_back,ENDTIME DESC);
COMMIT ;