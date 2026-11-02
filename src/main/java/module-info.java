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
 * Java clients for the <a href="https://cdr.everfox.com">Everfox CDR Cloud APIs</a>.
 *
 * <p>{@code com.everfox.cdr.internal} is not exported; it holds implementation details shared by the clients.
 */
module com.everfox.cdr {
    // CdrApiResponse takes an HttpResponse, so modules using the library need java.net.http too
    requires transitive java.net.http;
    requires tools.jackson.databind;

    exports com.everfox.cdr;
    exports com.everfox.cdr.async;
    exports com.everfox.cdr.instant;
    exports com.everfox.cdr.options;
}
