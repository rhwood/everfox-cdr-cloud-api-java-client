/*
 * Copyright 2026 Everfox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Java client for the <a href="https://cdr.everfox.com/api/async">Everfox CDR Cloud Async API</a>.
 *
 * <p>The Async API processes a file in four steps, each driven by the links returned in the previous step's
 * {@link com.everfox.cdr.async.AsyncApiStatus status}:
 *
 * <ol>
 *   <li>{@link com.everfox.cdr.async.AsyncApiClient#initiate(AsyncApiRequest) initiate} a job, which returns an upload
 *       link and a status link;
 *   <li>{@link com.everfox.cdr.async.AsyncApiClient#upload(AsyncApiStatus, byte[]) upload} the file to the upload link;
 *   <li>{@link com.everfox.cdr.async.AsyncApiClient#waitForCompletion(AsyncApiStatus) poll} the status link until the
 *       job is complete, which returns a result link if processing succeeded; and
 *   <li>{@link com.everfox.cdr.async.AsyncApiClient#download(AsyncApiStatus) download} the processed file from the
 *       result link.
 * </ol>
 *
 * <p>{@link com.everfox.cdr.async.AsyncApiClient#process(AsyncApiRequest, byte[])} performs all four steps.
 */
package com.everfox.cdr.async;
