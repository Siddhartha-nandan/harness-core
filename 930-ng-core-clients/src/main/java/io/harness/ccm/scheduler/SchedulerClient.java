/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.scheduler;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SchedulerClient {
  @POST("/v1/jobs") Call<SchedulerDTO> createOrUpdateJob(@Body RequestBody body);
  @POST("/v1/jobs/{job_name}/toggle")
  Call<SchedulerDTO> toggleJob(@Body RequestBody body, @Path("job_name") String job_name);
  @POST("/v1/jobs/{job_name}") Call<SchedulerDTO> deleteJob(@Body RequestBody body, @Path("job_name") String job_name);
}
